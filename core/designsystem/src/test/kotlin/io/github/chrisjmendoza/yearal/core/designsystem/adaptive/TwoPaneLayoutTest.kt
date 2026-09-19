package io.github.chrisjmendoza.yearal.core.designsystem.adaptive

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.ints.shouldBeGreaterThan
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [TwoPaneLayout] (docs/ARCHITECTURE.md §4 "Adaptive layouts"): both slots are always composed side
 * by side, and the list pane is narrower than the detail pane by default (FEATURES C11's "list on the
 * left, detail on the right" split).
 */
@RunWith(AndroidJUnit4::class)
class TwoPaneLayoutTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `both the list and the detail pane are composed at once`() {
        compose.setContent {
            TwoPaneLayout(
                modifier = Modifier.size(800.dp, 600.dp),
                list = { Text("the list") },
                detail = { Text("the detail") },
            )
        }

        compose.onNodeWithText("the list").assertExists()
        compose.onNodeWithText("the detail").assertExists()
    }

    @Test
    fun `the list pane is narrower than the detail pane by default`() {
        compose.setContent {
            TwoPaneLayout(
                modifier = Modifier.size(1000.dp, 600.dp),
                list = { Text("the list", modifier = Modifier.fillMaxSize().testTag("list")) },
                detail = { Text("the detail", modifier = Modifier.fillMaxSize().testTag("detail")) },
            )
        }

        val listWidth =
            compose
                .onNodeWithTag("list")
                .fetchSemanticsNode()
                .size.width
        val detailWidth =
            compose
                .onNodeWithTag("detail")
                .fetchSemanticsNode()
                .size.width
        detailWidth shouldBeGreaterThan listWidth
    }
}
