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
import java.util.concurrent.ThreadPoolExecutor;

/**
 * custom hystrix callable wrapper
 */
public interface HystrixCallableWrapper {

  /**
   * is need wrap
   *
   * @return return true will invoke  {@link #wrapCallable(Callable)}
   */
  boolean shouldWrap();

  /**
   * Provides an opportunity to wrap/decorate a {@code Callable<T>} before execution.
   * <p>
   * This can be used to inject additional behavior such as copying of thread state (such as {@link
   * ThreadLocal}).
   *
   * @param callable {@code Callable<T>} to be executed via a {@link ThreadPoolExecutor}
   * @return {@code Callable<T>} either as a pass-thru or wrapping the one given
   */
  <T> Callable<T> wrapCallable(Callable<T> callable);

}
