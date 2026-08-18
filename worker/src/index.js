/**
 * HelpCircle peer-alert sender.
 *
 * Exists only because Firebase Cloud Messaging cannot be triggered from a client: sending requires
 * OAuth2 service-account credentials, and the legacy server-key API that once allowed it was
 * retired in June 2024. Those credentials must never ship inside the Android app, since anyone
 * unpacking the APK could then push to every device in the project. Cloud Functions would be the
 * natural home, but they require Firebase's Blaze plan, so this Worker plays that role instead.
 *
 * It does three things and nothing else:
 *   1. verifies the caller's Firebase anonymous ID token, so only real app installs can call it;
 *   2. reads the caller's own member document, which both proves they belong to the circle they
 *      claim and supplies the nickname to show, so no display text is ever taken from the client;
 *   3. sends one data-only, high-priority message to that circle's topic.
 *
 * Topic addressing rather than per-device tokens is deliberate: no device token has to be stored
 * anywhere for a member to be reachable, which keeps the roster to the minimal fields the app's
 * privacy rule already permits.
 */

const FCM_SCOPES = [
  "https://www.googleapis.com/auth/firebase.messaging",
  "https://www.googleapis.com/auth/datastore",
].join(" ");

const GOOGLE_JWK_URL =
  "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";
const TOKEN_URL = "https://oauth2.googleapis.com/token";
const COMMUNITY_ID_PATTERN = /^[A-Za-z0-9_-]{1,64}$/;

// Cached per isolate. Access tokens last an hour, so re-minting one per request would spend most
// of this Worker's CPU budget on an RSA signature nobody needed.
let cachedAccessToken = null;
let cachedJwks = null;

export default {
  async fetch(request, env) {
    if (request.method !== "POST") {
      return json(405, { error: "method_not_allowed" });
    }
    try {
      return await handleAlert(request, env);
    } catch (error) {
      // Deliberately terse: the caller is a phone that can do nothing useful with a stack trace,
      // and detail here would help someone probing the endpoint more than it helps the app.
      console.log("alert failed:", error && error.message);
      return json(500, { error: "internal_error" });
    }
  },
};

async function handleAlert(request, env) {
  const projectId = env.FIREBASE_PROJECT_ID;

  const idToken = bearerToken(request.headers.get("Authorization"));
  if (!idToken) return json(401, { error: "missing_token" });

  const senderId = await verifyFirebaseIdToken(idToken, projectId);
  if (!senderId) return json(401, { error: "invalid_token" });

  const body = await request.json().catch(() => null);
  const communityId = body && body.communityId;
  if (!communityId || !COMMUNITY_ID_PATTERN.test(communityId)) {
    return json(400, { error: "invalid_community_id" });
  }

  const accessToken = await getAccessToken(env);

  const member = await readMemberDocument(projectId, communityId, senderId, accessToken);
  if (!member) return json(403, { error: "not_a_member" });

  await sendTopicMessage(projectId, communityId, senderId, member.nickname, accessToken);
  return json(200, { ok: true });
}

/** Reads the caller's own roster entry, which doubles as the membership check. */
async function readMemberDocument(projectId, communityId, uid, accessToken) {
  const url =
    "https://firestore.googleapis.com/v1/projects/" +
    projectId +
    "/databases/(default)/documents/communities/" +
    communityId +
    "/members/" +
    uid;
  const response = await fetch(url, { headers: { Authorization: "Bearer " + accessToken } });
  if (response.status === 404) return null;
  if (!response.ok) throw new Error("firestore " + response.status);
  const doc = await response.json();
  const fields = doc.fields || {};
  return { nickname: (fields.nickname && fields.nickname.stringValue) || "Someone" };
}

/**
 * Data-only and high priority on purpose. Data-only means the receiving app always gets
 * onMessageReceived, so it can drop the sender's own copy and build the notification itself; high
 * priority is what lets the message through Doze on an idle phone.
 */
async function sendTopicMessage(projectId, communityId, senderId, nickname, accessToken) {
  const response = await fetch(
    "https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send",
    {
      method: "POST",
      headers: {
        Authorization: "Bearer " + accessToken,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        message: {
          topic: "community_" + communityId,
          data: {
            type: "peer_crisis",
            communityId: communityId,
            senderId: senderId,
            nickname: nickname,
          },
          android: { priority: "high" },
        },
      }),
    }
  );
  if (!response.ok) {
    throw new Error("fcm " + response.status + ": " + (await response.text()));
  }
}

