# Git Hooks

This project uses Git hooks to enforce code quality checks before allowing commits and pushes. The hooks are automatically installed and managed through Gradle.

## Pre-commit Hook

The pre-commit hook runs a quick ktlint check to ensure code formatting is correct before allowing a commit. This helps catch formatting issues early in the development process.

## Pre-push Hook

The pre-push hook runs a comprehensive set of checks to ensure code quality before allowing a push:

1. **Gradle build and test** - Runs `clean check` to compile code and run tests
2. **Detekt static analysis** - Runs `detektAll` for comprehensive static code analysis
3. **Ktlint check** - Runs `ktlintCheck` to ensure code formatting compliance
4. **Version check** - Runs `versionCheck` to ensure the correct Java version and dependencies
5. **Schema compatibility check** - Runs `schemaCompatibilityCheck` to validate Avro schemas

These checks mirror the CI pipeline to prevent broken code from being pushed to the repository.

## Installation

The Git hooks are automatically installed when you run any Gradle task. If you want to manually install or update the hooks, you can run:

```bash
./gradlew installGitHooks
```

## Configuration

The Git hooks are configured in `build.gradle.kts` using the `gitHooks` extension:

```kotlin
gitHooks {
    prePush {
        command = "./gradlew --no-daemon --stacktrace clean check detektAll ktlintCheck versionCheck schemaCompatibilityCheck"
    }
    
    preCommit {
        command = "./gradlew --no-daemon --stacktrace ktlintCheck"
    }
    
    createHooksAutomatically.set(true)
    failIfHookInstallationFails.set(true)
}
```

## Skipping Hooks

If you need to skip the hooks for a specific commit or push (not recommended), you can use Git's `--no-verify` flag:

```bash
# Skip pre-commit hook
git commit --no-verify -m "Your commit message"

# Skip pre-push hook
git push --no-verify
```

## Troubleshooting

If you encounter issues with the Git hooks:

1. **Check Gradle wrapper** - Ensure the Gradle wrapper is working correctly
2. **Check permissions** - Ensure the hook scripts have execute permissions
3. **Check Java version** - Ensure you're using the correct Java version
4. **Run checks manually** - Try running the Gradle tasks manually to see specific errors

If the hooks are not working, you can manually run the checks:

```bash
# Run pre-commit checks
./gradlew --no-daemon --stacktrace ktlintCheck

# Run pre-push checks
./gradlew --no-daemon --stacktrace clean check detektAll ktlintCheck versionCheck schemaCompatibilityCheck
```