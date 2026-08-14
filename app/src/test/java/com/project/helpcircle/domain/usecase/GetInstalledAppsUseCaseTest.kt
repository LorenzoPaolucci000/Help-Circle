package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.AppCategory
import com.project.helpcircle.domain.model.AppInfo
import com.project.helpcircle.domain.repository.InstalledAppsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeInstalledAppsRepository(private val apps: List<AppInfo>) : InstalledAppsRepository {
    override suspend fun getInstalledApps(): List<AppInfo> = apps
}

class GetInstalledAppsUseCaseTest {

    @Test
    fun `sorts apps case-insensitively by display name`() = runBlocking {
        val apps = listOf(
            AppInfo("com.zebra", "zebra", AppCategory.SOCIAL),
            AppInfo("com.apple", "Apple", AppCategory.VIDEO),
            AppInfo("com.mango", "mango", AppCategory.SOCIAL)
        )
        val useCase = GetInstalledAppsUseCase(FakeInstalledAppsRepository(apps))

        val result = useCase()

        assertEquals(listOf("Apple", "mango", "zebra"), result.map { it.displayName })
    }

    @Test
    fun `excludes every category other than Social and Video`() = runBlocking {
        val socialApp = AppInfo("com.social", "SocialApp", AppCategory.SOCIAL)
        val videoApp = AppInfo("com.video", "VideoApp", AppCategory.VIDEO)
        val apps = listOf(
            socialApp,
            videoApp,
            AppInfo("com.game", "GameApp", AppCategory.GAME),
            AppInfo("com.news", "NewsApp", AppCategory.NEWS),
            AppInfo("com.productivity", "ProductivityApp", AppCategory.PRODUCTIVITY),
            AppInfo("com.other", "OtherApp", AppCategory.OTHER)
        )
        val useCase = GetInstalledAppsUseCase(FakeInstalledAppsRepository(apps))

        val result = useCase()

        assertEquals(listOf(socialApp, videoApp), result)
    }

    @Test
    fun `includes an allowlisted package even when it's a system app with a category other than Social or Video`() = runBlocking {
        // Mirrors a real device: YouTube ships pre-installed as a system app on GMS-certified
        // phones and doesn't self-declare Social or Video, yet must still be monitorable.
        val youtube = AppInfo("com.google.android.youtube", "YouTube", AppCategory.OTHER, isSystemApp = true)
        val useCase = GetInstalledAppsUseCase(FakeInstalledAppsRepository(listOf(youtube)))

        val result = useCase()

        assertEquals(listOf(youtube), result)
    }

    @Test
    fun `excludes a non-allowlisted system app even when its declared category is Social`() = runBlocking {
        // Mirrors a real device: the phone's own pre-installed Messages app self-declares Social.
        val stockMessagingApp = AppInfo("com.google.android.apps.messaging", "Messages", AppCategory.SOCIAL, isSystemApp = true)
        val useCase = GetInstalledAppsUseCase(FakeInstalledAppsRepository(listOf(stockMessagingApp)))

        val result = useCase()

        assertEquals(emptyList<AppInfo>(), result)
    }

    @Test
    fun `includes every TikTok package variant even when its declared category is wrong`() = runBlocking {
        // The Lite/Go build is the one actually installed on the project's test device, and used to
        // be picked up only by its self-declared Social category — the allowlist removes that
        // dependency for all three shipping ids.
        val tikTok = AppInfo("com.zhiliaoapp.musically", "TikTok", AppCategory.OTHER)
        val tikTokLite = AppInfo("com.zhiliaoapp.musically.go", "TikTok Lite", AppCategory.OTHER)
        val tikTokTrill = AppInfo("com.ss.android.ugc.trill", "TikTok", AppCategory.OTHER)
        val useCase = GetInstalledAppsUseCase(
            FakeInstalledAppsRepository(listOf(tikTok, tikTokLite, tikTokTrill))
        )

        val result = useCase()

        assertEquals(setOf(tikTok, tikTokLite, tikTokTrill), result.toSet())
    }

    @Test
    fun `excludes a blocklisted package even when its declared category is Social`() = runBlocking {
        val whatsApp = AppInfo("com.whatsapp", "WhatsApp", AppCategory.SOCIAL)
        val useCase = GetInstalledAppsUseCase(FakeInstalledAppsRepository(listOf(whatsApp)))

        val result = useCase()

        assertEquals(emptyList<AppInfo>(), result)
    }

    @Test
    fun `blocklist excludes every configured messaging package regardless of category`() = runBlocking {
        val messenger = AppInfo("com.facebook.orca", "Messenger", AppCategory.SOCIAL)
        val useCase = GetInstalledAppsUseCase(FakeInstalledAppsRepository(listOf(messenger)))

        val result = useCase()

        assertEquals(emptyList<AppInfo>(), result)
    }
}
