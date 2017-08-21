/*
 * Copyright 2017 Huawei Technologies Co., Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.servicecomb.saga.spring;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import io.servicecomb.saga.core.application.SagaCoordinator;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/requests")
public class SagaController {

  private final SagaCoordinator sagaCoordinator;

  @Autowired
  public SagaController(SagaCoordinator sagaCoordinator) {
    this.sagaCoordinator = sagaCoordinator;
  }

  @RequestMapping(value = "/", method = POST, consumes = APPLICATION_JSON_VALUE)
  public ResponseEntity<String> processRequests(HttpServletRequest request) {
    try {
      sagaCoordinator.run(IOUtils.toString(request.getInputStream()));
      return ResponseEntity.ok("success");
    } catch (IOException e) {
      return new ResponseEntity<>(e.getMessage(), INTERNAL_SERVER_ERROR);
    }
  }
}
