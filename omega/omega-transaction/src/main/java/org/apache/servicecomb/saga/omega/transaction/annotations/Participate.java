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

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
/**
 * Indicates the annotated method will start a sub-transaction. <br>
 * A <code>@Participate</code> method should satisfy below requirements:
 * <ol>
 *   <li>all parameters are serialized</li>
 *   <li>is idempotent</li>
 *   <li>the object instance which @Participate method resides in should be stateless</li>
 * </ol>
 */
public @interface Participate {
  /**
   * Confirm method name.<br>
   * A confirm method should satisfy below requirements:
   * <ol>
   *   <li>has same parameter list as @Participate method's</li>
   *   <li>all parameters are serialized</li>
   *   <li>is idempotent</li>
   *   <li>be in the same class as @Participate method is in</li>
   * </ol>
   *
   * @return
   */
  String confirmMethod() default "";

  /**
   * Cancel method name.<br>
   * A cancel method should satisfy below requirements:
   * <ol>
   *   <li>has same parameter list as @Participate method's</li>
   *   <li>all parameters are serialized</li>
   *   <li>is idempotent</li>
   *   <li>be in the same class as @Participate method is in</li>
   * </ol>
   *
   * @return
   */
  String cancelMethod() default "";

}
