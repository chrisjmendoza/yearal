package io.github.chrisjmendoza.yearal.widget.month

import android.content.Context
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.widget.R
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith

/**
 * `res/layout/month_widget_preview.xml` (ROADMAP M5 T5) is a real, RemoteViews-safe static mock-up of
 * the Month widget: it must inflate cleanly and show the fixed sample month title, the Gregorian span
 * directly beneath it, both weekday header rows, and a full 28-day grid with today (day 8) marked and
 * every cell carrying its Gregorian day beneath the IFC one.
 */
@RunWith(AndroidJUnit4::class)
class MonthWidgetPreviewLayoutTest {
    private companion object {
        /** Child indices of the root column, in render order. */
        private const val TITLE = 0
        private const val SPAN = 1
        private const val NOMINAL_HEADERS = 2
        private const val ACTUAL_HEADERS = 3
        private val GRID_ROWS = 4..7
    }

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun rowTexts(row: LinearLayout): List<String> =
        (0 until row.childCount).map { (row.getChildAt(it) as TextView).text.toString() }

    private fun root() = LayoutInflater.from(context).inflate(R.layout.month_widget_preview, null) as LinearLayout

    private fun gridCells(root: LinearLayout) =
        GRID_ROWS.map { root.getChildAt(it) as LinearLayout }.flatMap(::rowTexts)

    @Test
    fun `the title and the Gregorian span are the fixed sample values, the span directly under the title`() {
        val root = root()

        (root.getChildAt(TITLE) as TextView).text.toString() shouldBe
            context.getString(R.string.widget_preview_month_title)
        (root.getChildAt(SPAN) as TextView).text.toString() shouldBe
            context.getString(R.string.widget_preview_month_gregorian_span)
    }

    @Test
    fun `both weekday header rows have seven cells, nominal Sunday-first and actual Thursday-first`() {
        val root = root()
        val nominalRow = root.getChildAt(NOMINAL_HEADERS) as LinearLayout
        val actualRow = root.getChildAt(ACTUAL_HEADERS) as LinearLayout

        rowTexts(nominalRow) shouldBe listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        rowTexts(actualRow) shouldBe listOf("Thu", "Fri", "Sat", "Sun", "Mon", "Tue", "Wed")
    }

    @Test
    fun `the grid has four rows of seven days, IFC 1 through 28 on each cell's first line`() {
        val ifcDays = gridCells(root()).map { it.substringBefore('\n') }

        ifcDays shouldBe (1..28).map { it.toString() }
    }

    @Test
    fun `each cell's second line is its Gregorian day, September 10 through October 7`() {
        val gregorianDays = gridCells(root()).map { it.split('\n')[1] }

        // IFC September 2026 runs Gregorian Sep 10 - Oct 7 (widget_preview_month_gregorian_span), so the
        // 28 cells are Sep 10..30 followed by Oct 1..7 -- the Gregorian month rolls over mid-grid, which
        // is the whole reason a reader needs this second number.
        gregorianDays shouldBe ((10..30) + (1..7)).map { it.toString() }
    }

    @Test
    fun `today (day 8) and the illustrative day 21 carry the event-dot glyph on a third line`() {
        val allCellText = gridCells(root())

        allCellText.filter { it.contains('•') } shouldBe listOf("8\n17\n•", "21\n30\n•")
    }
}
