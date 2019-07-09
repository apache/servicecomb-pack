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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BenchmarkMetrics {

  private int requests;
  private AtomicInteger completeRequests = new AtomicInteger();
  private AtomicInteger failedRequests = new AtomicInteger();
  private int concurrency;
  private long timeTaken;
  private List<Long> transactionTime = Collections.synchronizedList(new ArrayList());

  private int beforeProgress;

  public void setRequests(int requests) {
    this.requests = requests;
  }

  //并发用户数
  public int getConcurrency() {
    return concurrency;
  }

  public void setConcurrency(int concurrency) {
    this.concurrency = concurrency;
  }

  public void addTransactionTime(long time) {
    transactionTime.add(time);
  }

  public long getTimeTaken() {
    return timeTaken / 1000;
  }

  public void setTimeTaken(long timeTaken) {
    this.timeTaken = timeTaken;
  }

  public int getCompleteRequests() {
    return completeRequests.get();
  }

  public int getFailedRequests() {
    return failedRequests.get();
  }

  //总请求数量
  public void completeRequestsIncrement() {
    completeRequests.incrementAndGet();
    printProgress();
  }

  //失败的请求数量
  public void failedRequestsIncrement() {
    failedRequests.incrementAndGet();
    printProgress();
  }

  //吞吐率
  public long getRequestsPerSecond() {
    return this.completeRequests.get() / (timeTaken / 1000);
  }

  //用户平均请求等待时间
  public long getTimePerRequest() {
    return this.timeTaken / (completeRequests.get() / concurrency);
  }

  public List<Long> getTransactionTime() {
    return transactionTime;
  }

  private void printProgress() {
    int progress = (int) ((float) completeRequests.get() / (float) requests * 100);
    if (beforeProgress != progress) {
      System.out.print("░");
      beforeProgress = progress;
    }
  }
}
