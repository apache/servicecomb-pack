package io.servicecomb.saga.core.application;


import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static java.lang.Thread.sleep;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import io.servicecomb.saga.core.DummyEvent;
import io.servicecomb.saga.core.PersistentStore;
import io.servicecomb.saga.core.SagaEvent;
import io.servicecomb.saga.core.SagaRequest;
import io.servicecomb.saga.core.application.SagaExecutionComponent.RetrySagaLog;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.Test;

public class RetrySagaLogTest {

  private final PersistentStore retryPersistentStore = mock(PersistentStore.class);
  private final SagaRequest sagaRequest = mock(SagaRequest.class);
  private final SagaEvent dummyEvent = new DummyEvent(sagaRequest);
  private final RetrySagaLog retrySagaLog = new RetrySagaLog(retryPersistentStore, 1000);
  private List<String> list = new ArrayList<>();

  @Test
  public void retryUntilSuccessWhenEventIsNotPersisted() throws InterruptedException {
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    executor.execute(() -> {
      doThrow(RuntimeException.class).when(retryPersistentStore).offer(dummyEvent);

      retrySagaLog.offer(dummyEvent);
      list.add("expect persistentStore retried all the time,but not");
    });

    sleep(5000);

    if (list.size() != 0) {
      expectFailing(RuntimeException.class);
    }
    executor.shutdown();
  }
}