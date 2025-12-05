# File Watcher Test File

This is a new test file created at 2025-11-20-23:37 to test file watcher detection.

## Content
- Testing real-time file detection
- Verifying index updates for new files
- Monitoring file watcher performance

## Code Example

```kotlin
fun testFileWatcher() {
    println("File watcher test file created at ${Instant.now()}")
    data class TestFile(
        val name: String,
        val timestamp: Instant,
        val content: String
    )
}
```

This file should be detected by the file watcher and indexed automatically.