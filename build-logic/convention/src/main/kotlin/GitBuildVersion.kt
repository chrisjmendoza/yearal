import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.kotlin.dsl.of
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

// Build identity for :app (docs/ARCHITECTURE.md §7 "Versioning", docs/release-builds.md "Version
// numbers"). Every build carries the commit it was made from, so two installs — or two feedback
// emails — are never both just "0.1.0".

/**
 * Play rejects a `versionCode` above 2,100,000,000; failing a little earlier leaves headroom for a
 * one-off manual bump without ever producing an unuploadable build.
 */
private const val MAX_VERSION_CODE = 2_000_000_000L

/**
 * The `versionCode` and `versionName` an `:app` build is stamped with.
 *
 * @property versionCode the commit count of `HEAD` (or the fallback in [resolveBuildVersion]).
 * @property versionName `VERSION_NAME` plus SemVer build metadata, e.g. `0.1.0+112.72dbfa1` or
 *   `0.1.0+112.72dbfa1.dirty`. **Not plain SemVer** — anything that needs the release number alone must
 *   cut it at the first `+`.
 */
data class BuildVersion(val versionCode: Long, val versionName: String)

/**
 * Asks git, once per configuration, for `HEAD`'s commit count, its 7-character short hash and whether
 * the worktree has uncommitted changes, returned as `"<count> <sha> <clean|dirty>"`, or `"!<reason>"`
 * when git cannot answer (not installed, not a checkout, a shallow clone).
 *
 * A [ValueSource] rather than three `providers.exec` calls because the configuration cache fingerprints
 * a value source by its *result*: the cache is invalidated only when a commit lands or the worktree flips
 * between clean and dirty. A `providers.exec { git status --porcelain }` would be fingerprinted by its
 * whole output — the list of modified files — and throw away the cache every time one more file is
 * touched; and `providers.exec` cannot report "git is not installed" without failing configuration.
 */
abstract class GitHeadValueSource : ValueSource<String, GitHeadValueSource.Parameters> {
    /** Where to run git: the repository root. */
    interface Parameters : ValueSourceParameters {
        /** The directory git runs in; any directory inside the checkout works. */
        val workingDirectory: DirectoryProperty
    }

    /** Gradle's process launcher, injected. */
    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String {
        val shallow = git("rev-parse", "--is-shallow-repository") ?: return "!not a git checkout, or git is not on PATH"
        // A shallow clone (CI's default fetch-depth: 1) counts only the commits it fetched, so its count
        // would silently go backwards; treat it as "unknown" instead.
        if (shallow == "true") return "!shallow clone (fetch the full history, e.g. fetch-depth: 0)"
        val count = git("rev-list", "--count", "HEAD") ?: return "!git rev-list failed"
        val sha = git("rev-parse", "--short=7", "HEAD") ?: return "!git rev-parse failed"
        val status = git("status", "--porcelain") ?: return "!git status failed"
        return "$count $sha ${if (status.isEmpty()) "clean" else "dirty"}"
    }

    /** Runs `git args` in the working directory; trimmed stdout on success, `null` on any failure. */
    private fun git(vararg args: String): String? {
        val stdout = ByteArrayOutputStream()
        return try {
            val result =
                execOperations.exec {
                    workingDir = parameters.workingDirectory.get().asFile
                    commandLine(listOf("git") + args)
                    standardOutput = stdout
                    errorOutput = ByteArrayOutputStream()
                    isIgnoreExitValue = true
                }
            if (result.exitValue == 0) stdout.toString(Charsets.UTF_8).trim() else null
        } catch (e: Exception) {
            // The git executable itself could not be started (not installed / not on PATH).
            null
        }
    }
}

/**
 * The [BuildVersion] for this build (docs/ARCHITECTURE.md §7 "Versioning"):
 *
 * - `versionCode` = `git rev-list --count HEAD`. `main` only ever moves by fast-forward
 *   (docs/WORKFLOW.md §1), so its commit count never goes down between two builds of `main` — which is
 *   what makes it a valid Play `versionCode` (Play requires each upload to be higher than the last).
 * - `versionName` = `"$VERSION_NAME+$count.$shortSha"`, with `.dirty` appended when the worktree has
 *   uncommitted changes (SemVer build metadata; harmless for Play, and it tells a feedback email apart
 *   from a clean build).
 *
 * When git cannot answer (a source archive, no git on PATH, a shallow clone), the `VERSION_BUILD` Gradle
 * property is used as the `versionCode` if given, else `1`, and the name ends in `.unknown`; a warning
 * says so. Fails the build if the resulting `versionCode` is not in `1..2_000_000_000`; the value is range-checked as a `Long` before
 * the caller narrows it to the `Int` AGP takes.
 */
fun Project.resolveBuildVersion(): BuildVersion {
    val semver = providers.gradleProperty("VERSION_NAME").get()
    val head =
        providers
            .of(GitHeadValueSource::class) {
                parameters.workingDirectory.set(rootProject.layout.projectDirectory)
            }.get()

    val version =
        if (!head.startsWith("!")) {
            val (count, sha, state) = head.split(' ')
            val suffix = if (state == "dirty") ".dirty" else ""
            BuildVersion(count.toLong(), "$semver+$count.$sha$suffix")
        } else {
            val build = providers.gradleProperty("VERSION_BUILD").orNull?.toLong()
            logger.warn(
                "Cannot derive the build number from git (${head.removePrefix("!")}); using " +
                    (if (build != null) "VERSION_BUILD=$build" else "versionCode 1") +
                    ". Installs of this build cannot be told apart from other fallback builds. " +
                    "See docs/release-builds.md \"Version numbers\".",
            )
            BuildVersion(build ?: 1L, "$semver+${build ?: 0}.unknown")
        }

    if (version.versionCode !in 1..MAX_VERSION_CODE) {
        throw GradleException(
            "versionCode ${version.versionCode} is outside 1..$MAX_VERSION_CODE (Play's hard cap is " +
                "2,100,000,000). See docs/ARCHITECTURE.md §7 \"Versioning\".",
        )
    }
    return version
}
