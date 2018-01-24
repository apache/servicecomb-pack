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

import java.util.concurrent.atomic.AtomicReference;

class TimeAwareInterceptor implements EventAwareInterceptor {
  private final EventAwareInterceptor interceptor;
  private final AtomicReference<EventAwareInterceptor> interceptorRef;

  TimeAwareInterceptor(EventAwareInterceptor interceptor) {
    this.interceptor = interceptor;
    this.interceptorRef = new AtomicReference<>(interceptor);
  }

  @Override
  public AlphaResponse preIntercept(String parentTxId, String retriesMethod, String signature, int retries,
      Object... args) {
    return interceptor.preIntercept(parentTxId, retriesMethod, signature, retries, args);
  }

  @Override
  public void postIntercept(String parentTxId, String signature) {
    if (interceptorRef.compareAndSet(interceptor, NO_OP_INTERCEPTOR)) {
      interceptor.postIntercept(parentTxId, signature);
    }
  }

  @Override
  public void onError(String parentTxId, String signature, Throwable throwable) {
    if (interceptorRef.compareAndSet(interceptor, NO_OP_INTERCEPTOR)) {
      interceptor.onError(parentTxId, signature, throwable);
    }
  }

  void onTimeout(String parentTxId, String signature, Throwable throwable) {
    if (interceptorRef.compareAndSet(interceptor, NO_OP_INTERCEPTOR)) {
      interceptor.onError(parentTxId, signature, throwable);
    }
  }
}
