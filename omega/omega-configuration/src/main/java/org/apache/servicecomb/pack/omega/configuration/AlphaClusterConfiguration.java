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

package org.apache.servicecomb.pack.omega.configuration;

import java.util.Collections;
import java.util.List;

public class AlphaClusterConfiguration {

  private List<String> addresses = Collections.singletonList("localhost:8080");

  private boolean enableSSL;

  private boolean mutualAuth;

  private String cert = "client.crt";

  private String key = "client.pem";

  private String certChain = "ca.crt";

  public List<String> getAddresses() {
    return addresses;
  }

  public void setAddresses(List<String> addresses) {
    this.addresses = addresses;
  }

  public boolean isEnableSSL() {
    return enableSSL;
  }

  public void setEnableSSL(boolean enableSSL) {
    this.enableSSL = enableSSL;
  }

  public boolean isMutualAuth() {
    return mutualAuth;
  }

  public void setMutualAuth(boolean mutualAuth) {
    this.mutualAuth = mutualAuth;
  }

  public String getCert() {
    return cert;
  }

  public void setCert(String cert) {
    this.cert = cert;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getCertChain() {
    return certChain;
  }

  public void setCertChain(String certChain) {
    this.certChain = certChain;
  }
}
