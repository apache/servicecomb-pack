package io.servicecomb.saga.core.application;


import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.servicecomb.saga.core.DummyEvent;
import io.servicecomb.saga.core.PersistentStore;
import io.servicecomb.saga.core.SagaEvent;
import io.servicecomb.saga.core.SagaRequest;
import io.servicecomb.saga.core.application.SagaExecutionComponent.RetrySagaLog;

public class RetrySagaLogTest {

  private final PersistentStore persistentStore = mock(PersistentStore.class);
  private final SagaRequest sagaRequest = mock(SagaRequest.class);
  private final SagaEvent dummyEvent = new DummyEvent(sagaRequest);
  private final RetrySagaLog retrySagaLog = new RetrySagaLog(persistentStore, 100);

  private volatile boolean interrupted = false;

  @Test
  public void retryUntilSuccessWhenEventIsNotPersisted() throws InterruptedException {
    doThrow(RuntimeException.class).
        doThrow(RuntimeException.class).
        doThrow(RuntimeException.class).
        doThrow(RuntimeException.class).
        doThrow(RuntimeException.class).
        doNothing().
        when(persistentStore).offer(dummyEvent);

    retrySagaLog.offer(dummyEvent);

    verify(persistentStore, times(6)).offer(dummyEvent);
  }

  @Test
  public void exitOnInterruption() throws InterruptedException {
    ExecutorService executor = Executors.newSingleThreadExecutor();

    Future<?> future = executor.submit(() -> {
      doThrow(RuntimeException.class).when(persistentStore).offer(dummyEvent);

      retrySagaLog.offer(dummyEvent);
      interrupted = true;
    });

    Thread.sleep(500);

    assertThat(future.cancel(true), is(true));

    await().atMost(2, TimeUnit.SECONDS).until(() -> interrupted);
    executor.shutdown();
  }
}