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

import java.util.Queue;

import org.apache.servicecomb.saga.omega.transaction.annotations.Compensable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
class GreetingService {
  private final Queue<String> compensated;

  private final int MAX_COUNT = 3;
  private int failedCount = 1;

  @Autowired
  GreetingService(Queue<String> compensated) {
    this.compensated = compensated;
  }

  @Compensable(compensationMethod = "goodbye")
  String greet(String name) {
    return "Greetings, " + name;
  }

  String goodbye(String name) {
    return appendMessage("Goodbye, " + name);
  }

  @Compensable(compensationMethod = "auRevoir")
  String bonjour(String name) {
    return "Bonjour, " + name;
  }

  String auRevoir(String name) {
    return appendMessage("Au revoir, " + name);
  }

  @Compensable(compensationMethod = "apologize")
  String beingRude(String name) {
    throw new IllegalStateException("You know where the door is, " + name);
  }

  String apologize(String name) {
    return appendMessage("My bad, please take the window instead, " + name);
  }

  @Compensable(retries = MAX_COUNT, compensationMethod = "close")
  String open(String name, int retries) {
    if (failedCount < retries) {
      failedCount += 1;
      throw new IllegalStateException("You know when the zoo opens, " + name);
    }
    resetCount();
    return "Welcome to visit the zoo, " + name;
  }

  String close(String name, int retries) {
    resetCount();
    return appendMessage("Sorry, the zoo has already closed, " + name);
  }

  private String appendMessage(String message) {
    compensated.add(message);
    return message;
  }

  void resetCount() {
    this.failedCount = 1;
  }
}
