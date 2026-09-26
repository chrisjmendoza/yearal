import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Convention for pure Kotlin/JVM modules (:core:calendar, :core:domain, :core:holidays).
// Enforces the workflow gates from docs/WORKFLOW.md: explicit API, warnings as errors,
// KDoc on every public declaration, ktlint formatting, JUnit 6 + Kotest.

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")
    id("com.diffplug.spotless")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    "testImplementation"(platform(libs.findLibrary("junit-bom").get()))
    "testImplementation"(libs.findLibrary("junit-jupiter").get())
    "testRuntimeOnly"(libs.findLibrary("junit-platform-launcher").get())
    "testImplementation"(libs.findLibrary("kotest-assertions-core").get())
    "testImplementation"(libs.findLibrary("kotest-property").get())
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("failed", "skipped")
        showStandardStreams = false
    }
}

// KDoc gate: an undocumented public declaration is a build failure.
dokka {
    dokkaSourceSets.configureEach {
        reportUndocumented.set(true)
    }
    dokkaPublications.configureEach {
        failOnWarning.set(true)
    }
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint(libs.findVersion("ktlint").get().requiredVersion)
    }
}

tasks.named("check") {
    dependsOn("dokkaGenerate")
}

// Module-boundary check (ROADMAP M0 T4): a pure-JVM module may depend only on another pure-JVM module,
// never on an Android module or on :core:data (CLAUDE.md rule 11 — this project's minSdk 26 means the
// JDK classes an Android module sees are Android's own, not the desktop JVM's, so a JVM-only module
// pulling one in would compile here and fail on-device in ways this build cannot detect).
// `:core:calendar` is stricter still (CLAUDE.md rule 1: pure, zero project dependencies), hence the
// empty allow-list for it specifically rather than the shared pure-JVM set.
val pureJvmModules = setOf(":core:calendar", ":core:domain", ":core:holidays", ":core:testing")

restrictProjectDependenciesTo(
    allowedPaths = if (project.path == ":core:calendar") emptySet() else pureJvmModules,
    ruleCitation = if (project.path == ":core:calendar") {
        ":core:calendar depends on nothing else in the project (CLAUDE.md rule 1)."
    } else {
        "a pure-JVM module depends only on another pure-JVM module, never an Android module or " +
            ":core:data (CLAUDE.md rule 11)."
    },
)
