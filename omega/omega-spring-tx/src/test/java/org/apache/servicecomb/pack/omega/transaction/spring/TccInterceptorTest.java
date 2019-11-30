/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.servicecomb.pack.omega.transaction.spring;

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static org.assertj.core.util.IterableUtil.toArray;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.apache.servicecomb.pack.common.TransactionStatus;
import org.apache.servicecomb.pack.omega.context.IdGenerator;
import org.apache.servicecomb.pack.omega.transaction.TxCompensateAckFailedEvent;
import org.apache.servicecomb.pack.omega.transaction.TxCompensateAckSucceedEvent;
import org.apache.servicecomb.pack.omega.transaction.tcc.TccMessageHandler;
import org.apache.servicecomb.pack.omega.transaction.tcc.events.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TransactionTestMain.class, MessageConfig.class})
public class TccInterceptorTest {

  private static final String globalTxId = UUID.randomUUID().toString();
  private final String newLocalTxId = UUID.randomUUID().toString();
  private final String anotherLocalTxId = UUID.randomUUID().toString();
  private final String username = uniquify("username");
  private final String email = uniquify("email");

  private final User user = new User(username, email);
  private final User illegalUser = new User(TransactionalUserService.ILLEGAL_USER, email);

  private final String usernameJack = uniquify("Jack");
  private final User jack = new User(usernameJack, uniquify("jack@gmail.com"));

  @Autowired
  private IdGenerator<String> idGenerator;

  @Autowired
  private List<String> messages;

  @Autowired
  private TccUserServiceMain tccUserServiceMain;

  @Autowired
  private UserRepository userRepository;

  private String confirmMethod;

  private String cancelMethod;

  @Autowired
  private TccMessageHandler coordinateMessageHandler;

  @Before
  public void setUp() throws Exception {
    when(idGenerator.nextId()).thenReturn(globalTxId, newLocalTxId, anotherLocalTxId);
    confirmMethod = TccUserService.class.getDeclaredMethod("confirm", User.class).toString();
    cancelMethod = TccUserService.class.getDeclaredMethod("cancel", User.class).toString();
  }

  @After
  public void tearDown() throws Exception {
    messages.clear();
    userRepository.deleteAll();
  }

  @AfterClass
  public static void afterClass() throws Exception {
  }

  @Test
  public void tccWorkflowSucceed() {
    tccUserServiceMain.add(user, jack);

    coordinateMessageHandler.onReceive(globalTxId, newLocalTxId, globalTxId, confirmMethod);
    coordinateMessageHandler.onReceive(globalTxId, anotherLocalTxId, globalTxId, confirmMethod);

    assertArrayEquals(
        new String[] {
            new TccStartedEvent(globalTxId, globalTxId).toString(),
            new ParticipationStartedEvent(globalTxId, newLocalTxId, globalTxId, confirmMethod, cancelMethod).toString(),
            new ParticipationEndedEvent(globalTxId, newLocalTxId, globalTxId, confirmMethod, cancelMethod,
                TransactionStatus.Succeed).toString(),
            new ParticipationStartedEvent(globalTxId, anotherLocalTxId, globalTxId, confirmMethod, cancelMethod).toString(),
            new ParticipationEndedEvent(globalTxId, anotherLocalTxId, globalTxId, confirmMethod, cancelMethod,
                TransactionStatus.Succeed).toString(),
            new TccEndedEvent(globalTxId, globalTxId, TransactionStatus.Succeed).toString(),
            new TxCompensateAckSucceedEvent(globalTxId, newLocalTxId, globalTxId).toString(),
            new CoordinatedEvent(globalTxId, newLocalTxId, globalTxId, confirmMethod, TransactionStatus.Succeed).toString(),
            new TxCompensateAckSucceedEvent(globalTxId, anotherLocalTxId, globalTxId).toString(),
            new CoordinatedEvent(globalTxId, anotherLocalTxId, globalTxId, confirmMethod, TransactionStatus.Succeed).toString()
        },
        toArray(messages)
    );

    User result = userRepository.findByUsername(user.username());
    assertThat(result.username(), is(user.username()));
    assertThat(result.email(), is(user.email()));

    result = userRepository.findByUsername(jack.username());
    assertThat(result.username(), is(jack.username()));
    assertThat(result.email(), is(jack.email()));
  }

  @Test
  public void tccWorkflowFailed() {
    try {
      tccUserServiceMain.add(user, illegalUser);
      expectFailing(IllegalArgumentException.class);
    } catch (IllegalArgumentException ignored) {

    }

    coordinateMessageHandler.onReceive(globalTxId, newLocalTxId, globalTxId, cancelMethod);
    assertArrayEquals(
        new String[] {
            new TccStartedEvent(globalTxId, globalTxId).toString(),
            new ParticipationStartedEvent(globalTxId, newLocalTxId, globalTxId, confirmMethod, cancelMethod).toString(),
            new ParticipationEndedEvent(globalTxId, newLocalTxId, globalTxId, confirmMethod, cancelMethod,
                TransactionStatus.Succeed).toString(),
            new ParticipationStartedEvent(globalTxId, anotherLocalTxId, globalTxId, confirmMethod, cancelMethod).toString(),
            new ParticipationEndedEvent(globalTxId, anotherLocalTxId, globalTxId, confirmMethod, cancelMethod,
                TransactionStatus.Failed).toString(),
            new TccEndedEvent(globalTxId, globalTxId, TransactionStatus.Failed).toString(),
            new TxCompensateAckFailedEvent(globalTxId, newLocalTxId, globalTxId).toString(),
            new CoordinatedEvent(globalTxId, newLocalTxId, globalTxId, cancelMethod, TransactionStatus.Succeed).toString()
        },
        toArray(messages)
    );

    User result = userRepository.findByUsername(user.username());
    assertThat(result.username(), is(user.username()));
    assertThat(result.email(), is(user.email()));

    result = userRepository.findByUsername(jack.username());
    assertThat(result, is(nullValue()));
  }
}
