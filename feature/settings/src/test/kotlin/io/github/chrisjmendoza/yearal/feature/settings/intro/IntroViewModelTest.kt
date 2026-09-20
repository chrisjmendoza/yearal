package io.github.chrisjmendoza.yearal.feature.settings.intro

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
 * [IntroViewModel]: [IntroViewModel.markSeen] writes `hasSeenIntro = true` through
 * [io.github.chrisjmendoza.yearal.core.domain.settings.SettingsRepository] and is idempotent, covering
 * "skipping marks it seen" and "finishing marks it seen" (`docs/FEATURES.md` L1) — [IntroScreenTest]
 * proves the screen calls it from the right places; this proves what it does once called.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IntroViewModelTest {
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
    fun `markSeen stores hasSeenIntro true`() =
        runTest(dispatcher) {
            val repository = FakeSettingsRepository(UserSettings.DEFAULT)
            val viewModel = IntroViewModel(repository)
            repository.current.hasSeenIntro shouldBe false

            viewModel.markSeen()
            dispatcher.scheduler.advanceUntilIdle()

            repository.current.hasSeenIntro shouldBe true
        }

    @Test
    fun `markSeen is idempotent - calling it again keeps the flag true`() =
        runTest(dispatcher) {
            val repository = FakeSettingsRepository(UserSettings.DEFAULT.copy(hasSeenIntro = true))
            val viewModel = IntroViewModel(repository)

            viewModel.markSeen()
            dispatcher.scheduler.advanceUntilIdle()

            repository.current.hasSeenIntro shouldBe true
        }

    @Test
    fun `markSeen never touches any other setting`() =
        runTest(dispatcher) {
            val nonDefault = UserSettings.DEFAULT.copy(dynamicColor = false)
            val repository = FakeSettingsRepository(nonDefault)
            val viewModel = IntroViewModel(repository)

            viewModel.markSeen()
            dispatcher.scheduler.advanceUntilIdle()

            repository.current shouldBe nonDefault.copy(hasSeenIntro = true)
        }
}
