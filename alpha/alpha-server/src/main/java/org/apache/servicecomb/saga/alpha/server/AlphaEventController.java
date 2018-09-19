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

package org.apache.servicecomb.saga.alpha.server;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import kamon.annotation.EnableKamon;
import kamon.annotation.Trace;

@EnableKamon
@Controller
@RequestMapping("/saga")
@Profile("test")
// Only export this Controller for test
class AlphaEventController {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final TxEventEnvelopeRepository eventRepository;

  AlphaEventController(TxEventEnvelopeRepository eventRepository) {
    this.eventRepository = eventRepository;
  }

  @Trace("getEvents")
  @GetMapping(value = "/events")
  ResponseEntity<Collection<TxEventVo>> events() {
    LOG.info("Get the events request");
    Iterable<TxEvent> events = eventRepository.findAll();

    List<TxEventVo> eventVos = new LinkedList<>();
    events.forEach(event -> eventVos.add(new TxEventVo(event)));
    LOG.info("Get the event size " + eventVos.size());

    return ResponseEntity.ok(eventVos);
  }

  @Trace("deleteEvents")
  @DeleteMapping("/events")
  ResponseEntity<String> clear() {
    eventRepository.deleteAll();
    return ResponseEntity.ok("All events deleted");
  }

  @JsonAutoDetect(fieldVisibility = Visibility.ANY)
  private static class TxEventVo extends TxEvent {
    private TxEventVo(TxEvent event) {
      super(event);
    }
  }
}
