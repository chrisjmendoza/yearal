package io.github.chrisjmendoza.yearal.feature.settings.settings

import app.cash.turbine.test
import io.github.chrisjmendoza.yearal.core.domain.settings.ColorPalette
import io.github.chrisjmendoza.yearal.core.domain.settings.ColorSource
import io.github.chrisjmendoza.yearal.core.domain.settings.ThemeMode
import io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings
import io.github.chrisjmendoza.yearal.core.domain.settings.WeekdayDisplay
import io.github.chrisjmendoza.yearal.core.domain.settings.WidgetTheme
import io.github.chrisjmendoza.yearal.core.testing.EventFixtures
import io.github.chrisjmendoza.yearal.core.testing.FakeEventRepository
import io.github.chrisjmendoza.yearal.core.testing.FakeSettingsRepository
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
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
 * [SettingsViewModel] against [FakeSettingsRepository]: the state is whatever the store holds, and
 * every intent goes through the store and comes back as a new state (docs/ARCHITECTURE.md §4 "State
 * management"; FEATURES W1, W2, H5). Holiday-set toggling itself is `feature:holidays`'s own
 * `HolidaysViewModelTest` now (ROADMAP M6 T2); this ViewModel no longer touches the bundled catalogue.
 *
 * A [StandardTestDispatcher] is used on purpose: with an unconfined one the first value is mapped
 * synchronously on subscription and `stateIn` conflates the initial [SettingsUiState.Loading] away.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        repository: FakeSettingsRepository,
        dynamicColorSupported: Boolean = true,
        eventRepository: FakeEventRepository = FakeEventRepository(),
    ) = SettingsViewModel(repository, dynamicColorSupported, eventRepository)

    @Test
    fun `starts loading, then mirrors the stored settings`() =
        runTest(dispatcher) {
            val stored =
                UserSettings(
                    weekdayDisplay = WeekdayDisplay.NOMINAL,
                    themeMode = ThemeMode.DARK,
                    colorSource = ColorSource.DYNAMIC,
                    enabledHolidaySets = setOf("us"),
                )
            val viewModel = viewModel(FakeSettingsRepository(stored))
            viewModel.uiState.value shouldBe SettingsUiState.Loading
            viewModel.uiState.test {
                awaitItem() shouldBe SettingsUiState.Loading
                val loaded = awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>()
                loaded.settings shouldBe stored
                loaded.dynamicColorSupported shouldBe true
            }
        }

    @Test
    fun `a fresh install shows the defaults - BOTH, system theme, brand colour, IFC and US packs`() =
        runTest(dispatcher) {
            viewModel(FakeSettingsRepository()).uiState.test {
                awaitItem() shouldBe SettingsUiState.Loading
                val loaded = awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>()
                loaded.settings.weekdayDisplay shouldBe WeekdayDisplay.BOTH
                loaded.settings.themeMode shouldBe ThemeMode.SYSTEM
                loaded.settings.colorSource shouldBe ColorSource.BRAND
                loaded.settings.enabledHolidaySets shouldBe setOf("ifc", "us")
            }
        }

    @Test
    fun `dynamicColorSupported reflects the injected flag`() =
        runTest(dispatcher) {
            viewModel(FakeSettingsRepository(), dynamicColorSupported = false).uiState.test {
                awaitItem() shouldBe SettingsUiState.Loading
                awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>().dynamicColorSupported shouldBe false
            }
        }

    @Test
    fun `setWeekdayDisplay updates the repository and the state`() =
        runTest(dispatcher) {
            val repository = FakeSettingsRepository()
            val viewModel = viewModel(repository)
            viewModel.uiState.test {
                awaitItem() shouldBe SettingsUiState.Loading
                awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>().settings.weekdayDisplay shouldBe
                    WeekdayDisplay.BOTH

                viewModel.setWeekdayDisplay(WeekdayDisplay.ACTUAL)

                awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>().settings.weekdayDisplay shouldBe
                    WeekdayDisplay.ACTUAL
                repository.current.weekdayDisplay shouldBe WeekdayDisplay.ACTUAL
                // Everything else is untouched by a read-modify-write.
                repository.current shouldBe UserSettings.DEFAULT.copy(weekdayDisplay = WeekdayDisplay.ACTUAL)
            }
        }

    @Test
    fun `setThemeMode updates the repository and the state`() =
        runTest(dispatcher) {
            val repository = FakeSettingsRepository()
            val viewModel = viewModel(repository)
            viewModel.uiState.test {
                awaitItem() shouldBe SettingsUiState.Loading
                awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>().settings.themeMode shouldBe ThemeMode.SYSTEM

                viewModel.setThemeMode(ThemeMode.DARK)

                awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>().settings.themeMode shouldBe ThemeMode.DARK
                repository.current shouldBe UserSettings.DEFAULT.copy(themeMode = ThemeMode.DARK)
            }
        }

    @Test
    fun `setColorSource updates the repository and the state`() =
        runTest(dispatcher) {
            val repository = FakeSettingsRepository()
            val viewModel = viewModel(repository)
            viewModel.uiState.test {
                awaitItem() shouldBe SettingsUiState.Loading
                awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>().settings.colorSource shouldBe
                    ColorSource.BRAND

                viewModel.setColorSource(ColorSource.DYNAMIC)

                awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>().settings.colorSource shouldBe
                    ColorSource.DYNAMIC
                repository.current shouldBe UserSettings.DEFAULT.copy(colorSource = ColorSource.DYNAMIC)
            }
        }

    @Test
    fun `setPalette updates the repository and the state`() =
        runTest(dispatcher) {
            val repository = FakeSettingsRepository()
            val viewModel = viewModel(repository)
            viewModel.uiState.test {
                awaitItem() shouldBe SettingsUiState.Loading
                awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>().settings.palette shouldBe
                    ColorPalette.TEAL

                viewModel.setPalette(ColorPalette.NIGHT)

                awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>().settings.palette shouldBe
                    ColorPalette.NIGHT
                repository.current shouldBe UserSettings.DEFAULT.copy(palette = ColorPalette.NIGHT)
            }
        }

    @Test
    fun `setPureBlack updates the repository and the state`() =
        runTest(dispatcher) {
            val repository = FakeSettingsRepository()
            val viewModel = viewModel(repository)
            viewModel.uiState.test {
                awaitItem() shouldBe SettingsUiState.Loading
                awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>().settings.pureBlack shouldBe false

                viewModel.setPureBlack(true)

                awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>().settings.pureBlack shouldBe true
                repository.current shouldBe UserSettings.DEFAULT.copy(pureBlack = true)
            }
        }

    @Test
    fun `setTodayWidgetTheme and setMonthWidgetTheme update the repository independently`() =
        runTest(dispatcher) {
            val repository = FakeSettingsRepository()
            val viewModel = viewModel(repository)
            viewModel.uiState.test {
                awaitItem() shouldBe SettingsUiState.Loading
                awaitItem()

                viewModel.setTodayWidgetTheme(WidgetTheme.DARK)
                awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>().settings.todayWidgetTheme shouldBe
                    WidgetTheme.DARK

                viewModel.setMonthWidgetTheme(WidgetTheme.LIGHT)
                awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>().settings.monthWidgetTheme shouldBe
                    WidgetTheme.LIGHT
            }
            repository.current shouldBe
                UserSettings.DEFAULT.copy(
                    todayWidgetTheme = WidgetTheme.DARK,
                    monthWidgetTheme = WidgetTheme.LIGHT,
                )
        }

    @Test
    fun `setWidgetBackgroundOpacity updates the repository and the state`() =
        runTest(dispatcher) {
            val repository = FakeSettingsRepository()
            val viewModel = viewModel(repository)
            viewModel.uiState.test {
                awaitItem() shouldBe SettingsUiState.Loading
                awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>().settings.widgetBackgroundOpacity shouldBe
                    100

                viewModel.setWidgetBackgroundOpacity(45)

                awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>().settings.widgetBackgroundOpacity shouldBe
                    45
                repository.current shouldBe UserSettings.DEFAULT.copy(widgetBackgroundOpacity = 45)
            }
        }

    @Test
    fun `setWidgetBackgroundOpacity coerces an out-of-range value into the 0 to 100 range`() =
        runTest(dispatcher) {
            // Starts from 50, not the 100 default, so each coerced write below is a genuinely new value
            // — otherwise a write that coerces back to the value already stored would emit nothing.
            val repository = FakeSettingsRepository(UserSettings(widgetBackgroundOpacity = 50))
            val viewModel = viewModel(repository)
            viewModel.uiState.test {
                awaitItem() shouldBe SettingsUiState.Loading
                awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>().settings.widgetBackgroundOpacity shouldBe 50

                viewModel.setWidgetBackgroundOpacity(150)
                awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>().settings.widgetBackgroundOpacity shouldBe
                    100

                viewModel.setWidgetBackgroundOpacity(-5)
                awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>().settings.widgetBackgroundOpacity shouldBe 0
            }
        }

    // ----- "Delete all data" (FEATURES W6): two-step destructive confirmation, cancel at each step,
    // the repository and settings both reset. -----

    @Test
    fun `requesting delete opens the first confirmation`() =
        runTest(dispatcher) {
            val viewModel = viewModel(FakeSettingsRepository())
            viewModel.uiState.test {
                awaitItem() shouldBe SettingsUiState.Loading
                awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>().deleteAllDataStep shouldBe
                    DeleteAllDataStep.NONE

                viewModel.requestDeleteAllData()
                awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>().deleteAllDataStep shouldBe
                    DeleteAllDataStep.CONFIRM_FIRST
            }
        }

    @Test
    fun `cancelling the first confirmation changes nothing`() =
        runTest(dispatcher) {
            val settingsRepo = FakeSettingsRepository(UserSettings(themeMode = ThemeMode.DARK))
            val eventRepo = FakeEventRepository()
            eventRepo.seed(listOf(EventFixtures.sol13Yearly()))
            val viewModel = viewModel(settingsRepo, eventRepository = eventRepo)
            viewModel.uiState.test {
                awaitItem() shouldBe SettingsUiState.Loading
                awaitItem()

                viewModel.requestDeleteAllData()
                awaitItem()
                viewModel.cancelDeleteAllData()
                awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>().deleteAllDataStep shouldBe
                    DeleteAllDataStep.NONE
            }
            settingsRepo.current.themeMode shouldBe ThemeMode.DARK
            eventRepo.currentEvents shouldHaveSize 1
        }

    @Test
    fun `cancelling the final confirmation also changes nothing`() =
        runTest(dispatcher) {
            val settingsRepo = FakeSettingsRepository(UserSettings(themeMode = ThemeMode.DARK))
            val eventRepo = FakeEventRepository()
            eventRepo.seed(listOf(EventFixtures.sol13Yearly()))
            val viewModel = viewModel(settingsRepo, eventRepository = eventRepo)
            viewModel.uiState.test {
                awaitItem() shouldBe SettingsUiState.Loading
                awaitItem()

                viewModel.requestDeleteAllData()
                awaitItem()
                viewModel.continueDeleteAllData()
                awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>().deleteAllDataStep shouldBe
                    DeleteAllDataStep.CONFIRM_SECOND
                viewModel.cancelDeleteAllData()
                awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>().deleteAllDataStep shouldBe
                    DeleteAllDataStep.NONE
            }
            settingsRepo.current.themeMode shouldBe ThemeMode.DARK
            eventRepo.currentEvents shouldHaveSize 1
        }

    @Test
    fun `confirming both steps erases every event and resets settings, then shows completion`() =
        runTest(dispatcher) {
            val settingsRepo =
                FakeSettingsRepository(UserSettings(themeMode = ThemeMode.DARK, colorSource = ColorSource.DYNAMIC))
            val eventRepo = FakeEventRepository()
            eventRepo.seed(listOf(EventFixtures.sol13Yearly(), EventFixtures.weeklyGregorian()))
            val viewModel = viewModel(settingsRepo, eventRepository = eventRepo)
            viewModel.uiState.test {
                awaitItem() shouldBe SettingsUiState.Loading
                awaitItem()

                viewModel.requestDeleteAllData()
                awaitItem()
                viewModel.continueDeleteAllData()
                awaitItem()
                viewModel.confirmDeleteAllData()

                awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>().deleteAllDataStep shouldBe
                    DeleteAllDataStep.DONE

                viewModel.dismissDeleteAllDataDone()
                awaitItem().shouldBeInstanceOf<SettingsUiState.Loaded>().deleteAllDataStep shouldBe
                    DeleteAllDataStep.NONE
            }
            eventRepo.currentEvents.shouldBeEmpty()
            settingsRepo.current shouldBe UserSettings.DEFAULT
        }
}
