package io.github.chrisjmendoza.yearal.feature.settings.more

import app.cash.turbine.test
import io.github.chrisjmendoza.yearal.core.domain.settings.ColorSource
import io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings
import io.github.chrisjmendoza.yearal.core.testing.FakeSettingsRepository
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * [MoreViewModel.settings] mirrors [io.github.chrisjmendoza.yearal.core.domain.settings.SettingsRepository.settings]
 * -- the only state the More hub needs, for the "Send feedback" row's diagnostics (ROADMAP M8 T6).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MoreViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `settings starts at the default and then follows the repository`() =
        runTest(dispatcher) {
            val repository = FakeSettingsRepository(UserSettings.DEFAULT)
            val viewModel = MoreViewModel(repository)

            viewModel.settings.test {
                awaitItem() shouldBe UserSettings.DEFAULT

                repository.update { it.copy(colorSource = ColorSource.DYNAMIC) }

                awaitItem() shouldBe UserSettings.DEFAULT.copy(colorSource = ColorSource.DYNAMIC)
            }
        }
}
