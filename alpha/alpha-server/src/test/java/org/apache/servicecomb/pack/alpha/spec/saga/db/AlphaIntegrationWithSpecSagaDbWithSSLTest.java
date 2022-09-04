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

package org.apache.servicecomb.pack.alpha.spec.saga.db;

import java.io.File;
import java.util.Arrays;
import javax.net.ssl.SSLException;

import org.apache.servicecomb.pack.alpha.server.AlphaApplication;
import org.apache.servicecomb.pack.alpha.server.AlphaConfig;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslProvider;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {AlphaApplication.class, AlphaConfig.class},
    properties = {
        "alpha.server.host=0.0.0.0",
        "alpha.server.port=8092",
        "alpha.event.pollingInterval=1",
        "spring.main.allow-bean-definition-overriding=true",
        "alpha.spec.names=saga-db",
        "alpha.spec.saga.db.datasource.username=sa",
        "alpha.spec.saga.db.datasource.url=jdbc:hsqldb:mem:saga",
        "alpha.spec.saga.db.cluster.enabled=false",
        "spring.profiles.active=ssl"
    })
public class AlphaIntegrationWithSpecSagaDbWithSSLTest extends AlphaIntegrationWithSpecSagaDbTest {
  private static final int port = 8092;

  @BeforeClass
  public static void setupClientChannel() {
    clientChannel = NettyChannelBuilder.forAddress("localhost", port)
        .negotiationType(NegotiationType.TLS)
        .sslContext(getSslContext())
        .build();
  }

  private static SslContext getSslContext(){
    ClassLoader classLoader = AlphaIntegrationWithSpecSagaDbWithSSLTest.class.getClassLoader();
    SslContext sslContext = null;
    try {
      sslContext = GrpcSslContexts.forClient().sslProvider(SslProvider.OPENSSL)
          .protocols("TLSv1.2","TLSv1.1")
          .ciphers(Arrays.asList("ECDHE-RSA-AES128-GCM-SHA256",
              "ECDHE-RSA-AES256-GCM-SHA384"))
          .trustManager(new File(classLoader.getResource("ca.crt").getFile()))
          .keyManager(new File(classLoader.getResource("client.crt").getFile()),
              new File(classLoader.getResource("client.pem").getFile())).build();
    } catch (SSLException e) {
      e.printStackTrace();
    }
    return sslContext;
  }

}
