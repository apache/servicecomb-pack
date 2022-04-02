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

package org.apache.servicecomb.pack.alpha.benchmark;

import org.apache.servicecomb.pack.omega.transaction.SagaMessageSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application implements CommandLineRunner {

  @Autowired
  SagaEventBenchmark sagaEventBenchmark;

  @Autowired(required = false)
  SagaMessageSender sender;

  @Value("${alpha.cluster.address}")
  String address;

  @Value("${n:0}")
  int requests;

  @Value("${c:1}")
  int concurrency;

  @Value("${w:0}")
  int warmUp;

  public static void main(String[] args) {
    boolean hasAlphaAddress = false;
    for(String arg : args){
      if(arg.startsWith("--alpha.cluster.address")){
        hasAlphaAddress = true;
      }
    }
    if(!hasAlphaAddress){
      printHelp();
      System.exit(0);
    }
    System.setProperty("omega.spec.names","saga");
    SpringApplication.run(Application.class, args);
  }

  @Override
  public void run(String... args) {

    try {
      if (checkParamter()) {
        sagaEventBenchmark.send(warmUp,requests, concurrency);
      } else {
        printHelp();
      }
    } finally {
      try{
        sender.onDisconnected();
      }catch (Throwable e){
        //
      }
      System.exit(0);
    }

  }

  private boolean checkParamter() {
    if (address == null) {
      return false;
    } else {
      if (requests == 0) {
        return false;
      } else {
        return true;
      }
    }
  }

  private static void printHelp() {
    System.out.println("\nalpha-benchmark: wrong number of arguments");
    System.out.println(
        "Usage: java -jar alpha-benchmark-0.5.0-SNAPSHOT-exec.jar --alpha.cluster.address=0.0.0.0:8080 --n=1000 --c=1");
    System.out.println("Options are:");
    System.out.println(
        String.format("%-5s %-15s %-25s", "  --n", "requests", "Number of requests to perform"));
    System.out.println(String.format("%-5s %-15s %-25s", "  --c", "concurrency",
        "Number of multiple requests to make at a time"));
    System.out.println(String.format("%-5s %-15s %-25s", "  --w", "warm-up",
        "Number of multiple requests warm-Up, w * 10"));
  }
}
