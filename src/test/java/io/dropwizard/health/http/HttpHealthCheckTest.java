package io.dropwizard.health.http;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import jakarta.ws.rs.ProcessingException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HttpHealthCheckTest {
    private static final String PATH = "/health-check";

    @RegisterExtension
    public WireMockExtension wireMockRule = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private HttpHealthCheck httpHealthCheck;

    @BeforeEach
    public void setUp() {
        this.httpHealthCheck = new HttpHealthCheck(wireMockRule.url(PATH));
    }

    @Test
    public void httpHealthCheckShouldConsiderA200ResponseHealthy() {
        stubFor(get(urlEqualTo(PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("HAPPY")));

        assertThat(httpHealthCheck.check().isHealthy()).isTrue();
    }

    @Test
    public void httpHealthCheckShouldConsiderA500ResponseUnhealthy() {
        stubFor(get(urlEqualTo(PATH))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("SAD")));

        assertThat(httpHealthCheck.check().isHealthy()).isFalse();
    }

    @Test
    public void httpHealthCheckShouldConsiderATimeoutUnhealthy() {
        assertThrows(ProcessingException.class, () -> {
            stubFor(get(urlEqualTo(PATH))
                    .willReturn(aResponse()
                            .withFixedDelay((int) (httpHealthCheck.DEFAULT_TIMEOUT.toMillis() * 2))
                            .withStatus(200)
                            .withBody("HAPPY")));

            assertThat(httpHealthCheck.check().isHealthy()).isFalse();
        });
    }

    @Test
    public void httpHealthCheckShouldConsiderAFaultUnhealthyButThenRecover() {
        stubFor(get(urlEqualTo(PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("HAPPY")));

        assertThat(httpHealthCheck.check().isHealthy()).isTrue();

        stubFor(get(urlEqualTo(PATH))
                .willReturn(aResponse()
                        .withFault(Fault.RANDOM_DATA_THEN_CLOSE)
                        .withStatus(200)));

        assertThat(httpHealthCheck.check().isHealthy()).isFalse();

        stubFor(get(urlEqualTo(PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("HAPPY")));

        assertThat(httpHealthCheck.check().isHealthy()).isTrue();
    }
}
