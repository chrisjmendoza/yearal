package io.github.chrisjmendoza.yearal.core.designsystem.adaptive

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [currentWindowWidthClass] against real breakpoints (docs/ARCHITECTURE.md §4 "Adaptive layouts"):
 * narrower than 600dp is [WindowWidthClass.COMPACT], 600..839dp is [WindowWidthClass.MEDIUM], 840dp
 * and up is [WindowWidthClass.EXPANDED] — and a live resize (the Compose analogue of a fold/unfold or
 * a desktop-window drag-resize, docs/ROADMAP.md M3 T4) recomposes into the new bucket without a
 * configuration change tearing anything down, proven with [DeviceConfigurationOverride.WindowSize]
 * rather than a real device.
 */
@RunWith(AndroidJUnit4::class)
class WindowWidthClassTest {
    @get:Rule
    val compose = createComposeRule()

    private fun widthClassAt(width: Dp): WindowWidthClass {
        var result: WindowWidthClass? = null
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(DpSize(width, HEIGHT))) {
                result = currentWindowWidthClass()
            }
        }
        compose.waitForIdle()
        return checkNotNull(result)
    }

    @Test
    fun `narrower than 600dp is compact`() {
        widthClassAt(599.dp) shouldBe WindowWidthClass.COMPACT
    }

    @Test
    fun `600dp is medium`() {
        widthClassAt(600.dp) shouldBe WindowWidthClass.MEDIUM
    }

    @Test
    fun `839dp is still medium`() {
        widthClassAt(839.dp) shouldBe WindowWidthClass.MEDIUM
    }

    @Test
    fun `840dp is expanded`() {
        widthClassAt(840.dp) shouldBe WindowWidthClass.EXPANDED
    }

    @Test
    fun `a live resize recomposes into the new bucket without a configuration change`() {
        var width by mutableStateOf(360.dp)
        var seen: WindowWidthClass? = null
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(DpSize(width, HEIGHT))) {
                seen = currentWindowWidthClass()
                Text("width class: $seen")
            }
        }
        compose.onNodeWithText("width class: ${WindowWidthClass.COMPACT}").assertExists()

        // A fold-out or a desktop drag-resize to a tablet-sized window, still the same composition.
        width = 900.dp
        compose.waitForIdle()

        seen shouldBe WindowWidthClass.EXPANDED
        compose.onNodeWithText("width class: ${WindowWidthClass.EXPANDED}").assertExists()

        // Fold back in.
        width = 360.dp
        compose.waitForIdle()

        seen shouldBe WindowWidthClass.COMPACT
    }

    private companion object {
        val HEIGHT = 800.dp
    }
}
