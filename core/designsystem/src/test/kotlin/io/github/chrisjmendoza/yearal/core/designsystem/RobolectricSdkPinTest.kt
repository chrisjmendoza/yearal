package io.github.chrisjmendoza.yearal.core.designsystem

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every Android module's Robolectric runtime is pinned to the app's `targetSdk`
 * (`gradle/libs.versions.toml`) by `configureIfcAndroid` (ROADMAP R7), never to whatever newest SDK
 * Robolectric itself ships. A library module's test manifest has no `targetSdk` of its own, so without
 * the pin Robolectric defaults to a newer runtime than the Compose test rule's Espresso input injection
 * supports, and every Compose interaction test fails with a cryptic `InputManager.getInstance()`
 * `NoSuchMethodException` that names none of this. This test pins the guarantee directly, in a module
 * with no Robolectric properties file of its own to hide behind: if it ever fails, the catalog's
 * `targetSdk` changed and this expected value needs updating alongside it, never the other way round.
 */
@RunWith(AndroidJUnit4::class)
class RobolectricSdkPinTest {
    @Test
    fun `robolectric runs the app's targetSdk, not its own newest SDK`() {
        Build.VERSION.SDK_INT shouldBe 36
    }
}
