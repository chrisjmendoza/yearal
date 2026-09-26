import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/** The `libs` version catalog, for use from the precompiled convention scripts. */
val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/**
 * Configuration shared by every Android module (application and library): SDK levels from the version
 * catalog, Java 17 bytecode, Kotlin warnings as errors, Robolectric-ready unit tests and Android Lint
 * as a CI error gate. See docs/ARCHITECTURE.md §1 ("Build and platform") and docs/adr/0001-toolchain.md.
 *
 * AGP 9 compiles Kotlin itself (built-in Kotlin, new DSL); modules never apply
 * `org.jetbrains.kotlin.android`. Under the new DSL, `CommonExtension` exposes its sub-blocks as
 * properties only (the `block { }` lambdas moved to the application/library extensions), hence `.apply`.
 */
fun Project.configureIfcAndroid(extension: CommonExtension) {
    extension.compileSdk = libs.findVersion("compileSdk").get().requiredVersion.toInt()

    extension.defaultConfig.apply {
        minSdk = libs.findVersion("minSdk").get().requiredVersion.toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    extension.compileOptions.apply {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Robolectric SDK pin (ROADMAP R7), generated instead of hand-copied. A library module's test
    // manifest has no `targetSdk`, so without a pin Robolectric defaults to the newest SDK it ships,
    // ahead of the app's targetSdk and of what the Compose test rule's Espresso input injection
    // supports — every Compose interaction test then fails with a cryptic `InputManager.getInstance()`
    // `NoSuchMethodException`. **Verified empirically that this must be a `robolectric.properties` file,
    // not a system property**: Robolectric's `sdk` config value is read only from
    // `Config.Implementation.fromProperties` via `PackagePropertiesLoader` (a classpath resource lookup);
    // the `robolectric.<key>` system-property override that its docs advertise only wires up the
    // enum-valued configs (`LooperMode`, `GraphicsMode`, ...) through `SingleValueConfigurer` — `sdk` has
    // no such path, so `systemProperty("robolectric.sdk", ...)` is silently ignored (confirmed by
    // deleting `:feature:calendar`'s hand-written file and watching its Compose tests fail with the
    // `InputManager` exception even with that system property set). The generated file lands on the
    // `Test` task's own classpath below — see that comment for why a `testImplementation` file
    // dependency does not work either — and keeps the pin from ever drifting apart from the catalog's
    // own `targetSdk`.
    val robolectricSdk = libs.findVersion("targetSdk").get().requiredVersion
    val robolectricPropertiesDir = layout.buildDirectory.dir("generated/robolectricProperties")
    val generateRobolectricProperties = tasks.register("generateRobolectricProperties") {
        outputs.dir(robolectricPropertiesDir)
        doLast {
            robolectricPropertiesDir.get().file("robolectric.properties").asFile.apply {
                parentFile.mkdirs()
                writeText("sdk=$robolectricSdk\n")
            }
        }
    }

    extension.testOptions.unitTests.apply {
        // Robolectric and Roborazzi need the merged resources and manifest on the JVM.
        isIncludeAndroidResources = true
        all { test ->
            // Robolectric 4.17's API 37 runtime reaches into jdk.internal.access, which JDK 17+
            // encapsulates; without this every Robolectric test fails in setUpApplicationState.
            test.jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED")
            // Roborazzi's own setup check recommends hardware PixelCopy rendering for screenshot
            // fidelity (ROADMAP R6 / M2 T10); harmless on modules with no Roborazzi captures.
            test.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
            // The generated robolectric.properties directory is prepended straight onto the Test task's
            // own classpath rather than declared as a `testImplementation` file dependency: AGP's variant
            // dependency model does not carry a plain `FileCollectionDependency` added to that bucket
            // through to the resolved per-variant unit-test runtime classpath (verified empirically — it
            // showed up in the raw `testImplementation` configuration's declared dependencies but never
            // in `testDebugUnitTest`'s actual `classpath`), so the file dependency route silently drops
            // the pin. Mutating `classpath` directly is what the Test task's own JVM process reads.
            test.dependsOn(generateRobolectricProperties)
            test.classpath = files(robolectricPropertiesDir).plus(test.classpath)
        }
    }

    extension.lint.apply {
        warningsAsErrors = true
        abortOnError = true
        // "A newer version is available" must never turn the build red on its own; dependency
        // updates are Dependabot's job (docs/ARCHITECTURE.md §7). OldTargetApi: targetSdk 36 below
        // compileSdk 37 is deliberate until the Android 17 behaviour changes are reviewed
        // (docs/ARCHITECTURE.md §1, "Beyond 1.0" in ROADMAP.md).
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable", "OldTargetApi")
    }

    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            allWarningsAsErrors.set(true)
        }
    }
}
