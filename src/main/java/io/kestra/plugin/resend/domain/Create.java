package io.kestra.plugin.resend.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.resend.Resend;
import com.resend.services.domains.model.CreateDomainOptions;
import com.resend.services.domains.model.CreateDomainResponse;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Create a Resend domain.",
    description = "Register a new sending domain in Resend."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Create a domain",
            code = """
                id: create_domain
                namespace: company.team

                tasks:
                  - id: add_domain
                    type: io.kestra.plugin.resend.domain.Create
                    apiKey: "{{ secret('RESEND_API_KEY') }}"
                    name: "example.com"
                    region: "us-east-1"
                """
        )
    }
)
public class Create extends Task implements RunnableTask<Create.Output> {
    @Schema(
        title = "Resend API key"
    )
    @NotNull private Property<String> apiKey;

    @Schema(
        title = "Domain name",
        description = "The domain you want to register in Resend (e.g., `example.com`)"
    )
    @NotNull private Property<String> name;

    @Schema(
        title = "Region",
        description = "Region where emails will be sent from. Defaults to `us-east-1`."
    )
    private Property<String> region;

    @Schema(
        title = "Custom return path",
        description = "Subdomain for the Return-Path address. Defaults to `send`. Avoid values like `test`, as they may appear to recipients."
    )
    private Property<String> customReturnPath;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Resend resend = new Resend(runContext.render(apiKey).as(String.class).orElseThrow());

        CreateDomainOptions options = CreateDomainOptions.builder()
            .name(runContext.render(name).as(String.class).orElseThrow())
            .region(runContext.render(region).as(String.class).orElse("us-east-1"))
            .customReturnPath(runContext.render(customReturnPath).as(String.class).orElse(null))
            .build();

        CreateDomainResponse response = resend.domains().create(options);

        runContext.logger().info("Created Resend Domain: {}", response);

        Map<String, Object> result = JacksonMapper.ofJson().convertValue(response, new TypeReference<>() {});

        return Output.builder()
            .id(response.getId())
            .result(result)
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Created Domain ID")
        private final String id;

        @Schema(title = "Raw response from Resend")
        private final Map<String, Object> result;
    }
}
