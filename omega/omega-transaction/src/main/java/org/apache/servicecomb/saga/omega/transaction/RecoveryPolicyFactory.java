/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.saga.omega.transaction;

public class RecoveryPolicyFactory {
  private static final RecoveryPolicy DEFAULT_RECOVERY = new DefaultRecovery();

  private static final RecoveryPolicy FORWARD_RECOVERY = new ForwardRecovery();

  /**
   * If retries == 0, use the default recovery to execute only once.
   * If retries > 0, it will use the forward recovery and retry the given times at most.
   * If retries == -1, it will use the forward recovery and retry forever until interrupted.
   */
  static RecoveryPolicy getRecoveryPolicy(int retries) {
    return retries != 0 ? FORWARD_RECOVERY : DEFAULT_RECOVERY;
  }
}
