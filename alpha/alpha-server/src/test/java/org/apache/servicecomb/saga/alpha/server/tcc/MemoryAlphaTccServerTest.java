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

package org.apache.servicecomb.saga.alpha.server.tcc;

import io.grpc.netty.NettyChannelBuilder;
import org.apache.servicecomb.saga.alpha.server.AlphaApplication;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {AlphaApplication.class},
    properties = {
        "alpha.server.host=0.0.0.0",
        "alpha.server.port=8090"
    })
@ActiveProfiles("memory")
public class MemoryAlphaTccServerTest extends AlphaTccServerTestBase {

  @BeforeClass
  public static void setupClientChannel() {
    clientChannel = NettyChannelBuilder.forAddress("localhost", 8090).usePlaintext().build();
  }

  /*@Autowired
  @Qualifier("defaultTccTxEventFacade")
  private TccTxEventFacade tccTxEventFacade;

  @Override
  public TccTxEventFacade getTccTxEventFacade() {
    return tccTxEventFacade;
  }*/
  
}
