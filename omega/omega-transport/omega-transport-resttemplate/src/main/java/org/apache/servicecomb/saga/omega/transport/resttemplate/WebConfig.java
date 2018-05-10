/*
 *
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
 *
 *
 */

package org.apache.servicecomb.saga.omega.transport.resttemplate;

import java.lang.invoke.MethodHandles;

import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableWebMvc
public class WebConfig extends WebMvcConfigurerAdapter {

  private final OmegaContext omegaContext;

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


  @Autowired
  public WebConfig(@Autowired(required=false) OmegaContext omegaContext) {
    this.omegaContext = omegaContext;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    if (omegaContext == null) {
      LOG.info("The OmegaContext is not injected, The transaction handler is disabled");
    }
    registry.addInterceptor(new TransactionHandlerInterceptor(omegaContext));
  }
}
