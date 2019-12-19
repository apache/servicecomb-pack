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

package org.apache.servicecomb.pack.integration.tests.explicitcontext;

import org.apache.servicecomb.pack.omega.context.OmegaContext;
import org.apache.servicecomb.pack.omega.context.annotations.SagaStart;
import org.apache.servicecomb.pack.omega.transaction.annotations.Compensable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

@Controller
@RequestMapping("/")
public class GreetingController {

  static final String TRESPASSER = "trespasser";

  private final GreetingService greetingService;

  private final RestTemplate restTemplate;

  private final OmegaContext omegaContext;


  @Autowired
  public GreetingController(GreetingService greetingService, RestTemplate restTemplate, OmegaContext omegaContext) {
    this.greetingService = greetingService;
    this.restTemplate = restTemplate;
    this.omegaContext = omegaContext;
  }

  @SagaStart
  @GetMapping("/greet")
  ResponseEntity<String> greet(@RequestParam String name) {

    HttpEntity transactionContext = transactionContextRequestBody();
    String greetings = greetingService.greet(name);

    if (!TRESPASSER.equals(name)) {
      String bonjour = restTemplate
          .postForObject("/bonjour?name={name}", transactionContext, String.class, name);
      return ResponseEntity.ok(greetings + "; " + bonjour);
    }

    String rude = restTemplate
        .postForObject("/rude?name={name}", transactionContext, String.class, name);

    return ResponseEntity.ok(greetings + "; " + rude);
  }

  @PostMapping(value = "/bonjour", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
  ResponseEntity<String> bonjour(@RequestParam String name, @RequestBody TransactionContextDto transactionContext) {
    return ResponseEntity.ok(greetingService.bonjour(name, transactionContext.convertBack()));
  }

  @PostMapping(value = "/rude", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
  ResponseEntity<String> rude(@RequestParam String name, @RequestBody TransactionContextDto transactionContext) {
    return ResponseEntity.ok(greetingService.beingRude(name, transactionContext.convertBack()));
  }

  @SagaStart
  @Compensable(compensationMethod = "goodNight")
  @GetMapping("/goodMorning")
  ResponseEntity<String> goodMorning(@RequestParam String name) {
    String bonjour = restTemplate
        .postForObject("/bonjour?name={name}", transactionContextRequestBody(), String.class,
            name);
    return ResponseEntity.ok("Good morning, " + bonjour);
  }

  ResponseEntity<String> goodNight(@RequestParam String name) {
    return ResponseEntity.ok("Good night, " + name);
  }

  @SagaStart
  @GetMapping("/open")
  ResponseEntity<String> open(@RequestParam String name, @RequestParam int retries) {

    String greetings = greetingService.greet(name);
    String status = greetingService.open(name, retries, omegaContext.getTransactionContext());
    return ResponseEntity.ok(greetings + "; " + status);
  }

  private HttpEntity transactionContextRequestBody() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON_UTF8);

    HttpEntity transactionContext = new HttpEntity<>(
        TransactionContextDto.convert(omegaContext.getTransactionContext()),
        headers
    );

    return transactionContext;
  }
}
