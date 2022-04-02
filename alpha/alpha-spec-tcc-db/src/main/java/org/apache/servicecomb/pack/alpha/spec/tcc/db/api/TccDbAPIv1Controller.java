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

package org.apache.servicecomb.pack.alpha.spec.tcc.db.api;

import org.apache.servicecomb.pack.alpha.core.api.APIv1;
import org.apache.servicecomb.pack.alpha.core.metrics.AlphaMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/alpha/api/v1")
public class TccDbAPIv1Controller {

  @Autowired
  APIv1 apIv1;

  @GetMapping(value = "/metrics")
  public ResponseEntity<AlphaMetrics> metrics() {
    return ResponseEntity.ok(apIv1.getMetrics());
  }
}
