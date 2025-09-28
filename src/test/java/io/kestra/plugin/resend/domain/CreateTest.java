package io.kestra.plugin.resend.domain;

import com.google.common.collect.ImmutableMap;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@EnabledIfEnvironmentVariable(named = "RESEND_API_KEY", matches = ".+")
@KestraTest
public class CreateTest {

    @Inject
    private RunContextFactory runContextFactory;

    private RunContext getRunContext() {
        return runContextFactory.of(Map.of(
            "firstFailed", false,
            "execution", ImmutableMap.of(
                "id", "#aBcDeFgH",
                "flowId", "resend-domain",
                "namespace", "org.test",
                "state", ImmutableMap.of("current", "SUCCESS")
            ),
            "duration", Duration.ofMillis(123456),
            "flow", ImmutableMap.of("id", "resend-domain"),
            "link", "http://todo.com",
            "customFields", ImmutableMap.of("Env", "dev"),
            "customMessage", "myCustomMessage"
        ));
    }

    @Test
    void createDomain() throws Exception {
        RunContext runContext = getRunContext();

        String apiKey = System.getenv("RESEND_API_KEY");

        Create task = Create.builder()
            .apiKey(Property.ofValue(apiKey))
            .name(Property.ofValue("your-test-domain.com"))
            .region(Property.ofValue("us-east-1"))
            .build();

        Create.Output output = task.run(runContext);

        assertThat(output.getResult(), notNullValue());
        assertThat(output.getResult().get("name"), is("your-test-domain.com"));
        assertThat(output.getResult().get("status"), anyOf(is("not_started"), is("pending")));
        assertThat(output.getResult(), hasKey("records"));
    }
}
