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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcServerConfig {
  @Value("${alpha.server.host:0.0.0.0}")
  private String host;

  @Value("${alpha.server.port:8080}")
  private int port;

  @Value("${alpha.server.ssl.enable:false}")
  private boolean sslEnable;

  @Value("${alpha.server.ssl.cert:server.crt}")
  private String cert;

  @Value("${alpha.server.ssl.key:server.pem}")
  private String key;

  @Value("${alpha.server.ssl.mutualAuth:false}")
  private boolean mutualAuth;

  @Value("${alpha.server.ssl.clientCert:client.crt}")
  private String clientCert;

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public boolean isSslEnable() {
    return sslEnable;
  }

  public String getCert() {
    return cert;
  }

  public String getKey() {
    return key;
  }

  public boolean isMutualAuth() {
    return mutualAuth;
  }

  public String getClientCert() {
    return clientCert;
  }
}


