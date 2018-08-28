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

package org.apache.servicecomb.saga.core;

import java.util.Collection;

public class CompositeSagaResponse implements SagaResponse {
  private final Collection<SagaResponse> responses;

  public CompositeSagaResponse(Collection<SagaResponse> responses) {
    this.responses = responses;
  }

  @Override
  public boolean succeeded() {
    if (responses.size() > 0) {
      boolean result = true;
      for (SagaResponse response : responses) {
        result = result && response.succeeded();
      }
      return result;
    } else {
      return false;
    }
  }

  @Override
  public String body() {
    StringBuffer result = new StringBuffer();
    if (responses.size() == 0) {
      result.append("{}");
    } else {
      result.append("[");
      for (SagaResponse response : responses) {
        result.append(response.body());
        result.append(", ");
      }
      result.delete(result.length()-2, result.length());
      result.append("]");
    }
    return result.toString();
  }


  public Collection<SagaResponse> responses() {
    return responses;
  }
}
