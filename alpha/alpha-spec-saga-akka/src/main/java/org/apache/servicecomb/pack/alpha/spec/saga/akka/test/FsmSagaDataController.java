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

package org.apache.servicecomb.pack.alpha.spec.saga.akka.test;

import akka.actor.ActorSystem;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.servicecomb.pack.alpha.core.TxEvent;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.model.SagaData;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.spring.integration.akka.SagaDataExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/test/saga/akka")
@Profile("test")
@ConditionalOnProperty(name= "alpha.feature.akka.enabled", havingValue = "true")
// Only export this Controller for test
public class FsmSagaDataController {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  ActorSystem system;

  @GetMapping(value = "/events/last")
  public ResponseEntity<Collection<Map>> events() {
    LOG.info("Get the events request");
    List<Map> eventVos = new LinkedList<>();
    SagaData data = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    if (data != null && data.getEvents() != null) {
      data.getEvents().forEach(event -> {
        Map<String, String> obj = new HashMap();
        obj.put("serviceName", event.getServiceName());
        obj.put("type", event.getClass().getSimpleName());
        eventVos.add(obj);
      });
      LOG.info("Get the event size {}", eventVos.size());
      LOG.info("Get the event data {}", eventVos);
    }
    return ResponseEntity.ok(eventVos);
  }

  @DeleteMapping("/events")
  public ResponseEntity<String> clear() {
    return ResponseEntity.ok("All events deleted");
  }

  @JsonAutoDetect(fieldVisibility = Visibility.ANY)
  private static class TxEventVo extends TxEvent {
    private TxEventVo(TxEvent event) {
      super(event);
    }
  }
}
