tasks.register("installGitHooks") {
    group = "build setup"
    description = "Install Git hooks for pre-commit and pre-push checks"
    
    doLast {
        val gitHooksDir = File(project.rootDir, ".git/hooks")
        if (!gitHooksDir.exists()) {
            logger.warn("Git hooks directory not found. Make sure you're in a Git repository.")
            return@doLast
        }
        
        // Create pre-commit hook
        val preCommitHook = File(gitHooksDir, "pre-commit")
        preCommitHook.writeText("""
            #!/bin/sh
            # Pre-commit hook to run ktlint check
            
            echo "Running pre-commit checks..."
            
            # Run ktlint check
            ./gradlew --no-daemon --stacktrace ktlintCheck
            
            # Check the result
            if [ \$? -ne 0 ]; then
                echo "Pre-commit checks failed. Please fix the issues before committing."
                exit 1
            fi
            
            echo "Pre-commit checks passed."
            exit 0
        """.trimIndent())
        
        // Make pre-commit hook executable
        preCommitHook.setExecutable(true)
        
        // Create pre-push hook
        val prePushHook = File(gitHooksDir, "pre-push")
        prePushHook.writeText("""
            #!/bin/sh
            # Pre-push hook to run comprehensive checks
            
            echo "Running pre-push checks..."
            
            # Run all checks
            ./gradlew --no-daemon --stacktrace clean check detektAll ktlintCheck versionCheck schemaCompatibilityCheck
            
            # Check the result
            if [ \$? -ne 0 ]; then
                echo "Pre-push checks failed. Please fix the issues before pushing."
                exit 1
            fi
            
            echo "Pre-push checks passed."
            exit 0
        """.trimIndent())
        
        // Make pre-push hook executable
        prePushHook.setExecutable(true)
        
        logger.lifecycle("Git hooks installed successfully!")
        logger.lifecycle("Pre-commit hook: Runs ktlintCheck")
        logger.lifecycle("Pre-push hook: Runs clean check detektAll ktlintCheck versionCheck schemaCompatibilityCheck")
    }
}