---
alwaysApply: true
description: "Android Debugging Guide (CLI-first with MCP fallback) and mandatory diagnostics gate."
tags:
  - android
  - debugging
  - cli-first
  - diagnostics
---
# Android Debugging Guide (CLI-first)

## Primary Tools (Use These First)

- adb (logcat, pm, am, install/uninstall)
- ./gradlew (clean, assembleDebug, connectedAndroidTest)
- Claude Context MCP for code pattern research (exception to CLI-first)
- mcp-router diagnostics: open_file_in_editor → get_file_problems

## MCP Fallback Tools

- Android MCP: mcp_android_execute_adb_shell_command (logcat, grep, pm)
- Mobile-MCP: mcp_mobile-mcp_mobile_launch_app, mcp_mobile-mcp_mobile_take_screenshot
- Gradle MCP: mcp_gradle-mcp-server_execute_gradle_task

## Debugging Workflow

1. Crash Detection (CLI)

- adb logcat -d | grep -E '(FATAL|AndroidRuntime|Exception)' | tail -20
- For stack trace context: adb logcat -d | grep -A 30 -B 5 'FATAL EXCEPTION' | tail -40

1. Investigation

- Claude Context search for related patterns
- Component-specific logs: adb logcat -d | grep -E '(UserDetail|MainActivity)' | tail -15

1. Fix Application

- Apply code fixes; search for similar patterns with Claude Context
- Run diagnostics: mcp-router open_file_in_editor → get_file_problems

1. Build Verification (CLI)

- ./gradlew :app:clean
- ./gradlew :app:assembleDebug

1. Installation and Testing (CLI)

- adb install -r app-debug.apk
- am start -n com.example.githubusers.debug/.MainActivity
- Optional screenshot via adb or fallback to Mobile-MCP

1. Verify Success

- adb logcat -d | grep -E '(Success|UserDetail)' | tail -10

## Manual vs MCP

- Prefer CLI. Use MCP when CLI is unavailable or remote automation is desired.

## Quality Checklist

- Crash location identified
- Patterns researched (Claude Context)
- Diagnostics gate clean or triaged
- Build passes and install verified
