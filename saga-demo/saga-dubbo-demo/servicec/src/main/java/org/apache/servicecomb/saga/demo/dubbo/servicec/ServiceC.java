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
package org.apache.servicecomb.saga.demo.dubbo.servicec;

import org.apache.servicecomb.saga.demo.dubbo.api.IServiceC;
import org.apache.servicecomb.saga.demo.dubbo.pub.AbsService;
import org.apache.servicecomb.saga.demo.dubbo.pub.InvokeContext;
import org.apache.servicecomb.saga.omega.transaction.annotations.Compensable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.MethodHandles;

public class ServiceC extends AbsService implements IServiceC {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public String getServiceName() {
        return "servicec";
    }

    @Override
    public String getTableName() {
        return "testc";
    }

    @Override
    @Compensable(compensationMethod="cancelRun")
    @Transactional(rollbackFor = Exception.class)
    public Object run(InvokeContext invokeContext) throws Exception {
        LOG.info("C.run called");
        doRunBusi();
        if(invokeContext.isException(getServiceName()) ){
            LOG.info("C.run exception");
            throw new Exception("C.run exception");
        }
        return null;
    }

    public void cancelRun(InvokeContext invokeContext){
        LOG.info("C.cancel called");
        doCancelBusi();
    }

}
