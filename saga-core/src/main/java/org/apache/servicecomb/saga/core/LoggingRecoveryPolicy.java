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

package org.apache.servicecomb.saga.core;

import java.lang.invoke.MethodHandles;
import kamon.annotation.EnableKamon;
import kamon.annotation.Segment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@EnableKamon
public class LoggingRecoveryPolicy implements RecoveryPolicy {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final RecoveryPolicy recoveryPolicy;

  public LoggingRecoveryPolicy(RecoveryPolicy recoveryPolicy) {
    this.recoveryPolicy = recoveryPolicy;
  }

  @Segment(name = "loggingPolicy", category = "application", library = "kamon")
  @Override
  public SagaResponse apply(SagaTask task, SagaRequest request, SagaResponse parentResponse) {
    log.info("Starting request id={} for service {}", request.id(), request.serviceName());
    SagaResponse response = recoveryPolicy.apply(task, request, parentResponse);
    log.info("Completed request id={} for service {}", request.id(), request.serviceName());
    return response;
  }
}
