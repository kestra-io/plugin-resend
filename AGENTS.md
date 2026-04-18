# Kestra Resend Plugin

## What

- Provides plugin components under `io.kestra.plugin.resend`.
- Includes classes such as `Create`, `Send`.

## Why

- This plugin integrates Kestra with Resend Domain.
- It provides tasks that manage and verify domains with the Resend API.

## How

### Architecture

Single-module plugin. Source packages under `io.kestra.plugin`:

- `resend`

Infrastructure dependencies (Docker Compose services):

- `app`

### Key Plugin Classes

- `io.kestra.plugin.resend.domain.Create`
- `io.kestra.plugin.resend.email.Send`

### Project Structure

```
plugin-resend/
├── src/main/java/io/kestra/plugin/resend/email/
├── src/test/java/io/kestra/plugin/resend/email/
├── build.gradle
└── README.md
```

## References

- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
