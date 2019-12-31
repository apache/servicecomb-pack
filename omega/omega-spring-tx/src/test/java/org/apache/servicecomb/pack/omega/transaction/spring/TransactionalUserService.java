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

package org.apache.servicecomb.pack.omega.transaction.spring;

import org.apache.servicecomb.pack.omega.transaction.annotations.Compensable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TransactionalUserService {
  static final String ILLEGAL_USER = "Illegal User";
  private final UserRepository userRepository;

  private int count = 0;

  @Autowired
  TransactionalUserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  void resetCount() {
    this.count = 0;
  }

  @Compensable(compensationMethod = "delete")
  public User add(User user) {
    if (ILLEGAL_USER.equals(user.username())) {
      throw new IllegalArgumentException("User is illegal");
    }
    return userRepository.save(user);
  }

  public void delete(User user) {
    userRepository.delete(user);
  }

  @Compensable(forwardRetries = 2, compensationMethod = "delete")
  public User add(User user, int count) {
    if (this.count < count) {
      this.count += 1;
      throw new IllegalStateException("Retry harder");
    }
    resetCount();
    return userRepository.save(user);
  }

  public void delete(User user, int count) {
    resetCount();
    userRepository.delete(user);
  }

  @Compensable(compensationMethod = "deleteWithTransactional")
  public User addWithTransactional(User user) {
    if (ILLEGAL_USER.equals(user.username())) {
      throw new IllegalArgumentException("User is illegal");
    }
    return userRepository.save(user);
  }

  // This method is failed on purpose which could rollback the call
  public void deleteWithTransactional(User user) {
    userRepository.delete(user);
    throw new RuntimeException("saga compensation rollback test");
  }
}
