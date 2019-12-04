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

package org.apache.servicecomb.pack.omega.transaction;

import org.apache.servicecomb.pack.omega.context.OmegaContext;
import org.apache.servicecomb.pack.omega.context.annotations.SagaStart;
import org.apache.servicecomb.pack.omega.transaction.wrapper.SagaStartAnnotationProcessorTimeoutWrapper;
import org.apache.servicecomb.pack.omega.transaction.wrapper.SagaStartAnnotationProcessorWrapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;

@Aspect
@Order(value = 100)
public class SagaStartAspect {

  private final SagaStartAnnotationProcessor sagaStartAnnotationProcessor;

  private final OmegaContext context;

  public SagaStartAspect(SagaMessageSender sender, OmegaContext context) {
    this.context = context;
    this.sagaStartAnnotationProcessor = new SagaStartAnnotationProcessor(context, sender);
  }

  @Around("execution(@org.apache.servicecomb.pack.omega.context.annotations.SagaStart * *(..)) && @annotation(sagaStart)")
  Object advise(ProceedingJoinPoint joinPoint, SagaStart sagaStart) throws Throwable {
    initializeOmegaContext();
    if(context.getAlphaMetas().isAkkaEnabled() && sagaStart.timeout()>0){
      SagaStartAnnotationProcessorTimeoutWrapper wrapper = new SagaStartAnnotationProcessorTimeoutWrapper(this.sagaStartAnnotationProcessor);
      return wrapper.apply(joinPoint,sagaStart,context);
    }else{
      SagaStartAnnotationProcessorWrapper wrapper = new SagaStartAnnotationProcessorWrapper(this.sagaStartAnnotationProcessor);
      return wrapper.apply(joinPoint,sagaStart,context);
    }
  }

  private void initializeOmegaContext() {
    context.setLocalTxId(context.newGlobalTxId());
  }
}
