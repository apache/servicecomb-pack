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

package org.apache.servicecomb.pack.alpha.fsm.repository.channel;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.servicecomb.pack.alpha.fsm.metrics.MetricsService;
import org.apache.servicecomb.pack.alpha.fsm.repository.AbstractTransactionRepositoryChannel;
import org.apache.servicecomb.pack.alpha.core.fsm.repository.model.GlobalTransaction;
import org.apache.servicecomb.pack.alpha.fsm.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemoryTransactionRepositoryChannel extends AbstractTransactionRepositoryChannel {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final LinkedBlockingQueue<GlobalTransaction> globalTransactionQueue;
  private int size;

  public MemoryTransactionRepositoryChannel(TransactionRepository repository, int size,
      MetricsService metricsService) {
    super(repository, metricsService);
    this.size = size > 0 ? size : Integer.MAX_VALUE;
    globalTransactionQueue = new LinkedBlockingQueue(this.size);
    new Thread(new GlobalTransactionConsumer(), "MemoryTransactionRepositoryChannel").start();
  }

  @Override
  public void sendTo(GlobalTransaction transaction) {
    try {
      globalTransactionQueue.put(transaction);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  class GlobalTransactionConsumer implements Runnable {

    @Override
    public void run() {
      while (true) {
        try {
          GlobalTransaction transaction = globalTransactionQueue.peek();
          if (transaction != null) {
            repository.send(transaction);
            globalTransactionQueue.poll();
          } else {
            Thread.sleep(10);
          }
        } catch (Exception ex) {
          LOG.error(ex.getMessage(), ex);
        }
      }
    }
  }
}
