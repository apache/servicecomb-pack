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

package org.apache.servicecomb.saga.demo.scb.booking;

import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.apache.servicecomb.provider.springmvc.reference.RestTemplateBuilder;
import org.apache.servicecomb.saga.omega.context.annotations.SagaStart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestSchema(schemaId = "booking")
@RequestMapping(path = "/")
public class BookingController {

  private RestTemplate template = RestTemplateBuilder.create();

  @SagaStart
  @PostMapping("/booking/{name}/{rooms}/{cars}")
  public String order(@PathVariable String name,  @PathVariable Integer rooms, @PathVariable Integer cars) {
    template.postForEntity(
        "cse://car/order/{name}/{cars}",
        null, String.class, name, cars);

    template.postForEntity(
        "cse://hotel/order/{name}/{rooms}",
        null, String.class, name, rooms);

    postBooking();

    return name + " booking " + rooms + " rooms and " + cars + " cars OK";
  }

  // This method is used by the byteman to inject the faults such as the timeout or the crash
  private void postBooking() {

  }
}
