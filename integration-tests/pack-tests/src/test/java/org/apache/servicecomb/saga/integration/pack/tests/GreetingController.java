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

package org.apache.servicecomb.saga.integration.pack.tests;

import org.apache.servicecomb.saga.omega.context.annotations.SagaStart;
import org.apache.servicecomb.saga.omega.transaction.annotations.Compensable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

@Controller
@RequestMapping("/")
public class GreetingController {
  static final String TRESPASSER = "trespasser";
  private final GreetingService greetingService;
  private final RestTemplate restTemplate;

  @Autowired
  public GreetingController(GreetingService greetingService, RestTemplate restTemplate) {
    this.greetingService = greetingService;
    this.restTemplate = restTemplate;
  }

  @SagaStart
  @GetMapping("/greet")
  ResponseEntity<String> greet(@RequestParam String name) {
    String greetings = greetingService.greet(name);

    if (!TRESPASSER.equals(name)) {
      String bonjour = restTemplate.getForObject("http://localhost:8080/bonjour?name={name}", String.class, name);

      return ResponseEntity.ok(greetings + "; " + bonjour);
    }

    String rude = restTemplate.getForObject("http://localhost:8080/rude?name={name}", String.class, name);

    return ResponseEntity.ok(greetings + "; " + rude);
  }

  @GetMapping("/bonjour")
  ResponseEntity<String> bonjour(@RequestParam String name) {
    return ResponseEntity.ok(greetingService.bonjour(name));
  }

  @GetMapping("/rude")
  ResponseEntity<String> rude(@RequestParam String name) {
    return ResponseEntity.ok(greetingService.beingRude(name));
  }

  @SagaStart
  @Compensable(compensationMethod = "goodNight")
  @GetMapping("/goodMorning")
  ResponseEntity<String> goodMorning(@RequestParam String name) {
    String bonjour = restTemplate.getForObject("http://localhost:8080/bonjour?name={name}", String.class, name);
    return ResponseEntity.ok("Good morning, " + bonjour);
  }

  ResponseEntity<String> goodNight(@RequestParam String name) {
    return ResponseEntity.ok("Good night, " + name);
  }

  @SagaStart
  @GetMapping("/open")
  ResponseEntity<String> open(@RequestParam String name, @RequestParam int retries) {
    String greetings = greetingService.greet(name);
    String status = greetingService.open(name, retries);
    return ResponseEntity.ok(greetings + "; " + status);
  }
}
