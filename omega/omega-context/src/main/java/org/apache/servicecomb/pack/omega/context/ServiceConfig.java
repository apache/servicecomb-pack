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

package org.apache.servicecomb.pack.omega.context;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ServiceConfig {
  private final String serviceName;
  private final String instanceId;
  // Current DB only supports instance id less then 35
  private static final int MAX_LENGTH = 35;

  public ServiceConfig(String serviceName) {
    this(serviceName,null);
  }

  public ServiceConfig(String serviceName, String instanceId) {
    this.serviceName = serviceName;
    if(instanceId == null || "".equalsIgnoreCase(instanceId.trim())){
      try {
        this.instanceId = serviceName + "-" + InetAddress.getLocalHost().getHostAddress();
      } catch (UnknownHostException e) {
        throw new IllegalStateException(e);
      }
    }else{
      instanceId = instanceId.trim();
      this.instanceId = instanceId;
    }

    if (this.instanceId.length() > MAX_LENGTH) {
      throw new IllegalArgumentException(String.format("The instanceId length exceeds maximum length limit [%d].", MAX_LENGTH));
    }
  }

  public String serviceName() {
    return serviceName;
  }

  public String instanceId() {
    return instanceId;
  }
}
