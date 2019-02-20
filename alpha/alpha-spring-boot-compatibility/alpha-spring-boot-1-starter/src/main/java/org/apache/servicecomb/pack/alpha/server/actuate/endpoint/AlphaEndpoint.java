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

package org.apache.servicecomb.pack.alpha.server.actuate.endpoint;

import org.apache.servicecomb.pack.alpha.core.NodeStatus;
import org.apache.servicecomb.pack.alpha.core.actuate.endpoint.AlphaStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "endpoints.alpha")
@Component
public class AlphaEndpoint implements Endpoint {

  public static final String END_POINT_ID = "alpha";

  @Autowired
  NodeStatus nodeStatus;

  private AlphaStatus alphaStatus = new AlphaStatus();

  @Override
  public String getId() {
    return END_POINT_ID;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public boolean isSensitive() {
    return false;
  }

  @Override
  public AlphaStatus invoke() {
    alphaStatus.setNodeType(nodeStatus.getTypeEnum());
    return alphaStatus;
  }

}
