/*
 * Copyright 2017 Huawei Technologies Co., Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.servicecomb.saga.core;

public class SuccessfulSagaResponse implements SagaResponse {
  private final String body;

  public SuccessfulSagaResponse(int statusCode, String content) {
    this.body = format(statusCode, content);
  }

  public SuccessfulSagaResponse(String body) {
    this.body = body;
  }

  @Override
  public boolean succeeded() {
    return true;
  }

  @Override
  public String body() {
    return body;
  }

  private String format(int statusCode, String content) {
    return String.format("{\n"
        + "  \"statusCode\": %d,\n"
        + "  \"content\": \"%s\"\n"
        + "}", statusCode, content);
  }
}
