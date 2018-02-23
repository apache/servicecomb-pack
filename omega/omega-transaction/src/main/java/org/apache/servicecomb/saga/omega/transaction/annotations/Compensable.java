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

package org.apache.servicecomb.saga.omega.transaction.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates the annotated method will start a sub-transaction. <br>
 * A <code>@Compensable</code> method should satisfy below requirements:
 * <ol>
 *   <li>all parameters are serialized</li>
 *   <li>is idempotent</li>
 *   <li>the object instance which @Compensable method resides in should be stateless</li>
 *   <li>if compensationMethod exists, both methods must be commutative, see this <a href="https://servicecomb.incubator.apache.org/docs/distributed_saga_2/">link</a>.</li>
 * </ol>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Compensable {

  int retries() default 0;

  /**
   * Compensation method name.<br>
   * A compensation method should satisfy below requirements:
   * <ol>
   *   <li>has same parameter list as @Compensable method's</li>
   *   <li>all parameters are serialized</li>
   *   <li>is idempotent</li>
   *   <li>be in the same class as @Compensable method is in</li>
   * </ol>
   *
   * @return
   */
  String compensationMethod() default "";

  int retryDelayInMilliseconds() default 0;

  /**
   * <code>@Compensable</code> method timeout, in seconds. <br>
   * Default value is 0, which means never timeout.
   *
   * @return
   */
  int timeout() default 0;
}
