package io.servicecomb.saga.format;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static io.servicecomb.saga.core.Operation.SUCCESSFUL_SAGA_RESPONSE;
import static io.servicecomb.saga.core.SagaResponse.EMPTY_RESPONSE;
import static java.util.Collections.emptyMap;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.servicecomb.saga.core.SagaResponse;
import io.servicecomb.saga.transports.RestTransport;

@SuppressWarnings("unchecked")
public class JacksonRestOperationTest {

  private final String address = uniquify("address");
  private final String path = uniquify("path");
  private final String method = "PUT";
  private final Map<String, Map<String, String>> params = new HashMap<>();

  private final RestTransport transport = Mockito.mock(RestTransport.class);
  private final JacksonRestOperation restOperation = new JacksonRestOperation(path, method, params);

  @Before
  public void setUp() throws Exception {
    restOperation.with(() -> transport);
  }

  @Test
  public void appendsResponseToForm() throws Exception {
    ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class);
    when(transport.with(eq(address), eq(path), eq(method), argumentCaptor.capture())).thenReturn(EMPTY_RESPONSE);

    SagaResponse response = restOperation.send(address, SUCCESSFUL_SAGA_RESPONSE);

    assertThat(response, is(EMPTY_RESPONSE));

    Map<String, Map<String, String>> updatedParams = argumentCaptor.getValue();
    assertThat(updatedParams.getOrDefault("form", emptyMap()).get("response"), is(SUCCESSFUL_SAGA_RESPONSE.body()));
    assertThat(params.isEmpty(), is(true));
  }
}