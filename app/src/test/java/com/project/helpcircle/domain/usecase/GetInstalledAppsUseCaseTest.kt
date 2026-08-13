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
            AppInfo("com.zebra", "zebra", AppCategory.OTHER),
            AppInfo("com.apple", "Apple", AppCategory.OTHER),
            AppInfo("com.mango", "mango", AppCategory.OTHER)
        )
        val useCase = GetInstalledAppsUseCase(FakeInstalledAppsRepository(apps))

        val result = useCase()

        assertEquals(listOf("Apple", "mango", "zebra"), result.map { it.displayName })
    }
}
