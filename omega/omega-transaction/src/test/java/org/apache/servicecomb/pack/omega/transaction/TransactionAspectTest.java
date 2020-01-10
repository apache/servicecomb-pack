/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.pack.omega.transaction;

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.servicecomb.pack.common.EventType;
import org.apache.servicecomb.pack.contract.grpc.ServerMeta;
import org.apache.servicecomb.pack.omega.context.IdGenerator;
import org.apache.servicecomb.pack.omega.context.OmegaContext;
import org.apache.servicecomb.pack.omega.context.TransactionContextProperties;
import org.apache.servicecomb.pack.omega.transaction.annotations.Compensable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class TransactionAspectTest {

  private final List<TxEvent> messages = new ArrayList<>();
  private final String globalTxId = UUID.randomUUID().toString();
  private final String localTxId = UUID.randomUUID().toString();
  private final String newLocalTxId = UUID.randomUUID().toString();

  private final String transactionGlobalTxId = UUID.randomUUID().toString();
  private final String transactionLocalTxId = UUID.randomUUID().toString();

  private final SagaMessageSender sender = new SagaMessageSender() {
    @Override
    public void onConnected() {
    }

    @Override
    public void onDisconnected() {
    }

    @Override
    public ServerMeta onGetServerMeta() {
      return null;
    }

    @Override
    public void close() {
    }

    @Override
    public String target() {
      return "UNKNOWN";
    }

    @Override
    public AlphaResponse send(TxEvent event) {
      messages.add(event);
      return new AlphaResponse(false);
    }
  };
  private final ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
  private final MethodSignature methodSignature = mock(MethodSignature.class);

  @SuppressWarnings("unchecked")
  private final IdGenerator<String> idGenerator = mock(IdGenerator.class);
  private final Compensable compensable = mock(Compensable.class);

  private final OmegaContext omegaContext = new OmegaContext(idGenerator);
  private final TransactionAspect aspect = new TransactionAspect(sender, omegaContext);

  private final TransactionContextProperties transactionContextProperties = mock(TransactionContextProperties.class);

  @Before
  public void setUp() throws Exception {
    when(idGenerator.nextId()).thenReturn(newLocalTxId);
    when(joinPoint.getSignature()).thenReturn(methodSignature);
    when(joinPoint.getTarget()).thenReturn(this);

    when(methodSignature.getMethod()).thenReturn(this.getClass().getDeclaredMethod("doNothing"));
    when(compensable.compensationMethod()).thenReturn("doNothing");
    when(compensable.forwardRetries()).thenReturn(0);

    omegaContext.setGlobalTxId(globalTxId);
    omegaContext.setLocalTxId(localTxId);

    when(transactionContextProperties.getGlobalTxId()).thenReturn(transactionGlobalTxId);
    when(transactionContextProperties.getLocalTxId()).thenReturn(transactionLocalTxId);
  }

  @Test
  public void setNewLocalTxIdCompensableWithTransactionContext() throws Throwable {
    // setup the argument class
    when(joinPoint.getArgs()).thenReturn(new Object[]{transactionContextProperties});
    aspect.advise(joinPoint, compensable);
    assertThat(messages.size(), is(2));
    TxEvent startedEvent = messages.get(0);

    assertThat(startedEvent.globalTxId(), is(transactionGlobalTxId));
    assertThat(startedEvent.localTxId(), is(newLocalTxId));
    assertThat(startedEvent.parentTxId(), is(transactionLocalTxId));
    assertThat(startedEvent.type(), is(EventType.TxStartedEvent));
    assertThat(startedEvent.forwardRetries(), is(0));
    assertThat(startedEvent.retryMethod().isEmpty(), is(true));

    TxEvent endedEvent = messages.get(1);

    assertThat(endedEvent.globalTxId(), is(transactionGlobalTxId));
    assertThat(endedEvent.localTxId(), is(newLocalTxId));
    assertThat(endedEvent.parentTxId(), is(transactionLocalTxId));
    assertThat(endedEvent.type(), is(EventType.TxEndedEvent));

    assertThat(omegaContext.globalTxId(), is(transactionGlobalTxId));
    assertThat(omegaContext.localTxId(), is(transactionLocalTxId));
  }

  @Test
  public void globalTxIsNotSet() throws Throwable {
    omegaContext.setGlobalTxId(null);
    try {
      aspect.advise(joinPoint, compensable);
      fail("Expect exception here");
    } catch (OmegaException ex) {
      assertThat(ex.getMessage(), is("Cannot find the globalTxId from OmegaContext. Please using @SagaStart to start a global transaction."));
    }
  }

  @Test
  public void newLocalTxIdInCompensable() throws Throwable {
    aspect.advise(joinPoint, compensable);
    assertThat(messages.size(), is(2));
    TxEvent startedEvent = messages.get(0);

    assertThat(startedEvent.globalTxId(), is(globalTxId));
    assertThat(startedEvent.localTxId(), is(newLocalTxId));
    assertThat(startedEvent.parentTxId(), is(localTxId));
    assertThat(startedEvent.type(), is(EventType.TxStartedEvent));
    assertThat(startedEvent.forwardRetries(), is(0));
    assertThat(startedEvent.retryMethod().isEmpty(), is(true));

    TxEvent endedEvent = messages.get(1);

    assertThat(endedEvent.globalTxId(), is(globalTxId));
    assertThat(endedEvent.localTxId(), is(newLocalTxId));
    assertThat(endedEvent.parentTxId(), is(localTxId));
    assertThat(endedEvent.type(), is(EventType.TxEndedEvent));

    assertThat(omegaContext.globalTxId(), is(globalTxId));
    assertThat(omegaContext.localTxId(), is(localTxId));
  }

  @Test
  public void restoreContextOnCompensableError() throws Throwable {
    RuntimeException oops = new RuntimeException("oops");

    when(joinPoint.proceed()).thenThrow(oops);

    try {
      aspect.advise(joinPoint, compensable);
      expectFailing(RuntimeException.class);
    } catch (RuntimeException e) {
      assertThat(e, is(oops));
    }

    TxEvent event = messages.get(1);

    assertThat(event.globalTxId(), is(globalTxId));
    assertThat(event.localTxId(), is(newLocalTxId));
    assertThat(event.parentTxId(), is(localTxId));
    assertThat(event.type(), is(EventType.TxAbortedEvent));

    assertThat(omegaContext.globalTxId(), is(globalTxId));
    assertThat(omegaContext.localTxId(), is(localTxId));
  }

  @Test
  public void interruptsOnCompensableTimeoutExceptionWithSleepBlocked() throws Throwable {
    when(compensable.forwardTimeout()).thenReturn(2);
    when(joinPoint.proceed()).thenAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        Thread.sleep(5000);
        return null;
      }
    });
    try {
      aspect.advise(joinPoint, compensable);
    } catch (RuntimeException e) {
      assertThat(e, instanceOf(TransactionTimeoutException.class));
      assertThat(e.getCause(), instanceOf(InterruptedException.class));
    }
  }

  @Test
  public void interruptsOnCompensableTimeoutExceptionWithWaitBlocked() throws Throwable {
    when(compensable.forwardTimeout()).thenReturn(2);
    when(joinPoint.proceed()).thenAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        Thread.currentThread().wait(5000);
        return null;
      }
    });
    try {
      aspect.advise(joinPoint, compensable);
    } catch (RuntimeException e) {
      assertThat(e, instanceOf(TransactionTimeoutException.class));
      assertThat(e.getCause(), instanceOf(IllegalMonitorStateException.class));
    }
  }

  @Test
  public void interruptsOnCompensableTimeoutExceptionWithIOBlocked() throws Throwable {
    when(compensable.forwardTimeout()).thenReturn(2);
    when(joinPoint.proceed()).thenAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        String name = "delete.me";
        new File(name).deleteOnExit();
        RandomAccessFile raf = new RandomAccessFile(name, "rw");
        FileChannel fc = raf.getChannel();
        try {
          ByteBuffer buffer = ByteBuffer.wrap(new String("1").getBytes());
          while (true) {
            fc.write(buffer);
          }
        } finally {
          if (fc != null) {
            fc.close();
          }
        }
      }
    });
    try {
      aspect.advise(joinPoint, compensable);
    } catch (RuntimeException e) {
      assertThat(e, instanceOf(TransactionTimeoutException.class));
      assertThat(e.getCause(), instanceOf(ClosedByInterruptException.class));
    }
  }

  @Test
  public void interruptsOnCompensableTimeoutExceptionWithCPUBusyBlocked() throws Throwable {
    when(compensable.forwardTimeout()).thenReturn(3);
    when(joinPoint.proceed()).thenAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        while (true){
          Thread.sleep(1);
        }
      }
    });
    try {
      aspect.advise(joinPoint, compensable);
    } catch (RuntimeException e) {
      assertThat(e, instanceOf(TransactionTimeoutException.class));
      assertThat(e.getCause(), instanceOf(InterruptedException.class));
    }
  }

  @Test
  public void interruptsOnCompensableTimeoutRejectionBySecurity() throws Throwable {
    final Thread main = Thread.currentThread();
    when(compensable.forwardTimeout()).thenReturn(2);
    when(joinPoint.proceed()).thenAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        System.setSecurityManager(new AccessRejectionSecurityManager(main));
        return null;
      }
    });
    try {
      aspect.advise(joinPoint, compensable);
    } catch (RuntimeException e) {
      assertThat(e, instanceOf(OmegaException.class));
    }
  }

  /**
   * Send TxAbortedEvent after three failed retries
   * TxStartedEvent retry 1
   * TxStartedEvent retry 2
   * TxStartedEvent retry 3
   * TxAbortedEvent
   * */
  @Test
  public void retryReachesMaximumAndForwardException() throws Throwable {
    RuntimeException oops = new RuntimeException("oops");
    when(joinPoint.proceed()).thenThrow(oops);
    when(compensable.forwardRetries()).thenReturn(3);

    try {
      aspect.advise(joinPoint, compensable);
      expectFailing(RuntimeException.class);
    } catch (RuntimeException e) {
      assertThat(e.getMessage(), is("oops"));
    }

    assertThat(messages.size(), is(4));

    // TxStartedEvent retry 1
    TxEvent startedEvent1 = messages.get(0);
    assertThat(startedEvent1.globalTxId(), is(globalTxId));
    assertThat(startedEvent1.localTxId(), is(newLocalTxId));
    assertThat(startedEvent1.parentTxId(), is(localTxId));
    assertThat(startedEvent1.type(), is(EventType.TxStartedEvent));
    assertThat(startedEvent1.forwardRetries(), is(3));
    assertThat(startedEvent1.retryMethod(),
        is(this.getClass().getDeclaredMethod("doNothing").toString()));

    // TxStartedEvent retry 2
    TxEvent startedEvent2 = messages.get(1);
    assertThat(startedEvent2.localTxId(), is(newLocalTxId));
    assertThat(startedEvent2.type(), is(EventType.TxStartedEvent));
    assertThat(startedEvent2.forwardRetries(), is(2));

    // TxStartedEvent retry 3
    TxEvent startedEvent3 = messages.get(2);
    assertThat(startedEvent3.localTxId(), is(newLocalTxId));
    assertThat(startedEvent3.type(), is(EventType.TxStartedEvent));
    assertThat(startedEvent3.forwardRetries(), is(1));

    // TxAbortedEvent
    assertThat(messages.get(3).type(), is(EventType.TxAbortedEvent));

    assertThat(omegaContext.globalTxId(), is(globalTxId));
    assertThat(omegaContext.localTxId(), is(localTxId));
  }

  private String doNothing() {
    return "doNothing";
  }

  static class AccessRejectionSecurityManager extends SecurityManager {
    private Thread main;
    public AccessRejectionSecurityManager(Thread main){
      this.main = main;
    }
    public void checkAccess(Thread t) {
      for(StackTraceElement stack : main.getStackTrace()){
        if(stack.getMethodName().equals("interruptsOnCompensableTimeoutRejectionBySecurity")){
          throw new SecurityException("simulation");
        }
      }
    }

    public void checkPermission(Permission perm) {
      // Has All Permission
    }
  }
}
