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

package org.apache.servicecomb.saga.spring;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import org.apache.servicecomb.provider.rest.common.RestSchema;

@Controller
@RequestMapping("/rest")
@RestSchema(schemaId = "greeting-rest-endpoint")
public class GreetingController {

  @RequestMapping(value = "/usableResource", method = POST, consumes = APPLICATION_FORM_URLENCODED_VALUE)
  public ResponseEntity<String> postUsableResource(
      @RequestAttribute(name = "hello") String who,
      @RequestAttribute(name = "response") String response) {

    return ResponseEntity.ok("hello " + who + ", with response " + response);
  }
}
