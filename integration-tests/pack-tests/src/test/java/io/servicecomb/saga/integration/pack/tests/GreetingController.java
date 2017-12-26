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

package io.servicecomb.saga.integration.pack.tests;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.servicecomb.saga.omega.context.OmegaContext;

@Controller
@RequestMapping("/")
public class GreetingController {
  private final GreetingService greetingService;
  private final OmegaContext context;

  @Autowired
  public GreetingController(GreetingService greetingService, OmegaContext context) {
    this.greetingService = greetingService;
    this.context = context;
  }


  @GetMapping("/greet")
  ResponseEntity<String> greet(@RequestParam String name) {
    // TODO: 2017/12/26 to be removed when tx id retrieval is done
    context.setGlobalTxId(UUID.randomUUID().toString());
    context.setLocalTxId(UUID.randomUUID().toString());

    return ResponseEntity.ok(greetingService.greet(name));
  }
}
