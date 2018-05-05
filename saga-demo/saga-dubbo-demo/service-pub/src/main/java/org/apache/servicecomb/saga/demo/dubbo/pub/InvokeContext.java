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
package org.apache.servicecomb.saga.demo.dubbo.pub;

public class InvokeContext implements java.io.Serializable{

    private String invokeCode;

    public String getInvokeCode() {
        return invokeCode;
    }

    public InvokeContext setInvokeCode(String invokeCode) {
        this.invokeCode = invokeCode;
        return this;
    }

    public boolean isInvokeB(String serviceName) {
        return "servicea".equals(serviceName) && (IInvokeCode.AExceptionWhenAb.equals(invokeCode)
                || IInvokeCode.Ab.equals(invokeCode)
                || IInvokeCode.BExceptionWhenAb.equals(invokeCode)
                || IInvokeCode.AbAc.equals(invokeCode)
                || IInvokeCode.CExceptionWhenAbAc.equals(invokeCode)
                || IInvokeCode.AbBc.equals(invokeCode)
                || IInvokeCode.CExceptionWhenAbBc.equals(invokeCode));
    }

    public boolean isInvokeC(String serviceName) {
        return ("serviceb".equals(serviceName) && IInvokeCode.AbBc.equals(invokeCode))
                || ("serviceb".equals(serviceName) && IInvokeCode.CExceptionWhenAbBc.equals(invokeCode))
                || ("servicea".equals(serviceName) && IInvokeCode.AbAc.equals(invokeCode))
                || ("servicea".equals(serviceName) && IInvokeCode.CExceptionWhenAbAc.equals(invokeCode));
    }

    public boolean isException(String serviceName) {
        return ("servicea".equals(serviceName) && IInvokeCode.AExceptionWhenAb.equals(invokeCode))
               || ("serviceb".equals(serviceName) && IInvokeCode.BExceptionWhenAb.equals(invokeCode))
                || ("servicec".equals(serviceName) && (IInvokeCode.CExceptionWhenAbAc.equals(invokeCode)
                || IInvokeCode.CExceptionWhenAbBc.equals(invokeCode)));
    }

}
