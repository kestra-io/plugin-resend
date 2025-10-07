package io.kestra.plugin.resend.email;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import com.resend.services.emails.model.Tag;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.io.InputStream;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.kestra.core.utils.Rethrow.throwFunction;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Send an email with Resend.",
    description = "Send an email using the Resend API. Supports To, CC, BCC, Reply-To, scheduled sending, attachments, headers, and tags."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Send a simple email",
            code = """
                id: send_email
                namespace: company.team

                tasks:
                  - id: notify
                    type: io.kestra.plugin.resend.email.Send
                    from: "your-email@example.com"
                    to:
                      - "demo@example.com"
                    subject: "Welcome!"
                    html: "<h1>Hello</h1>"
                    apiKey: "{{ secret('RESEND_API_KEY') }}"
                """
        ),
        @Example(
            full = true,
            title = "Send an email with attachment.",
            code = """
                id: send_email_with_attachment
                namespace: company.team

                inputs:
                  - id: user_file
                    type: FILE

                tasks:
                  - id: send_email
                    type: io.kestra.plugin.resend.email.Send
                    from: "user@example.com"
                    to:
                      - "recipient@example.com"
                    subject: "Welcome with attachment!"
                    html: "<h1>Hello</h1><p>Here’s your file.</p>"
                    attachments:
                      - name: "test.pdf"
                        uri: "{{ inputs.user_file }}"
                        contentType: "application/pdf"
                    apiKey: "{{ secret('RESEND_API_KEY') }}"
                """
        )
    }
)
public class Send extends Task implements RunnableTask<Send.Output> {
    @Schema(
        title = "Resend API key"
    )
    @NotNull
    private Property<String> apiKey;

    @Schema(
        title = "From",
        description = "Sender email address – must be a verified domain in Resend."
    )
    @NotNull
    private Property<String> from;

    @Schema(
        title = "To",
        description = "Recipient(s) – a single string or a list of addresses."
    )
    @NotNull
    private Property<List<String>> to;

    @Schema(
        title = "Subject",
        description = "The subject line of the email"
    )
    @NotNull
    private Property<String> subject;

    @Schema(
        title = "CC",
        description = "Optional CC recipients"
    )
    private Property<List<String>> cc;

    @Schema(
        title = "BCC",
        description = "Optional BCC recipients"
    )
    private Property<List<String>> bcc;

    @Schema(
        title = "Reply-To",
        description = "Optional Reply-To addresses"
    )
    private Property<List<String>> replyTo;

    @Schema(
        title = "HTML Body",
        description = "HTML content of the email"
    )
    private Property<String> html;

    @Schema(
        title = "Text Body",
        description = "Plain text content of the email"
    )
    private Property<String> text;

    @Schema(
        title = "Headers",
        description = "Custom headers as key/value pairs"
    )
    private Property<Map<String, String>> headers;

    @Schema(
        title = "Idempotency Key",
        description = "Optional unique key to avoid duplicate sends"
    )
    private Property<String> idempotencyKey;

    @Schema(
        title = "Scheduled At",
        description = "The schedule of the email in ISO 8601 format (e.g., 2024-08-05T11:52:01.858Z)"
    )
    private Property<String> scheduledAt;

    @Schema(
        title = "Attachments",
        description = "File attachments (filename, content, contentType, contentId)"
    )
    private Property<List<Attachment>> attachments;

    @Schema(
        title = "Tags",
        description = "Custom tags (name/value pairs) for tracking"
    )
    private Property<List<Tag>> tags;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Resend resend = new Resend(runContext.render(apiKey).as(String.class).orElseThrow());

        CreateEmailOptions.Builder params = CreateEmailOptions.builder()
            .from(runContext.render(from).as(String.class).orElseThrow())
            .to(runContext.render(to).asList(String.class))
            .subject(runContext.render(subject).as(String.class).orElseThrow())
            .cc(runContext.render(cc).asList(String.class))
            .bcc(runContext.render(bcc).asList(String.class))
            .replyTo(runContext.render(replyTo).asList(String.class))
            .html(runContext.render(html).as(String.class).orElse(null))
            .text(runContext.render(text).as(String.class).orElse(null))
            .headers(runContext.render(headers).asMap(String.class, String.class))
            .scheduledAt(runContext.render(scheduledAt).as(String.class).orElse(null))
            .tags(runContext.render(tags).asList(Tag.class));

        if (attachments != null) {
            params.attachments(attachmentResources(runContext.render(attachments).asList(Attachment.class), runContext));
        }

        CreateEmailResponse response = resend.emails().send(params.build());

        return Output.builder()
            .id(response.getId())
            .build();
    }

    private List<com.resend.services.emails.model.Attachment> attachmentResources(List<Attachment> list, RunContext runContext) throws Exception {
        return list.stream()
            .map(throwFunction(attachment -> {
                String rName = runContext.render(attachment.getName()).as(String.class).orElseThrow();
                String rContentId = runContext.render(attachment.getContentId()).as(String.class).orElse(null);
                String rContentType = runContext.render(attachment.getContentType()).as(String.class).orElse(null);

                String rPath = runContext.render(attachment.getPath()).as(String.class).orElse(null);
                if (rPath != null) {
                    return com.resend.services.emails.model.Attachment.builder()
                        .fileName(rName)
                        .contentId(rContentId)
                        .path(rPath)
                        .contentType(rContentType)
                        .build();
                }

                String rUri = runContext.render(attachment.getUri()).as(String.class).orElse(null);
                if (rUri != null) {
                    try (InputStream inputStream = runContext.storage().getFile(URI.create(rUri))) {
                        byte[] bytes = inputStream.readAllBytes();
                        String base64 = Base64.getEncoder().encodeToString(bytes);

                        return com.resend.services.emails.model.Attachment.builder()
                            .fileName(rName)
                            .contentId(rContentId)
                            .content(base64)
                            .contentType(rContentType)
                            .build();
                    }
                }

                throw new IllegalArgumentException("Attachment must define either 'uri' (Kestra file) or 'path' (remote file).");
            }))
            .collect(Collectors.toList());
    }

    @Getter
    @Builder
    @Jacksonized
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Attachment {
        @Schema(
            title = "An attachment URI from Kestra internal storage"
        )
        private Property<String> uri;

        @Schema(
            title = "A remote file URL (http/https) to attach",
            description = "Use this for files available on the internet."
        )
        private Property<String> path;

        @Schema(
            title = "The name of the attachment (e.g., 'filename.txt')"
        )
        @NotNull
        private Property<String> name;

        @Schema
        private Property<String> contentId;

        @Schema
        private Property<String> contentType;
    }


    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Resend email ID")
        private final String id;
    }
}
