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

package org.apache.servicecomb.saga.omega.connector.grpc;

import java.util.Collections;
import java.util.List;
import org.apache.servicecomb.saga.omega.connector.grpc.tcc.TccMessageSender;
import org.apache.servicecomb.saga.omega.transaction.MessageDeserializer;
import org.apache.servicecomb.saga.omega.transaction.MessageHandler;
import org.apache.servicecomb.saga.omega.transaction.MessageSerializer;
import org.apache.servicecomb.saga.omega.transaction.tcc.TccMessageHandler;

public class AlphaClusterConfig {

  private List<String> addresses;

  private boolean enableSSL;

  private boolean enableMutualAuth;

  private String cert;

  private String key;

  private String certChain;

  private MessageSerializer messageSerializer;

  private MessageDeserializer messageDeserializer;

  private MessageHandler messageHandler;

  private TccMessageHandler tccMessageHandler;

  /**
   * @deprecated Use {@link Builder} instead.
   */
  @Deprecated
  public AlphaClusterConfig(List<String> addresses,
      boolean enableSSL,
      boolean enableMutualAuth,
      String cert,
      String key,
      String certChain) {
    this.addresses = addresses == null ? Collections.<String>emptyList() : addresses;
    this.enableMutualAuth = enableMutualAuth;
    this.enableSSL = enableSSL;
    this.cert = cert;
    this.key = key;
    this.certChain = certChain;
  }

  private AlphaClusterConfig(List<String> addresses, boolean enableSSL, boolean enableMutualAuth,
      String cert, String key, String certChain,
      MessageSerializer messageSerializer,
      MessageDeserializer messageDeserializer,
      MessageHandler messageHandler,
      TccMessageHandler tccMessageHandler) {
    this.addresses = addresses;
    this.enableSSL = enableSSL;
    this.enableMutualAuth = enableMutualAuth;
    this.cert = cert;
    this.key = key;
    this.certChain = certChain;
    this.messageSerializer = messageSerializer;
    this.messageDeserializer = messageDeserializer;
    this.messageHandler = messageHandler;
    this.tccMessageHandler = tccMessageHandler;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private List<String> addresses;
    private boolean enableSSL;
    private boolean enableMutualAuth;
    private String cert;
    private String key;
    private String certChain;
    private MessageSerializer messageSerializer;
    private MessageDeserializer messageDeserializer;
    private MessageHandler messageHandler;
    private TccMessageHandler tccMessageHandler;

    public Builder addresses(List<String> addresses) {
      this.addresses = addresses;
      return this;
    }

    public Builder enableSSL(boolean enableSSL) {
      this.enableSSL = enableSSL;
      return this;
    }

    public Builder enableMutualAuth(boolean enableMutualAuth) {
      this.enableMutualAuth = enableMutualAuth;
      return this;
    }

    public Builder cert(String cert) {
      this.cert = cert;
      return this;
    }

    public Builder key(String key) {
      this.key = key;
      return this;
    }

    public Builder certChain(String certChain) {
      this.certChain = certChain;
      return this;
    }

    public Builder messageSerializer(MessageSerializer messageSerializer) {
      this.messageSerializer = messageSerializer;
      return this;
    }

    public Builder messageDeserializer(MessageDeserializer messageDeserializer) {
      this.messageDeserializer = messageDeserializer;
      return this;
    }

    public Builder messageHandler(MessageHandler messageHandler) {
      this.messageHandler = messageHandler;
      return this;
    }

    public Builder tccMessageHandler(TccMessageHandler tccMessageHandler) {
      this.tccMessageHandler = tccMessageHandler;
      return this;
    }


    public AlphaClusterConfig build() {
      return new AlphaClusterConfig(this.addresses,
          this.enableSSL,
          this.enableMutualAuth,
          this.cert,
          this.key,
          this.certChain,
          this.messageSerializer,
          this.messageDeserializer,
          messageHandler,
          tccMessageHandler);
    }
  }

  public List<String> getAddresses() {
    return addresses;
  }

  public boolean isEnableSSL() {
    return enableSSL;
  }

  public boolean isEnableMutualAuth() {
    return enableMutualAuth;
  }

  public String getCert() {
    return cert;
  }

  public String getKey() {
    return key;
  }

  public String getCertChain() {
    return certChain;
  }

  public MessageSerializer getMessageSerializer() {
    return messageSerializer;
  }

  public MessageDeserializer getMessageDeserializer() {
    return messageDeserializer;
  }

  public MessageHandler getMessageHandler() {
    return messageHandler;
  }

  public TccMessageHandler getTccMessageHandler() {
    return tccMessageHandler;
  }
}