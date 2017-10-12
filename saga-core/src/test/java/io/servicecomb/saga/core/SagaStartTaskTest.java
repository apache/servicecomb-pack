package io.servicecomb.saga.core;

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static io.servicecomb.saga.core.SagaResponse.EMPTY_RESPONSE;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class SagaStartTaskTest {
  private final SagaRequest request = mock(SagaRequest.class);
  private final SagaLog sagaLog = mock(SagaLog.class);

  private final String sagaId = "0";
  private final String requestJson = null;
  private final SagaStartTask sagaStartTask = new SagaStartTask(sagaId, requestJson, sagaLog);

  @Test
  public void emptyResponseOnSuccessfulEventPersistence() throws Exception {
    ArgumentCaptor<SagaStartedEvent> argumentCaptor = ArgumentCaptor.forClass(SagaStartedEvent.class);
    doNothing().when(sagaLog).offer(argumentCaptor.capture());

    SagaResponse response = sagaStartTask.commit(request);

    assertThat(response, is(EMPTY_RESPONSE));

    SagaStartedEvent event = argumentCaptor.getValue();
    assertThat(event.sagaId, is(sagaId));
    assertThat(event.json(null), is(requestJson));
    assertThat(event.payload(), is(request));
  }

  @Test
  public void blowsUpWhenEventIsNotPersisted() {
    doThrow(RuntimeException.class).when(sagaLog).offer(any(SagaStartedEvent.class));

    try {
      sagaStartTask.commit(request);
      expectFailing(SagaStartFailedException.class);
    } catch (SagaStartFailedException e) {
      assertThat(e.getMessage(), is("Failed to persist SagaStartedEvent"));
    }
  }

  @Test
  public void emptyResponseOnCompensation() throws Exception {
    SagaResponse response = sagaStartTask.compensate(request);

    assertThat(response, is(EMPTY_RESPONSE));
  }
}