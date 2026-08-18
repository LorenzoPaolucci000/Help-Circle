# HelpCircle peer-alert sender

A single Cloudflare Worker that pushes "a peer needs support" alerts to a circle through Firebase
Cloud Messaging.

## Why this exists

FCM delivery is free and unlimited, but *sending* requires OAuth2 service-account credentials — the
legacy server-key API was retired in June 2024. Those credentials cannot ship inside the Android
app, because anyone unpacking the APK could then push to every device in the project. Cloud
Functions would be the obvious home for them, but Cloud Functions require Firebase's Blaze plan.

This Worker is the smallest possible stand-in: it holds the credential, checks the caller is a real
member of the circle they name, and sends one message. Cloudflare's free plan allows 100,000
requests a day and needs no credit card.

## What it does per request

1. Verifies the caller's Firebase anonymous ID token against Google's public keys, so only genuine
   installs of this app can call it.
2. Reads the caller's own member document. This proves membership *and* supplies the nickname, so
   no text shown to other people is ever taken from the request body.
3. Sends one data-only, high-priority message to the topic `community_<communityId>`.

Data-only means the receiving app always gets `onMessageReceived`, so it can discard the sender's
own copy and build the notification itself. High priority is what gets the message through Doze.

## Setup

You need the Firebase service-account key and the Cloudflare CLI. Everything below is free.

### 1. Download the Firebase service-account key

1. Open the [Firebase console](https://console.firebase.google.com/) and select **help-circle-c80fa**.
2. Gear icon → **Project settings** → **Service accounts** tab.
3. Click **Generate new private key**, confirm, and save the JSON file somewhere outside this repo.

That file contains `client_email` and `private_key`. Treat it like a password: it grants send access
to the whole project. Do not commit it.

### 2. Install the CLI and log in

```bash
cd worker
npm install
npx wrangler login
```

`wrangler login` opens a browser and asks you to authorise Wrangler on your Cloudflare account.

### 3. Store the two secrets

Run each command and paste the value when prompted.

```bash
npx wrangler secret put FIREBASE_CLIENT_EMAIL
# paste the "client_email" value, e.g. firebase-adminsdk-xxxxx@help-circle-c80fa.iam.gserviceaccount.com

npx wrangler secret put FIREBASE_PRIVATE_KEY
# paste the whole "private_key" value, including the BEGIN and END lines
```

The private key works whether you paste real newlines or the literal `\n` sequences as they appear
in the JSON file; the Worker normalises both.

`FIREBASE_PROJECT_ID` is not a secret and is already set in `wrangler.toml`.

### 4. Deploy

```bash
npx wrangler deploy
```

Wrangler prints the deployed URL, of the form:

```
https://helpcircle-alerts.<your-subdomain>.workers.dev
```

**Keep that URL** — the Android app needs it in the next step.

### 5. Check it is alive

```bash
curl -i -X POST https://helpcircle-alerts.<your-subdomain>.workers.dev
```

A correct deployment answers `401 {"error":"missing_token"}`. That is the success case here: it
means the Worker is running and refusing unauthenticated callers. A 404 or a connection error means
the deploy did not land.

To watch live logs while testing from the phone:

```bash
npx wrangler tail
```

## Request shape

```
POST /
Authorization: Bearer <Firebase ID token>
Content-Type: application/json

{ "communityId": "3e0016b9-a9b9-4efc-bd80-b5a82ee547d1" }
```

| Response | Meaning |
| --- | --- |
| `200 {"ok":true}` | Message accepted by FCM |
| `401 missing_token` / `invalid_token` | No or bad Firebase ID token |
| `400 invalid_community_id` | Missing or malformed community id |
| `403 not_a_member` | Valid user, but not a member of that circle |

## Limitations worth knowing

- **No rate limiting.** A member could call this repeatedly and spam their own circle. The app only
  calls it on a crisis transition, so this is a misuse concern rather than an accident one; adding a
  limit would need Workers KV or Durable Objects.
- **A member can alert their own circle at any time**, not only during a real crisis, since the
  Worker cannot verify that the sender's device genuinely detected one.
- **The free plan resets at 00:00 UTC** and returns error 1027 past 100,000 requests in a day.
- **This is a second point of failure outside Firebase.** If the Worker is down, alerts stop; the
  app treats a failed send as non-fatal and carries on detecting locally.
