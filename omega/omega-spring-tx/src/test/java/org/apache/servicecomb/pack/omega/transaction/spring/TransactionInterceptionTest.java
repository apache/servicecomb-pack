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

package org.apache.servicecomb.pack.omega.transaction.spring;

import static akka.actor.ActorRef.noSender;
import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.japi.Creator;
import akka.japi.pf.FI.UnitApply;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.servicecomb.pack.omega.context.IdGenerator;
import org.apache.servicecomb.pack.omega.context.OmegaContext;
import org.apache.servicecomb.pack.omega.transaction.TxCompensateAckSucceedEvent;
import org.apache.servicecomb.pack.omega.transaction.spring.annotations.OmegaContextAware;
import org.apache.servicecomb.pack.omega.transaction.MessageHandler;
import org.apache.servicecomb.pack.omega.transaction.TxAbortedEvent;
import org.apache.servicecomb.pack.omega.transaction.TxCompensatedEvent;
import org.apache.servicecomb.pack.omega.transaction.TxEndedEvent;
import org.apache.servicecomb.pack.omega.transaction.TxStartedEvent;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {
    "omega.spec.names=saga",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.url=jdbc:h2:mem:alpha;MODE=MYSQL"},
    classes = {TransactionTestMain.class, MessageConfig.class, EclipseLinkJpaConfiguration.class})
@AutoConfigureMockMvc
public class TransactionInterceptionTest {
  @SuppressWarnings("unchecked")
  private static final String globalTxId = UUID.randomUUID().toString();
  private final String newLocalTxId = UUID.randomUUID().toString();
  private final String anotherLocalTxId = UUID.randomUUID().toString();
  private final String username = uniquify("username");
  private final String email = uniquify("email");

  private final User user = new User(username, email);
  private final User illegalUser = new User(TransactionalUserService.ILLEGAL_USER, email);

  private final String usernameJack = uniquify("Jack");
  private final User jack = new User(usernameJack, uniquify("jack@gmail.com"));

  @OmegaContextAware
  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  @Autowired
  private IdGenerator<String> idGenerator;

  @Autowired
  private List<String> messages;

  @Autowired
  private TransactionalUserService userService;

  @Autowired
  private OmegaContext omegaContext;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private MessageHandler messageHandler;

  private String compensationMethod;

  private String compensationMethod2;

  private String retryMethod;

  @Before
  public void setUp() throws Exception {
    when(idGenerator.nextId()).thenReturn(newLocalTxId, anotherLocalTxId);
    omegaContext.setGlobalTxId(globalTxId);
    omegaContext.setLocalTxId(globalTxId);
    retryMethod = TransactionalUserService.class.getDeclaredMethod("add", User.class, int.class).toString();
    compensationMethod = TransactionalUserService.class.getDeclaredMethod("delete", User.class).toString();
    compensationMethod2 = TransactionalUserService.class.getDeclaredMethod("delete", User.class, int.class).toString();
  }

  @After
  public void tearDown() throws Exception {
    messages.clear();
    userRepository.deleteAll();
    omegaContext.clear();
    userService.resetCount();
  }

  @AfterClass
  public static void afterClass() throws Exception {
  }

  @Test
  public void sendsUserToRemote_AroundTransaction() throws Exception {
    User user = userService.add(this.user);

    assertArrayEquals(
        new String[] {
            new TxStartedEvent(globalTxId, newLocalTxId, globalTxId, compensationMethod, 0, "", 0,
                0, 0, 0, 0, user).toString(),
            new TxEndedEvent(globalTxId, newLocalTxId, globalTxId, compensationMethod).toString()},
        toArray(messages)
    );

    User actual = userRepository.findByUsername(user.username());
    assertThat(actual, is(user));
  }

  @Test
  public void sendsAbortEvent_OnSubTransactionFailure() throws Exception {
    Throwable throwable = null;
    try {
      userService.add(illegalUser);
      expectFailing(IllegalArgumentException.class);
    } catch (IllegalArgumentException ignored) {
      throwable = ignored;
    }

    assertArrayEquals(
        new String[] {
            new TxStartedEvent(globalTxId, newLocalTxId, globalTxId, compensationMethod, 0, "", 0,
                0, 0, 0, 0, illegalUser).toString(),
            new TxAbortedEvent(globalTxId, newLocalTxId, globalTxId, compensationMethod, throwable).toString()},
        toArray(messages)
    );
  }

