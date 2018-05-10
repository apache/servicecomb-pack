/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicecomb.saga.omega.transport.dubbo;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.config.spring.extension.SpringExtensionFactory;
import com.alibaba.dubbo.rpc.*;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static org.apache.servicecomb.saga.omega.context.OmegaContext.GLOBAL_TX_ID_KEY;
import static org.apache.servicecomb.saga.omega.context.OmegaContext.LOCAL_TX_ID_KEY;

/**
 * get saga transaction id from dubbo invocation and set into omega context
*/
@Activate(group = Constants.PROVIDER)
public class SagaDubboProviderFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // TODO not sure if it's a good way to look up OmegaContext during every invoke
        OmegaContext omegaContext = new SpringExtensionFactory().getExtension(OmegaContext.class, "omegaContext");
        if (omegaContext != null) {
          String globalTxId = invocation.getAttachment(GLOBAL_TX_ID_KEY);
          if (globalTxId == null) {
            LOG.info("no such omega context global id: {}", GLOBAL_TX_ID_KEY);
          } else {
            omegaContext.setGlobalTxId(globalTxId);
            omegaContext.setLocalTxId(invocation.getAttachment(LOCAL_TX_ID_KEY));
            LOG.info("Added {} {} and {} {} to omegaContext", new Object[] {GLOBAL_TX_ID_KEY, omegaContext.globalTxId(),
                LOCAL_TX_ID_KEY, omegaContext.localTxId()});
          }
          invocation.getAttachments().put(GLOBAL_TX_ID_KEY, null);
          invocation.getAttachments().put(LOCAL_TX_ID_KEY, null);
        }

        if(invoker != null){
            return invoker.invoke(invocation);
        }
        return null;
    }
}
