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

package org.apache.servicecomb.saga.omega.transaction.tcc;

import java.util.HashMap;
import java.util.Map;

public class DefaultParametersContext implements ParametersContext {
  private Map<String, Object[]> parameters = new HashMap<>();

  @Override
  public Object[] getParameters(String localTransactionId) {
    return parameters.get(localTransactionId);
  }

  @Override
  public void putParamters(String localTransactionId, Object ... paramters) {
    parameters.put(localTransactionId, paramters);
  }

  @Override
  public void removeParameter(String localTransactionId) {
    parameters.remove(localTransactionId);
  }
}
