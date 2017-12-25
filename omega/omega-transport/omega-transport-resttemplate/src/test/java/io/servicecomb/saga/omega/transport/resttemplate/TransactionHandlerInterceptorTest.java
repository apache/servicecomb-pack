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
 *
 */

package io.servicecomb.saga.omega.transport.resttemplate;

import static io.servicecomb.saga.omega.transport.resttemplate.TransactionClientHttpRequestInterceptor.GLOBAL_TX_ID_KEY;
import static io.servicecomb.saga.omega.transport.resttemplate.TransactionClientHttpRequestInterceptor.LOCAL_TX_ID_KEY;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.servlet.HandlerInterceptor;

import io.servicecomb.saga.omega.transaction.MessageSender;
import io.servicecomb.saga.omega.transaction.MessageSerializer;
import io.servicecomb.saga.omega.transport.resttemplate.TransactionHandlerInterceptorTest.MessageConfig;

@RunWith(SpringRunner.class)
@Import({MessageConfig.class})
public class TransactionHandlerInterceptorTest {
  private static final String TX_STARTED_EVENT = "TxStartedEvent";
  private static final String TX_ENDED_EVENT = "TxEndedEvent";
  private static final String globalTxId = UUID.randomUUID().toString();
  private static final String localTxId = UUID.randomUUID().toString();

  @Autowired
  private MessageSender sender;

  @Autowired
  private MessageSerializer serializer;

  @Autowired
  private TransactionHandlerInterceptor requestInterceptor;

  @Autowired
  private List<byte[]> messages;

  private HttpServletRequest request = mock(HttpServletRequest.class);

  private HttpServletResponse response = mock(HttpServletResponse.class);

  @After
  public void tearDown() throws Exception {
    messages.clear();
  }

  @Test
  public void preInterceptHeaderValueAndSendOut() throws Exception {
    when(request.getHeader(GLOBAL_TX_ID_KEY)).thenReturn(globalTxId);
    when(request.getHeader(LOCAL_TX_ID_KEY)).thenReturn(localTxId);

    requestInterceptor.preHandle(request, response, null);

    assertThat(messages.size(), is(1));
    String deserializedString = new String(messages.get(0));
    assertThat(deserializedString.contains(TX_STARTED_EVENT), is(true));
    assertThat(deserializedString.startsWith(globalTxId), is(true));
    assertThat(deserializedString.contains(localTxId), is(true));
  }

  @Test
  public void postInterceptHeaderValueAndSendOut() throws Exception {
    when(request.getHeader(GLOBAL_TX_ID_KEY)).thenReturn(globalTxId);
    when(request.getHeader(LOCAL_TX_ID_KEY)).thenReturn(localTxId);

    requestInterceptor.afterCompletion(request, response, null, null);

    assertThat(messages.size(), is(1));
    String deserializedString = new String(messages.get(0));
    assertThat(deserializedString.contains(TX_ENDED_EVENT), is(true));
    assertThat(deserializedString.startsWith(globalTxId), is(true));
    assertThat(deserializedString.contains(localTxId), is(true));
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
      return messages::add;
    }

    @Bean
    MessageSerializer serializer() {
      return event -> {
        if (TX_STARTED_EVENT.equals(event.type())) {
          return txStartedEvent(event.globalTxId(),
              event.localTxId(),
              event.parentTxId(),
              event.payloads()).getBytes();
        }
        return txEndedEvent(event.globalTxId(),
            event.localTxId(),
            event.parentTxId()).getBytes();
      };
    }

    @Bean
    HandlerInterceptor handlerInterceptor(MessageSender sender, MessageSerializer serializer) {
      return new TransactionHandlerInterceptor(sender, serializer);
    }
  }

  private static String txStartedEvent(String globalTxId,
      String localTxId,
      String parentTxId,
      Object[] payloads) {
    return globalTxId + ":" + localTxId + ":" + parentTxId + ":" + TX_STARTED_EVENT + ":" + Arrays.toString(payloads);
  }

  private static String txEndedEvent(String globalTxId, String localTxId, String parentTxId) {
    return globalTxId + ":" + localTxId + ":" + parentTxId + ":" + TX_ENDED_EVENT;
  }
}