package io.servicecomb.saga.core;

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.Test;

public class SagaStartTaskTest {
  private final SagaRequest request = mock(SagaRequest.class);
  private final SagaLog sagaLog = mock(SagaLog.class);

  private final SagaStartTask sagaStartTask = new SagaStartTask("0", null, sagaLog);

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

}