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

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.apache.servicecomb.pack.common.EventType;
import org.apache.servicecomb.pack.omega.transaction.SagaMessageSender;
import org.apache.servicecomb.pack.omega.transaction.TxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.security.SecureRandom;

@Component
public class SagaEventBenchmark {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final static SecureRandom SECURERANDOM = new SecureRandom();

  @Autowired(required = false)
  SagaMessageSender sender;
  int warmUpConcurrency;
  int warmUpRequests;
  BenchmarkMetrics metrics;

  public void send(int warmUpConcurrency, int requests, int concurrency) {
    this.warmUpConcurrency = warmUpConcurrency;
    this.warmUpRequests = 10;
    System.out.print("Benchmarking ");

    // 预热
    if (warmUpConcurrency > 0) {
      metrics = new BenchmarkMetrics();
      this.warmUp(warmUpConcurrency);
    }

    // 压测
    metrics = new BenchmarkMetrics();
    metrics.setRequests(requests);
    metrics.setConcurrency(concurrency);
    long s = System.currentTimeMillis();
    CountDownLatch begin = new CountDownLatch(1);
    CountDownLatch end = new CountDownLatch(concurrency);
    begin.countDown();
    String[] id_prefixs = generateRandomIdPrefix(concurrency);
    for (int i = 0; i < concurrency; i++) {
      Execute execute = new Execute(sender, id_prefixs[i],requests / concurrency, begin, end);
      new Thread(execute).start();
    }
    try {
      end.await();
      long e = System.currentTimeMillis();
      metrics.setTimeTaken(e - s);
      System.out.println("\n");

      System.out.println(String.format("%-25s %s", "Warm Up", warmUpRequests * warmUpConcurrency));
      System.out.println(String.format("%-25s %s", "Concurrency Level", metrics.getConcurrency()));
      System.out.println(
          String.format("%-25s %s", "Time taken for tests", metrics.getTimeTaken() + " seconds"));
      System.out
          .println(String.format("%-25s %s", "Complete requests", metrics.getCompleteRequests()));
      System.out.println(String.format("%-25s %s", "Failed requests", metrics.getFailedRequests()));
      System.out.println(String
          .format("%-25s %s", "Requests per second", metrics.getRequestsPerSecond() + " [#/sec]"));
      System.out.println(
          String.format("%-25s %s", "Time per request", metrics.getTimePerRequest() + " [ms]"));
      System.out.println();
      System.out.println("Percentage of the requests served within a certain time (ms)");

      int size = metrics.getTransactionTime().size();
      int percentage = 50;
      for (int i = 0; i <= 5; i++) {
        float peek = size * ((float) percentage / 100);
        System.out.println(String.format("%-5s %.2f", percentage + "%", getAverage(
            metrics.getTransactionTime().subList(0, (int) peek)).getAsDouble()));
        percentage = percentage + 10;
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOG.error(e.getMessage(), e);
    }
    LOG.info("OK");
  }

  private void warmUp(int warmUp) {
    CountDownLatch begin = new CountDownLatch(1);
    CountDownLatch end = new CountDownLatch(warmUp);
    begin.countDown();

    // 预热
    if (warmUp > 0) {
      for (int i = 0; i < warmUp; i++) {
        String id_prefix = "warmup-";
        Execute execute = new Execute(sender, id_prefix, warmUpRequests, begin, end);
        new Thread(execute).start();
      }
      try {
        end.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LOG.error("warmUp Exception = [{}]", e.getMessage(), e);
      }
    }

  }

  private OptionalDouble getAverage(List<Long> times) {
    try {
      return times.stream().mapToLong(Long::longValue).average();
    } catch (Exception e) {

      throw e;
    }
  }

  private class Execute implements Runnable {
    String id_prefix;
    SagaMessageSender sender;
    CountDownLatch begin;
    CountDownLatch end;
    int requests;

    public Execute(SagaMessageSender sender, String id_prefix, int requests, CountDownLatch begin,
        CountDownLatch end) {
      this.sender = sender;
      this.id_prefix = id_prefix;
      this.requests = requests;
      this.begin = begin;
      this.end = end;
    }

    @Override
    public void run() {
      try {
        begin.await();
        for (int i = 0; i < requests; i++) {
          metrics.completeRequestsIncrement();
          long s = System.currentTimeMillis();
          final String globalTxId = id_prefix + "-" + i;
          final String localTxId_1 = UUID.randomUUID().toString();
          final String localTxId_2 = UUID.randomUUID().toString();
          final String localTxId_3 = UUID.randomUUID().toString();
          try {
            sagaSuccessfulEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream()
                .forEach(event -> {
                  if(LOG.isDebugEnabled()){
                    LOG.debug(event.toString());
                  }
                  sender.send(event);
                });
          } catch (Throwable e) {
            metrics.failedRequestsIncrement();
          } finally {
            long e = System.currentTimeMillis();
            metrics.addTransactionTime(e - s);
          }
        }
        end.countDown();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LOG.error(e.getMessage(), e);
      }
    }
  }

  public List<TxEvent> sagaSuccessfulEvents(String globalTxId, String localTxId_1,
      String localTxId_2, String localTxId_3) {
    List<TxEvent> sagaEvents = new ArrayList<>();
    sagaEvents.add(
        new TxEvent(EventType.SagaStartedEvent, globalTxId, globalTxId, globalTxId, "", 0, null,
            0, 0, 0, 0, 0));
    sagaEvents.add(
        new TxEvent(EventType.TxStartedEvent, globalTxId, localTxId_1, globalTxId, "service a", 0,
            null, 0, 0, 0, 0, 0));
    sagaEvents.add(
        new TxEvent(EventType.TxEndedEvent, globalTxId, localTxId_1, globalTxId, "service a", 0,
            null, 0, 0, 0, 0, 0));
    sagaEvents.add(
        new TxEvent(EventType.TxStartedEvent, globalTxId, localTxId_2, globalTxId, "service b", 0,
            null, 0, 0, 0, 0, 0));
    sagaEvents.add(
        new TxEvent(EventType.TxEndedEvent, globalTxId, localTxId_2, globalTxId, "service b", 0,
            null, 0, 0, 0, 0, 0));
    sagaEvents.add(
        new TxEvent(EventType.TxStartedEvent, globalTxId, localTxId_3, globalTxId, "service c", 0,
            null, 0, 0, 0, 0, 0));
    sagaEvents.add(
        new TxEvent(EventType.TxEndedEvent, globalTxId, localTxId_3, globalTxId, "service c", 0,
            null, 0, 0, 0, 0, 0));
    sagaEvents.add(
        new TxEvent(EventType.SagaEndedEvent, globalTxId, globalTxId, globalTxId, "", 0, null, 0, 0, 0, 0, 0));
    return sagaEvents;
  }

  private String[] generateRandomIdPrefix(int numberOfWords) {
    String[] randomStrings = new String[numberOfWords];
    for (int i = 0; i < numberOfWords; i++) {
      char[] word = new char[8];
      for (int j = 0; j < word.length; j++) {
        word[j] = (char) ('a' + SECURERANDOM.nextInt(26));
      }
      randomStrings[i] = new String(word);
    }
    return randomStrings;
  }
}
