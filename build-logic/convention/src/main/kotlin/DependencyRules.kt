import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

// Module-boundary checks (ROADMAP M0 T4) for docs/ARCHITECTURE.md §2 "Dependency direction" and
// CLAUDE.md rules 1, 10 and 11. Both functions walk every `implementation`/`api` configuration's
// declared project dependencies at configuration time — the same shape the original
// `ifc.android.feature` check used, generalised so it is not copy-pasted per convention plugin.

/**
 * Fails the build if this project declares an `implementation`/`api` dependency on another project
 * whose path starts with one of [forbiddenPrefixes]. Used for a deny-list rule ("never depend on a
 * feature, or on :core:data") — see [restrictProjectDependenciesTo] for the pure-JVM allow-list rule.
 */
fun Project.forbidProjectDependencies(
    forbiddenPrefixes: List<String>,
    ruleCitation: String,
) {
    afterEvaluate {
        configurations
            .filter { it.name.endsWith("implementation", ignoreCase = true) || it.name.endsWith("api", ignoreCase = true) }
            .forEach { configuration ->
                configuration.dependencies
                    .filterIsInstance<ProjectDependency>()
                    .map { it.path }
                    .filter { path -> forbiddenPrefixes.any { path.startsWith(it) } }
                    .forEach { path ->
                        throw GradleException(
                            "${project.path} may not depend on $path via ${configuration.name}: $ruleCitation",
                        )
                    }
            }
    }
}

/**
 * Fails the build if this project declares an `implementation`/`api` dependency on another project
 * whose path is not in [allowedPaths]. Used for the pure-JVM allow-list rule (CLAUDE.md rules 1 and 11):
 * an empty [allowedPaths] means the project may depend on nothing in the build at all.
 */
fun Project.restrictProjectDependenciesTo(
    allowedPaths: Set<String>,
    ruleCitation: String,
) {
    afterEvaluate {
        configurations
            .filter { it.name.endsWith("implementation", ignoreCase = true) || it.name.endsWith("api", ignoreCase = true) }
            .forEach { configuration ->
                configuration.dependencies
                    .filterIsInstance<ProjectDependency>()
                    .map { it.path }
                    .filterNot { it in allowedPaths }
                    .forEach { path ->
                        throw GradleException(
                            "${project.path} may not depend on $path via ${configuration.name}: $ruleCitation",
                        )
                    }
            }
    }
}