  @Test
  public void compensateOnTransactionException() throws Exception {
    User user = userService.add(this.user);

    // another sub transaction to the same service within the same global transaction
    String localTxId = omegaContext.newLocalTxId();
    User anotherUser = userService.add(jack);

    messageHandler.onReceive(globalTxId, newLocalTxId, globalTxId, compensationMethod, user);
    messageHandler.onReceive(globalTxId, anotherLocalTxId, localTxId, compensationMethod, anotherUser);

    assertThat(userRepository.findByUsername(user.username()), is(nullValue()));
    assertThat(userRepository.findByUsername(anotherUser.username()), is(nullValue()));

    assertArrayEquals(
        new String[] {
            new TxStartedEvent(globalTxId, newLocalTxId, globalTxId, compensationMethod, 0, "", 0,
                0, 0, 0, 0, user).toString(),
            new TxEndedEvent(globalTxId, newLocalTxId, globalTxId, compensationMethod).toString(),
            new TxStartedEvent(globalTxId, anotherLocalTxId, localTxId, compensationMethod, 0, "", 0,
                0, 0, 0, 0, anotherUser).toString(),
            new TxEndedEvent(globalTxId, anotherLocalTxId, localTxId, compensationMethod).toString(),
            new TxCompensatedEvent(globalTxId, newLocalTxId, globalTxId, compensationMethod).toString(),
            new TxCompensatedEvent(globalTxId, anotherLocalTxId, localTxId, compensationMethod).toString()
        },
        toArray(messages)
    );
  }

//  @Test
//  public void retryTillSuccess() {
//    try {
//      userService.add(user, 1);
//    } catch (Exception e) {
//      fail("unexpected exception throw: " + e);
//    }
//
//    assertThat(messages.size(), is(3));
//
//    assertThat(messages.get(0),
//        is(new TxStartedEvent(globalTxId, newLocalTxId, globalTxId, compensationMethod2, 0, retryMethod, 2, user, 1)
//            .toString()));
//
//    assertThat(messages.get(1),
//        is(new TxStartedEvent(globalTxId, newLocalTxId, globalTxId, compensationMethod2, 0, retryMethod, 1, user, 1)
//            .toString()));
//    assertThat(messages.get(2),
//        is(new TxEndedEvent(globalTxId, newLocalTxId, globalTxId, compensationMethod2).toString()));
//
//    assertThat(userRepository.count(), is(1L));
//    Iterable<User> users =  userRepository.findAll();
//    for(User user: users ) {
//      assertThat(user, is(this.user));
//    }
//  }

  @Test
  public void retryReachesMaximumThenThrowsException() {
    try {
      userService.add(user, 3);
      expectFailing(IllegalStateException.class);
    } catch (IllegalStateException e) {
      assertThat(e.getMessage(), is("Retry harder"));
    }

    assertThat(messages.size(), is(3));
    assertThat(messages.get(0),
        is(new TxStartedEvent(globalTxId, newLocalTxId, globalTxId, compensationMethod2, 0, retryMethod, 2, 0, 0, 0, 0, user, 3)
            .toString()));

    assertThat(messages.get(1),
        is(new TxStartedEvent(globalTxId, newLocalTxId, globalTxId, compensationMethod2, 0, retryMethod, 1, 0, 0, 0, 0, user, 3)
            .toString()));

    String abortedEvent2 = messages.get(2);
    assertThat(abortedEvent2, allOf(containsString("TxAbortedEvent"), containsString("Retry harder")));

    assertThat(userRepository.count(), is(0L));
  }

  @Test
  public void passesOmegaContextThroughDifferentThreads() throws Exception {
    new Thread(new Runnable() {
      @Override
      public void run() {
        userService.add(user);
      }
    }).start();
    waitTillSavedUser(username);

    String localTxId = omegaContext.newLocalTxId();
    new Thread(new Runnable() {
      @Override
      public void run() {
        userService.add(jack);
      }
    }).start();
    waitTillSavedUser(usernameJack);

    assertArrayEquals(
        new String[] {
            new TxStartedEvent(globalTxId, newLocalTxId, globalTxId, compensationMethod, 0, "", 0, 0, 0, 0, 0, user).toString(),
            new TxEndedEvent(globalTxId, newLocalTxId, globalTxId, compensationMethod).toString(),
            new TxStartedEvent(globalTxId, anotherLocalTxId, localTxId, compensationMethod, 0, "", 0, 0, 0, 0, 0, jack).toString(),
            new TxEndedEvent(globalTxId, anotherLocalTxId, localTxId, compensationMethod).toString()},
        toArray(messages)
    );
  }

