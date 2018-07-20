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

package org.apache.servicecomb.saga.format;

import java.util.HashMap;
import java.util.Map;

import org.apache.servicecomb.saga.core.ForwardRecovery;
import org.apache.servicecomb.saga.core.RecoveryPolicy;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.servicecomb.saga.core.BackwardRecovery;
import org.apache.servicecomb.saga.core.SagaDefinition;

class JsonSagaDefinition implements SagaDefinition {

  static final RecoveryPolicy backwardRecovery = new BackwardRecovery();

  private static final Map<String, RecoveryPolicy> policies = new HashMap<String, RecoveryPolicy>(){{
    put(RecoveryPolicy.SAGA_BACKWARD_RECOVERY_POLICY, backwardRecovery);
    put(RecoveryPolicy.SAGA_FORWARD_RECOVERY_POLICY, new ForwardRecovery());
  }};

  private final JsonSagaRequest[] requests;
  private final RecoveryPolicy policy;

  public JsonSagaDefinition(
      @JsonProperty("policy") String policy,
      @JsonProperty("requests") JsonSagaRequest[] requests) {

    this.requests = requests;
    this.policy = policies.getOrDefault(policy, backwardRecovery);
  }

  @Override
  public RecoveryPolicy policy() {
    return policy;
  }

  @Override
  public JsonSagaRequest[] requests() {
    return requests;
  }
}
