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

package org.apache.servicecomb.pack.omega.transaction.spring;

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class CompensableAnnotationCheckingTest {
  @Test
  public void blowsUpWhenCompensableMethodIsNotFound() throws Exception {
    try {
      try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder(TransactionTestMain.class)
          .profiles("annotation-checking")
          .properties("omega.spec.names=saga",
              "spring.jpa.hibernate.ddl-auto=none",
              "spring.datasource.driver-class-name=org.h2.Driver",
              "spring.datasource.url=jdbc:h2:mem:alpha;MODE=MYSQL")
          .run()) {
        expectFailing(BeanCreationException.class);
      }
    } catch (BeanCreationException e) {
      assertThat(e.getCause().getMessage(), startsWith("No such Compensation method [none]"));
    }
  }

  @Test
  public void blowsUpWhenCompensateRetriesIsBelowNegativeOne() throws Exception {
    try {
      try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder(TransactionTestMain.class)
          .profiles("annotation-retries-checking")
          .properties("omega.spec.names=saga",
              "spring.jpa.hibernate.ddl-auto=none",
              "spring.datasource.driver-class-name=org.h2.Driver",
              "spring.datasource.url=jdbc:h2:mem:alpha;MODE=MYSQL")
          .run()) {
        expectFailing(BeanCreationException.class);
      }
    } catch (BeanCreationException e) {
      assertThat(e.getCause().getMessage(), endsWith("the forward retries should not below -1."));
    }
  }

  @Test
  public void blowsUpWhenAnnotationOnWrongType() throws Exception {
    try {
      try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder(TransactionTestMain.class)
          .profiles("omega-context-aware-checking")
          .properties("omega.spec.names=saga",
              "spring.jpa.hibernate.ddl-auto=none",
              "spring.datasource.driver-class-name=org.h2.Driver",
              "spring.datasource.url=jdbc:h2:mem:alpha;MODE=MYSQL")
          .run()) {
        expectFailing(BeanCreationException.class);
      }
    } catch (BeanCreationException e) {
      assertThat(e.getCause().getMessage(),
          is("Only Executor, ExecutorService, and ScheduledExecutorService are supported for @OmegaContextAware"));
    }
  }
}
