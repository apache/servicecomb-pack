/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.servicecomb.saga.omega.transaction.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.annotations.Participate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class TccUserService {
  static final String ILLEGAL_USER = "Illegal User";
  private final UserRepository userRepository;

  @Autowired
  TccUserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Participate(confirmMethod = "confirm", cancelMethod = "cancel")
  User add(User user) {
    // Only for the validation check
    if (ILLEGAL_USER.equals(user.username())) {
      throw new IllegalArgumentException("User is illegal");
    }
    return userRepository.save(user);
  }

  void confirm(User user) {
    User result = userRepository.findByUsername(user.username());
    // Just make sure we can get the resource and keep doing other business
    assertThat(result, is(user));
  }

  void cancel(User user) {
    userRepository.delete(user);
  }


}
