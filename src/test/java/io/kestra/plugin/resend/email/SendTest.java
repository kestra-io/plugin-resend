package io.kestra.plugin.resend.email;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.ArgumentCaptor;

import com.google.common.collect.ImmutableMap;
import com.resend.Resend;
import com.resend.core.net.RequestOptions;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@KestraTest
public class SendTest {

    private static final String FROM = "demo@example.com";
    private static final String TO = "your-email@example.com";
    private static final String SUBJECT = "Resend plugin test";

    @Inject
    private RunContextFactory runContextFactory;

    private RunContext getRunContext() {
        return runContextFactory.of(
            Map.of(
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
            )
        );
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "RESEND_API_KEY", matches = ".+")
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

    @Test
    void sendForwardsIdempotencyKeyToResend() throws Exception {
        RunContext runContext = getRunContext();

        String idempotencyKey = "flow-" + UUID.randomUUID();
        CreateEmailResponse fakeResponse = new CreateEmailResponse(UUID.randomUUID().toString());

        Emails mockEmails = mock(Emails.class);
        when(mockEmails.send(any(CreateEmailOptions.class))).thenReturn(fakeResponse);
        when(mockEmails.send(any(CreateEmailOptions.class), any(RequestOptions.class))).thenReturn(fakeResponse);

        Send task = Send.builder()
            .apiKey(Property.ofValue("test-api-key"))
            .from(Property.ofValue(FROM))
            .to(Property.ofValue(List.of(TO)))
            .subject(Property.ofValue(SUBJECT))
            .text(Property.ofValue("Hello from Kestra Resend Plugin!"))
            .idempotencyKey(Property.ofValue(idempotencyKey))
            .build();

        try (var mockedResend = mockConstruction(Resend.class, (mock, context) -> when(mock.emails()).thenReturn(mockEmails))) {
            Send.Output output = task.run(runContext);
            assertThat(output.getId(), is(fakeResponse.getId()));
        }

        ArgumentCaptor<RequestOptions> requestOptionsCaptor = ArgumentCaptor.forClass(RequestOptions.class);
        verify(mockEmails).send(any(CreateEmailOptions.class), requestOptionsCaptor.capture());

        assertThat(requestOptionsCaptor.getValue().getIdempotencyKey(), is(idempotencyKey));
    }
}
