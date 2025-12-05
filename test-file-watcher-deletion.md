# File Watcher Deletion Test

This file will remain while the other test file gets deleted to test deletion detection.

## Content for Search Testing

```typescript
function deletionTest() {
  return "This file should remain searchable";
}
```

This content should still be available for searching after the other file is deleted.