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
 */
public interface IInvokeCode {
  String Ab_description = "A->B, success";
  String Ab = "Ab";
  String AExceptionWhenAb_description = "A->B, A.run exception after A call B.run";
  String AExceptionWhenAb = "AExceptionWhenAb";
  String BExceptionWhenAb_description = "A->B, B.run exception";
  String BExceptionWhenAb = "BExceptionWhenAb";
  String AbAc_description = "A->B, A->C success";
  String AbAc = "AbAc";
  String CExceptionWhenAbAc_description = "A->B,A->C, C.run exception";
  String CExceptionWhenAbAc = "CExceptionWhenAbAc";
  String AbBc_description = "A->B, B->C, success";
  String AbBc = "AbBc";
  String CExceptionWhenAbBc_description = "A->B, B->C, C.run exception";
  String CExceptionWhenAbBc = "CExceptionWhenAbBc";
}
