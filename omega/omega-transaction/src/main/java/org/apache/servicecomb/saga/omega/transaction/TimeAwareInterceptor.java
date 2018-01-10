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

package org.apache.servicecomb.saga.omega.transaction;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeoutException;

class TimeAwareInterceptor implements EventAwareInterceptor {
  private final EventAwareInterceptor interceptor;
  private final BlockingDeque<EventAwareInterceptor> interceptors = new LinkedBlockingDeque<>(2);

  TimeAwareInterceptor(EventAwareInterceptor interceptor) {
    this.interceptor = interceptor;
    this.interceptors.offer(interceptor);
  }

  @Override
  public void preIntercept(String localTxId, String signature, Object... args) {
    interceptor.preIntercept(localTxId, signature, args);
  }

  @Override
  public void postIntercept(String localTxId, String signature) {
    interceptors.offerLast(NO_OP_INTERCEPTOR);
    interceptors.pollFirst().postIntercept(localTxId, signature);
  }

  @Override
  public void onError(String localTxId, String signature, Throwable throwable) {
    interceptors.offerLast(NO_OP_INTERCEPTOR);
    interceptors.pollFirst().onError(localTxId, signature, throwable);
  }

  void onTimeout(String signature, String localTxId) {
    interceptors.offerFirst(NO_OP_INTERCEPTOR);
    interceptors.pollLast().onError(localTxId, signature, new TimeoutException());
  }
}
