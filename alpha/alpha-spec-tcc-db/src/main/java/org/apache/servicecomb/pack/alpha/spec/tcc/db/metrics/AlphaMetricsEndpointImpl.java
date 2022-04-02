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

package org.apache.servicecomb.pack.alpha.spec.tcc.db.metrics;

import org.apache.servicecomb.pack.alpha.core.NodeStatus;
import org.apache.servicecomb.pack.alpha.core.NodeStatus.TypeEnum;
import org.apache.servicecomb.pack.alpha.core.metrics.AlphaMetricsEndpoint;
import org.apache.servicecomb.pack.alpha.core.metrics.MetricsBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class AlphaMetricsEndpointImpl implements AlphaMetricsEndpoint {

  @Autowired
  @Lazy
  private NodeStatus nodeStatus;

  @Autowired
  MetricsService metricsService;

  public MetricsBean getMetrics() {
    return metricsService != null ? metricsService.metrics() : null;
  }

  public TypeEnum getNodeType(){
    return nodeStatus.getTypeEnum();
  }

}
