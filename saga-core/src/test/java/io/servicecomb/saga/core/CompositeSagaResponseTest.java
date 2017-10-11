package io.servicecomb.saga.core;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

import org.junit.Test;
import org.mockito.Mockito;

public class CompositeSagaResponseTest {

  private final SagaResponse response1 = Mockito.mock(SagaResponse.class);
  private final SagaResponse response2 = Mockito.mock(SagaResponse.class);

  private final SagaResponse compositeSagaResponse = new CompositeSagaResponse(asList(response1, response2));

  @Test
  public void succeededOnlyWhenAllAreSuccessful() throws Exception {
    when(response1.succeeded()).thenReturn(true);
    when(response2.succeeded()).thenReturn(true);

    assertThat(compositeSagaResponse.succeeded(), is(true));
  }

  @Test
  public void failedWhenAnyIsNotSuccessful() throws Exception {
    when(response1.succeeded()).thenReturn(true);
    when(response2.succeeded()).thenReturn(false);

    assertThat(compositeSagaResponse.succeeded(), is(false));
  }

  @Test
  public void bodyCombinesAllResponseBodies() throws Exception {
    when(response1.body()).thenReturn("{\n"
        + "  \"status\": 500,\n"
        + "  \"body\" : \"oops\"\n"
        + "}\n");

    when(response2.body()).thenReturn("{\n"
        + "  \"status\": 200,\n"
        + "  \"body\" : \"blah\"\n"
        + "}\n");

    assertThat(compositeSagaResponse.body(), sameJSONAs("[\n"
        + "  {\n"
        + "    \"status\": 500,\n"
        + "    \"body\": \"oops\"\n"
        + "  },\n"
        + "  {\n"
        + "    \"status\": 200,\n"
        + "    \"body\": \"blah\"\n"
        + "  }\n"
        + "]"));
  }
}