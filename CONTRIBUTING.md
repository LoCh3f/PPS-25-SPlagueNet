# Contributing

## Commit messages

This project uses Conventional Commits.

Format:

```text
type(scope): description
```

Examples:

- `feat(auth): add login validation`
- `fix(ui): correct button alignment`
- `docs(readme): update setup instructions`

Allowed types:

- `feat`
- `fix`
- `docs`
- `refactor`
- `test`
- `build`
- `ci`
- `chore`
- `perf`

Use the imperative mood, keep the subject short, and do not end it with a period.

Breaking changes must use `!` after the type or scope, for example:

```text
feat(api)!: replace the response format
```