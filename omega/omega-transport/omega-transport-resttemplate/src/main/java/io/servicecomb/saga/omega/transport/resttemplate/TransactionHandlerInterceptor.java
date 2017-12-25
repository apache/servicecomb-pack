/*
 *
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
 *
 */

package io.servicecomb.saga.omega.transport.resttemplate;

import static io.servicecomb.saga.omega.transport.resttemplate.TransactionClientHttpRequestInterceptor.GLOBAL_TX_ID_KEY;
import static io.servicecomb.saga.omega.transport.resttemplate.TransactionClientHttpRequestInterceptor.LOCAL_TX_ID_KEY;

import java.lang.invoke.MethodHandles;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import io.servicecomb.saga.omega.transaction.MessageSender;
import io.servicecomb.saga.omega.transaction.MessageSerializer;
import io.servicecomb.saga.omega.transaction.TxEndedEvent;
import io.servicecomb.saga.omega.transaction.TxStartedEvent;

public class TransactionHandlerInterceptor implements HandlerInterceptor {

  private static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final MessageSender sender;

  private final MessageSerializer serializer;

  public TransactionHandlerInterceptor(MessageSender sender, MessageSerializer serializer) {
    this.sender = sender;
    this.serializer = serializer;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    String globalTxId = request.getHeader(GLOBAL_TX_ID_KEY);
    if (globalTxId == null) {
      LOG.info("no such header: {}", GLOBAL_TX_ID_KEY);
    }
    String localTxId = request.getHeader(LOCAL_TX_ID_KEY);
    if (localTxId == null) {
      LOG.info("no such header: {}", LOCAL_TX_ID_KEY);
    }
    // TODO: 12/25/2017 which content should be inside payloads?
    sender.send(serializer.serialize(new TxStartedEvent(globalTxId, localTxId, null, null)));
    return true;
  }

  @Override
  public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o,
      ModelAndView modelAndView) throws Exception {
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object o,
      Exception e) throws Exception {
    String globalTxId = request.getHeader(GLOBAL_TX_ID_KEY);
    if (globalTxId == null) {
      LOG.info("no such header: {}", GLOBAL_TX_ID_KEY);
    }
    String localTxId = request.getHeader(LOCAL_TX_ID_KEY);
    if (localTxId == null) {
      LOG.info("no such header: {}", LOCAL_TX_ID_KEY);
    }
    sender.send(serializer.serialize(new TxEndedEvent(globalTxId, localTxId, null)));
  }
}
