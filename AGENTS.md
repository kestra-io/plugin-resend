# Kestra Resend Plugin

## What

- Provides plugin components under `io.kestra.plugin.resend`.
- Includes classes such as `Create`, `Send`.

## Why

- What user problem does this solve? Teams need to manage domains and send email from orchestrated workflows instead of relying on manual console work, ad hoc scripts, or disconnected schedulers.
- Why would a team adopt this plugin in a workflow? It keeps Resend steps in the same Kestra flow as upstream preparation, approvals, retries, notifications, and downstream systems.
- What operational/business outcome does it enable? It reduces manual handoffs and fragmented tooling while improving reliability, traceability, and delivery speed for processes that depend on Resend.

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