  @Test
  public void assertCompensationMethodTransactionAware() throws NoSuchMethodException {
    userService.add(user);
    compensationMethod = TransactionalUserService.class.getDeclaredMethod("deleteWithTransactional", User.class).toString();
    messageHandler.onReceive(globalTxId, newLocalTxId, globalTxId, compensationMethod, user);
    User actualUser = userRepository.findByUsername(user.username());
    assertNotNull(actualUser);
    assertThat(actualUser.username(), is(username));
    assertThat(actualUser.email(), is(email));
  }

  @Test
  public void passesOmegaContextInThreadPool() throws Exception {
    executor.schedule(new Runnable() {
      @Override
      public void run() {
        userService.add(user);
      }
    }, 0, MILLISECONDS);
    waitTillSavedUser(username);

    String localTxId = omegaContext.newLocalTxId();
    executor.invokeAll(singletonList(new Callable<User>() {
      @Override
      public User call() throws Exception {
        return userService.add(jack);
      }
    }));
    waitTillSavedUser(usernameJack);

    assertArrayEquals(
        new String[] {
            new TxStartedEvent(globalTxId, newLocalTxId, globalTxId, compensationMethod, 0, "", 0, 0, 0, 0, 0, user).toString(),
            new TxEndedEvent(globalTxId, newLocalTxId, globalTxId, compensationMethod).toString(),
            new TxStartedEvent(globalTxId, anotherLocalTxId, localTxId, compensationMethod, 0, "", 0, 0, 0, 0, 0, jack).toString(),
            new TxEndedEvent(globalTxId, anotherLocalTxId, localTxId, compensationMethod).toString()},
        toArray(messages)
    );
  }

  // TODO: 2018/1/4 reactive is not supported yet, omega context won't be updated on shared threads
  @Test
  public void passesOmegaContextThroughReactiveX() throws Exception {
    Flowable.just(user)
        .parallel()
        .runOn(Schedulers.io())
        .doOnNext(new Consumer() {
          @Override
          public void accept(Object user) throws Exception {
            userService.add((User)user);
          }
        })
        .sequential()
        .subscribe();

    waitTillSavedUser(username);

    assertArrayEquals(
        new String[] {
            new TxStartedEvent(globalTxId, newLocalTxId, globalTxId, compensationMethod, 0, "", 0, 0, 0, 0, 0, user).toString(),
            new TxEndedEvent(globalTxId, newLocalTxId, globalTxId, compensationMethod).toString()},
        toArray(messages)
    );
  }

  // TODO: 2018/1/4 actor system is not supported yet
  @Test
  public void passesOmegaContextAmongActors() throws Exception {
    ActorSystem actorSystem = ActorSystem.create();

    ActorRef actorRef = actorSystem.actorOf(UserServiceActor.props(userService));
    actorRef.tell(user, noSender());

    waitTillSavedUser(username);

    assertArrayEquals(
        new String[] {
            new TxStartedEvent(globalTxId, newLocalTxId, globalTxId, compensationMethod, 0, "", 0, 0, 0, 0, 0, user).toString(),
            new TxEndedEvent(globalTxId, newLocalTxId, globalTxId, compensationMethod).toString()},
        toArray(messages)
    );
    actorSystem.terminate();
  }

  private void waitTillSavedUser(final String username) {
    await().atMost(1000, MILLISECONDS).until(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return userRepository.findByUsername(username) != null;
      }
    });
  }

  private static class UserServiceActor extends AbstractLoggingActor {
    private final TransactionalUserService userService;

    private UserServiceActor(TransactionalUserService userService) {
      this.userService = userService;
    }

    static Props props(final TransactionalUserService userService) {
      return Props.create(UserServiceActor.class, new Creator<UserServiceActor>() {
        @Override
        public UserServiceActor create() throws Exception {
          return new UserServiceActor(userService);
        }
      });
    }

    @Override
    public Receive createReceive() {
      return receiveBuilder()
          .match(User.class, new UnitApply<User>() {
            @Override
            public void apply(User user) throws Exception {
              userService.add(user);
            }
          }).build();
    }
  }

  private String[] toArray(List<String> messages) {
    return messages.toArray(new String[messages.size()]);
  }
}
