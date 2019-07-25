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

package org.apache.servicecomb.pack.omega.transaction;

import org.apache.servicecomb.pack.omega.context.OmegaContext;
import org.apache.servicecomb.pack.omega.context.TransactionContext;
import org.apache.servicecomb.pack.omega.context.TransactionContextProperties;
import org.slf4j.Logger;

public abstract class TransactionContextHelper {

  protected TransactionContext extractTransactionContext(Object[] args) {
    if (args != null && args.length > 0) {
      for (Object arg : args) {
        // check the TransactionContext first
        if (arg instanceof TransactionContext) {
          return (TransactionContext) arg;
        }
        if (arg instanceof TransactionContextProperties) {
          TransactionContextProperties transactionContextProperties = (TransactionContextProperties) arg;
          return new TransactionContext(transactionContextProperties.getGlobalTxId(),
              transactionContextProperties.getLocalTxId());
        }
      }
    }
    return null;
  }

  protected void populateOmegaContext(OmegaContext context, TransactionContext transactionContext) {
    if (context.globalTxId() != null) {
      getLogger()
          .warn("The context {}'s globalTxId is not empty. Update it for globalTxId:{} and localTxId:{}", context,
              transactionContext.globalTxId(), transactionContext.localTxId());
    } else {
      getLogger()
          .debug("Updated context {} for globalTxId:{} and localTxId:{}", context,
              transactionContext.globalTxId(), transactionContext.localTxId());
    }
    context.setGlobalTxId(transactionContext.globalTxId());
    context.setLocalTxId(transactionContext.localTxId());
  }

  protected abstract Logger getLogger();
}
