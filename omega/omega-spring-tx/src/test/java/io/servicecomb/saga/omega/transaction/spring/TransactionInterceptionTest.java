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

package io.servicecomb.saga.omega.transaction.spring;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import io.servicecomb.saga.omega.context.OmegaContext;
import io.servicecomb.saga.omega.transaction.MessageHandler;
import io.servicecomb.saga.omega.transaction.MessageSender;
import io.servicecomb.saga.omega.transaction.TxEvent;
import io.servicecomb.saga.omega.transaction.spring.TransactionInterceptionTest.MessageConfig;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TransactionTestMain.class, MessageConfig.class})
@AutoConfigureMockMvc
public class TransactionInterceptionTest {
  private static final String TX_STARTED_EVENT = "TxStartedEvent";
  private static final String TX_ENDED_EVENT = "TxEndedEvent";
  private static final String globalTxId = UUID.randomUUID().toString();
  private final String localTxId = UUID.randomUUID().toString();
  private final String parentTxId = UUID.randomUUID().toString();
  private final String username = uniquify("username");
  private final String email = uniquify("email");

  @Autowired
  private List<byte[]> messages;

  @Autowired
  private TransactionalUserService userService;

  @Autowired
  private OmegaContext omegaContext;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private MessageHandler messageHandler;

  @Before
  public void setUp() throws Exception {
    omegaContext.setGlobalTxId(globalTxId);
    omegaContext.setLocalTxId(localTxId);
    omegaContext.setParentTxId(parentTxId);
  }

  @After
  public void tearDown() throws Exception {
    messages.clear();
  }

  @Test
  public void sendsUserToRemote_AroundTransaction() throws Exception {
    User user = userService.add(new User(username, email));

    assertEquals(
        asList(
            txStartedEvent(globalTxId, localTxId, parentTxId, username, email),
            txEndedEvent(globalTxId, localTxId, parentTxId)),
        toString(messages)
    );

    User actual = userRepository.findOne(user.id());
    assertThat(actual, is(user));
  }

  @Test
  public void compensateOnTransactionException() throws Exception {
    User user = userService.add(new User(username, email));

    messageHandler.onReceive("to be compensated".getBytes());

    User actual = userRepository.findOne(user.id());
    assertThat(actual, is(nullValue()));

    assertThat(omegaContext.containsContext(globalTxId), is(false));
  }

  private List<String> toString(List<byte[]> messages) {
    return messages.stream()
        .map(String::new)
        .collect(Collectors.toList());
  }

  @Configuration
  static class MessageConfig {
    private final List<byte[]> messages = new ArrayList<>();

    @Bean
    List<byte[]> messages() {
      return messages;
    }

    @Bean
    MessageSender sender() {
      return (event) -> messages.add(serialize(event));
    }

    private byte[] serialize(TxEvent event) {
      if (TX_STARTED_EVENT.equals(event.type())) {
        User user = ((User) event.payloads()[0]);
        return txStartedEvent(event.globalTxId(),
            event.localTxId(),
            event.parentTxId(),
            user.username(),
            user.email()).getBytes();
      }
      return txEndedEvent(event.globalTxId(),
          event.localTxId(),
          event.parentTxId()).getBytes();
    }

    @Bean
    MessageHandler handler(OmegaContext omegaContext) {
      return bytes -> omegaContext.compensate(globalTxId);
    }
  }

  private static String txStartedEvent(String globalTxId,
      String localTxId,
      String parentTxId,
      String username,
      String email) {
    return globalTxId + ":" + localTxId + ":" + parentTxId + ":" + TX_STARTED_EVENT + ":" + username + ":" + email;
  }

  private static String txEndedEvent(String globalTxId, String localTxId, String parentTxId) {
    return globalTxId + ":" + localTxId + ":" + parentTxId + ":" + TX_ENDED_EVENT;
  }
}
