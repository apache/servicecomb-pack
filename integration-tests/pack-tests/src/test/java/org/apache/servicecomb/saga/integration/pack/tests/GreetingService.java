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

import org.springframework.stereotype.Service;

import org.apache.servicecomb.saga.omega.transaction.annotations.Compensable;

@Service
public class GreetingService {
  @Compensable(compensationMethod = "goodbye")
  String greet(String name) {
    return "Greetings, " + name;
  }

  String goodbye(String name) {
    return "Goodbye, " + name;
  }

  @Compensable(compensationMethod = "auRevoir")
  String bonjour(String name) {
    return "Bonjour, " + name;
  }

  String auRevoir(String name) {
    return "Au revoir, " + name;
  }
}
