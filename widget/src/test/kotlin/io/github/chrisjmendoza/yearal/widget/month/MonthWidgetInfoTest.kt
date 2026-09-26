package io.github.chrisjmendoza.yearal.widget.month

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.widget.R
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

/**
 * Proves the values in `res/xml/month_widget_info.xml` (ROADMAP M5 T3; docs/ARCHITECTURE.md §5
 * "Configuration"): the 4-hour self-healing backstop, both resize directions, `home_screen` only (not
 * `keyguard`, `docs/security-and-privacy.md` §3.2), and the min/target size, raised since ROADMAP M8 T1
 * (accessibility audit finding #14) as far toward the 48dp touch-target floor as a 360dp-wide phone's
 * launcher grid allows -- no longer [MonthGlanceWidget.COMPACT]'s own 250dp, which is a
 * `SizeMode.Responsive` breakpoint for content, not a touch-target size, and not the full 352dp the
 * floor itself would need, which the XML file's own comment explains would make the widget unplaceable
 * on that phone.
 */
@RunWith(AndroidJUnit4::class)
class MonthWidgetInfoTest {
    private fun rootAttributes(): Map<String, String> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = context.resources.getXml(R.xml.month_widget_info)
        var eventType = parser.eventType
        while (eventType != XmlPullParser.START_TAG) {
            eventType = parser.next()
        }
        return (0 until parser.attributeCount).associate { index ->
            parser.getAttributeName(index) to parser.getAttributeValue(index)
        }
    }

    /** A dimension attribute like `250dp` may come back as `250dp` or a coerced `250.0dip`. */
    private fun magnitudeOf(value: String): Float = Regex("""[0-9.]+""").find(value)!!.value.toFloat()

    @Test
    fun `updatePeriodMillis is the four hour self-healing backstop, same as the Today widget`() {
        val fourHoursMillis = 4 * 60 * 60 * 1000
        rootAttributes()["updatePeriodMillis"]!!.toInt() shouldBe fourHoursMillis
    }

    @Test
    fun `resize mode allows both directions`() {
        val mode = rootAttributes().getValue("resizeMode")
        mode shouldBe "0x3"
    }

    @Test
    fun `widget category is home screen only, never keyguard`() {
        val category = rootAttributes().getValue("widgetCategory")
        category shouldBe "0x1"
    }

    /**
     * `minWidth`/`minResizeWidth` were raised from 250dp to 320dp (ROADMAP M8 T1, accessibility audit
     * finding #14) -- five nominal 70dp home-screen cells (`70 * 5 - 30 = 320`), not the 352dp the 48dp
     * touch-target floor itself needs: 352dp+ does not fit a 360dp-wide phone's launcher grid and would
     * make the widget unplaceable there, a worse outcome than a too-small cell. At 320dp a column is
     * `(320 - 16) / 7 ~= 43.4dp`, about 5dp under the floor; the floor is only met once a placement
     * reaches six or more nominal cells (about 390dp and up). See the XML file's own comment for the
     * full arithmetic. `minHeight` is unchanged -- the finding was about the columns, not the rows.
     */
    @Test
    fun `min width is the largest value that still fits a 360dp phone, min height is unchanged`() {
        val attrs = rootAttributes()
        magnitudeOf(attrs.getValue("minWidth")) shouldBe 320f
        magnitudeOf(attrs.getValue("minHeight")) shouldBe 180f
    }

    @Test
    fun `max resize width stays a valid upper bound above the new min width`() {
        val attrs = rootAttributes()
        magnitudeOf(attrs.getValue("maxResizeWidth")) shouldBe 460f
        magnitudeOf(attrs.getValue("maxResizeHeight")) shouldBe 320f
    }

    @Test
    fun `target cell width matches the new min width, in cell units`() {
        val attrs = rootAttributes()
        attrs.getValue("targetCellWidth").toInt() shouldBe 5
        attrs.getValue("targetCellHeight").toInt() shouldBe 3
    }

    @Test
    fun `initial and preview layouts, and the description, are declared`() {
        val attrs = rootAttributes()
        attrs.getValue("initialLayout") shouldBe "@${R.layout.month_widget_loading}"
        // ROADMAP M5 T5: a real static mock-up, not the loading placeholder.
        attrs.getValue("previewLayout") shouldBe "@${R.layout.month_widget_preview}"
        attrs.getValue("description") shouldBe "@${R.string.month_widget_description}"
    }

    /** No `android:configure`: the config activity is ROADMAP M5 T4, out of scope here. */
    @Test
    fun `has no configuration activity`() {
        rootAttributes().containsKey("configure") shouldBe false
    }

    /**
     * `previewImage` (ROADMAP M5 T5) is valid since API 11 (verified against the SDK's own
     * `api-versions.xml`), so it lives in the base file rather than needing its own resource-qualifier
     * split, and stays declared at every API level this app supports.
     */
    @Test
    @Config(sdk = [26])
    fun `below API 31, previewImage is the static vector fallback`() {
        rootAttributes().getValue("previewImage") shouldBe "@${R.drawable.widget_preview_image}"
    }
}
