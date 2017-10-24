package io.servicecomb.saga.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class SagaEndTaskTest {
  private final SagaRequest request = mock(SagaRequest.class);
  private final SagaLog sagaLog = mock(SagaLog.class);

  private final String sagaId = "0";
  private final SagaEndTask sagaEndTask = new SagaEndTask(sagaId, sagaLog);

  @Test
  public void emptyResponseOnSuccessfulEventPersistence() throws Exception {
    ArgumentCaptor<SagaEndedEvent> argumentCaptor = ArgumentCaptor.forClass(SagaEndedEvent.class);
    doNothing().when(sagaLog).offer(argumentCaptor.capture());

    sagaEndTask.commit(request, SagaResponse.EMPTY_RESPONSE);

    SagaEndedEvent event = argumentCaptor.getValue();
    assertThat(event.sagaId, is(sagaId));
    assertThat(event.json(null), is("{}"));
    assertThat(event.payload(), is(request));
  }
}