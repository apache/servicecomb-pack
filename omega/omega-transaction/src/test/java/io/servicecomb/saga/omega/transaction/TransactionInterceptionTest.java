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

package io.servicecomb.saga.omega.transaction;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import io.servicecomb.saga.omega.transaction.TransactionInterceptionTest.MessageConfig;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TransactionTestMain.class, MessageConfig.class})
public class TransactionInterceptionTest {
  private final String username = uniquify("username");
  private final String email = uniquify("email");

  @Autowired
  private List<byte[]> messages;

  @Autowired
  private TransactionalUserService userService;

  @Test
  public void sendsUserToRemote_BeforeTransaction() throws Exception {
    userService.add(new User(username, email));

    assertThat(messages, contains((username + ":" + email).getBytes()));
  }

  @Configuration
  static class MessageConfig {
    private final List<byte[]> messages = new ArrayList<>();

    @Bean
    List<byte[]> messages() {
      return messages;
    }

    @Bean
    MessageSender sender() {
      return messages::add;
    }

    @Bean
    MessageSerializer serializer() {
      return messages -> {
        if (messages[0] instanceof User) {
          User user = ((User) messages[0]);
          return (user.username() + ":" + user.email()).getBytes();
        }
        throw new IllegalArgumentException("Expected instance of User, but was " + messages.getClass());
      };
    }
  }

}
