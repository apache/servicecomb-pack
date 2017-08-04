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

import java.io.PrintWriter;
import java.io.StringWriter;

public class FailedSagaResponse implements SagaResponse {

  private final int statusCode;
  private final String cause;
  private final String content;

  public FailedSagaResponse(int statusCode, String cause, String content) {
    this.statusCode = statusCode;
    this.cause = cause;
    this.content = content;
  }

  public FailedSagaResponse(String cause, Throwable e) {
    this.statusCode = 0;
    this.cause = cause;
    this.content = stackTrace(e);
  }

  @Override
  public boolean succeeded() {
    return false;
  }

  @Override
  public String body() {
    return String.format("{\n"
        + "  \"statusCode\": %d,\n"
        + "  \"cause\": \"%s\",\n"
        + "  \"content\": \"%s\"\n"
        + "}", statusCode, cause, content);
  }

  private String stackTrace(Throwable e) {
    StringWriter writer = new StringWriter();
    e.printStackTrace(new PrintWriter(writer));
    return writer.toString();
  }
}
