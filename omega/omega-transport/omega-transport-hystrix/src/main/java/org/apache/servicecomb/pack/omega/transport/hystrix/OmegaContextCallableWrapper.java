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

package org.apache.servicecomb.pack.omega.transport.hystrix;

import java.util.concurrent.Callable;
import org.apache.commons.lang3.StringUtils;
import org.apache.servicecomb.pack.omega.context.OmegaContext;

/**
 * process omegaContext ，passing threadLocal variables
 */
public class OmegaContextCallableWrapper implements HystrixCallableWrapper {

  private OmegaContext context;

  public OmegaContextCallableWrapper(OmegaContext context) {
    this.context = context;
  }

  @Override
  public boolean shouldWrap() {
    return context != null && StringUtils.isNotEmpty(context.globalTxId());
  }

  @Override
  public <T> Callable<T> wrapCallable(Callable<T> callable) {
    return new WrappedCallable<>(callable, context.globalTxId(), context.localTxId(), context);
  }

  static class WrappedCallable<T> implements Callable<T> {

    private final Callable<T> target;

    private final String globalTxId;
    private final String localTxId;
    private final OmegaContext omegaContext;

    public WrappedCallable(Callable<T> target, String globalTxId, String localTxId,
        OmegaContext omegaContext) {
      this.target = target;
      this.omegaContext = omegaContext;
      this.globalTxId = globalTxId;
      this.localTxId = localTxId;
    }

    @Override
    public T call() throws Exception {
      try {
        omegaContext.setGlobalTxId(globalTxId);
        omegaContext.setLocalTxId(localTxId);
        return target.call();
      } finally {
        omegaContext.clear();
      }
    }
  }
}
