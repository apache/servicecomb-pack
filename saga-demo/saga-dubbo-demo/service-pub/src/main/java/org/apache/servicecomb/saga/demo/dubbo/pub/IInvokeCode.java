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

/**
 * a invoke code describe a test scenario
* @date 05/05/2018 3:27 PM
*/
public interface IInvokeCode {

    public static final String Ab_description="A->B, success";
    public static final String Ab = "Ab";
    public static final String AExceptionWhenAb_description="A->B, A.run exception after A call B.run";
    public static final String AExceptionWhenAb="AExceptionWhenAb";
    public static final String BExceptionWhenAb_description="A->B, B.run exception";
    public static final String BExceptionWhenAb="BExceptionWhenAb";
    public static final String AbAc_description="A->B, A->C success";
    public static final String AbAc="AbAc";
    public static final String CExceptionWhenAbAc_description="A->B,A->C, C.run exception";
    public static final String CExceptionWhenAbAc="CExceptionWhenAbAc";
    public static final String AbBc_description="A->B, B->C, success";
    public static final String AbBc="AbBc";
    public static final String CExceptionWhenAbBc_description="A->B, B->C, C.run exception";
    public static final String CExceptionWhenAbBc="CExceptionWhenAbBc";
}
