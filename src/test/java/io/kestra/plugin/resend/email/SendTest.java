package io.kestra.plugin.resend.email;

import com.google.common.collect.ImmutableMap;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@EnabledIfEnvironmentVariable(named = "RESEND_API_KEY", matches = ".+")
@KestraTest
public class SendTest {

    private static final String FROM = "demo@example.com";
    private static final String TO = "your-email@example.com";
    private static final String SUBJECT = "Resend plugin test";

    @Inject
    private RunContextFactory runContextFactory;

    private RunContext getRunContext() {
        return runContextFactory.of(Map.of(
            "firstFailed", false,
            "execution", ImmutableMap.of(
                "id", "#EmailTest",
                "flowId", "resend-email",
                "namespace", "org.test",
                "state", ImmutableMap.of("current", "SUCCESS")
            ),
            "duration", Duration.ofMillis(123456),
            "flow", ImmutableMap.of("id", "resend-email"),
            "link", "http://todo.com",
            "customFields", ImmutableMap.of("Env", "dev"),
            "customMessage", "resend-test"
        ));
    }

    @Test
    void Send() throws Exception {
        RunContext runContext = getRunContext();

        String apiKey = System.getenv("RESEND_API_KEY");

        Send task = Send.builder()
            .apiKey(Property.ofValue(apiKey))
            .from(Property.ofValue(FROM))
            .to(Property.ofValue(List.of(TO)))
            .subject(Property.ofValue(SUBJECT))
            .html(Property.ofValue("<h1>Hello from Kestra Resend Plugin!</h1>"))
            .text(Property.ofValue("Hello from Kestra Resend Plugin!"))
            .build();

        Send.Output output = task.run(runContext);

        assertThat(output.getId(), notNullValue());
        assertThat(output.getId(), matchesPattern("^[0-9a-f\\-]{36}$"));
    }
}
