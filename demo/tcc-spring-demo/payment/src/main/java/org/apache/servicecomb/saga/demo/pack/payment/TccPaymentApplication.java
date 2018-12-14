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

package org.apache.servicecomb.saga.demo.pack.payment;

import org.apache.servicecomb.saga.omega.spring.EnableOmega;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
@Profile("prd")
@EnableOmega
public class TccPaymentApplication {
  public static void main(String[] args) {
    SpringApplication.run(TccPaymentApplication.class, args);
  }

  @Bean
  CommandLineRunner kickOff(AccountDao accountDao) {
    return args -> {
      // Set up the account information
      accountDao.save(new Account("UserA", 100, 90));
      accountDao.save(new Account("UserB", 10, 10));
      accountDao.save(new Account("UserC", 1, 1));
    };
  }
}