/** Verifies a Firebase ID token and returns its subject, or null if anything about it is wrong. */
async function verifyFirebaseIdToken(idToken, projectId) {
  const parts = idToken.split(".");
  if (parts.length !== 3) return null;

  // Decoding is its own try/catch because a token that merely *looks* like a JWT still throws
  // here on base64 or JSON that will not parse. Letting that reach the outer handler would answer
  // a caller's malformed input with a 500, which reads as a fault on this side and would bury a
  // genuine server error among the noise.
  let header;
  let payload;
  try {
    header = JSON.parse(decodeBase64Url(parts[0]));
    payload = JSON.parse(decodeBase64Url(parts[1]));
  } catch (error) {
    return null;
  }

  if (header.alg !== "RS256" || !header.kid) return null;
  if (payload.aud !== projectId) return null;
  if (payload.iss !== "https://securetoken.google.com/" + projectId) return null;
  if (!payload.sub) return null;

  const now = Math.floor(Date.now() / 1000);
  if (payload.exp <= now || payload.iat > now + 300) return null;

  const keys = await getGoogleJwks();
  const jwk = keys.find(function (key) {
    return key.kid === header.kid;
  });
  if (!jwk) return null;

  const key = await crypto.subtle.importKey(
    "jwk",
    jwk,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["verify"]
  );
  const verified = await crypto.subtle.verify(
    "RSASSA-PKCS1-v1_5",
    key,
    base64UrlToBytes(parts[2]),
    new TextEncoder().encode(parts[0] + "." + parts[1])
  );
  return verified ? payload.sub : null;
}

async function getGoogleJwks() {
  if (cachedJwks && cachedJwks.expiresAt > Date.now()) return cachedJwks.keys;
  const response = await fetch(GOOGLE_JWK_URL);
  if (!response.ok) throw new Error("jwks " + response.status);
  const keys = (await response.json()).keys;
  cachedJwks = { keys: keys, expiresAt: Date.now() + 60 * 60 * 1000 };
  return keys;
}

/** Exchanges the service-account key for an access token, reusing it until it is nearly expired. */
async function getAccessToken(env) {
  if (cachedAccessToken && cachedAccessToken.expiresAt > Date.now() + 60000) {
    return cachedAccessToken.token;
  }

  const now = Math.floor(Date.now() / 1000);
  const claims = {
    iss: env.FIREBASE_CLIENT_EMAIL,
    scope: FCM_SCOPES,
    aud: TOKEN_URL,
    iat: now,
    exp: now + 3600,
  };
  const unsigned =
    encodeBase64Url(JSON.stringify({ alg: "RS256", typ: "JWT" })) +
    "." +
    encodeBase64Url(JSON.stringify(claims));

  const key = await importServiceAccountKey(env.FIREBASE_PRIVATE_KEY);
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(unsigned)
  );
  const assertion = unsigned + "." + bytesToBase64Url(new Uint8Array(signature));

  const response = await fetch(TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: assertion,
    }),
  });
  if (!response.ok) {
    throw new Error("oauth " + response.status + ": " + (await response.text()));
  }

  const token = await response.json();
  cachedAccessToken = {
    token: token.access_token,
    expiresAt: Date.now() + token.expires_in * 1000,
  };
  return cachedAccessToken.token;
}

function importServiceAccountKey(pem) {
  // Tolerates the key however it was pasted: wrangler keeps real newlines, but the same value
  // copied straight out of a JSON key file carries literal backslash-n instead.
  const body = pem
    .replace(/\\n/g, "\n")
    .replace(/-----[A-Z ]+-----/g, "")
    .replace(/\s/g, "");
  return crypto.subtle.importKey(
    "pkcs8",
    base64ToBytes(body),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );
}

function bearerToken(header) {
  if (!header || header.indexOf("Bearer ") !== 0) return null;
  return header.slice("Bearer ".length).trim() || null;
}

function base64ToBytes(value) {
  const binary = atob(value);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return bytes;
}

function base64UrlToBytes(value) {
  return base64ToBytes(value.replace(/-/g, "+").replace(/_/g, "/"));
}

function decodeBase64Url(value) {
  return new TextDecoder().decode(base64UrlToBytes(value));
}

function bytesToBase64Url(bytes) {
  let binary = "";
  for (let i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i]);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function encodeBase64Url(value) {
  return bytesToBase64Url(new TextEncoder().encode(value));
}

function json(status, body) {
  return new Response(JSON.stringify(body), {
    status: status,
    headers: { "Content-Type": "application/json" },
  });
}
