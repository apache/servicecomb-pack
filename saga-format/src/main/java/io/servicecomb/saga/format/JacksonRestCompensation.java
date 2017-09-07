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

package io.servicecomb.saga.format;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.servicecomb.saga.core.Compensation;
import java.util.Map;

public class JacksonRestCompensation extends JacksonRestOperation implements Compensation {

  private final int retries;

  public JacksonRestCompensation(
    String path,
    String method,
    Map<String, Map<String, String>> params) {
    this(DEFAULT_RETRIES, path, method, params);
  }

  @JsonCreator
  public JacksonRestCompensation(
      @JsonProperty("retries") int retries,
      @JsonProperty("path") String path,
      @JsonProperty("method") String method,
      @JsonProperty("params") Map<String, Map<String, String>> params) {
    super(path, method, params);
    this.retries = retries <= 0? DEFAULT_RETRIES : retries;
  }

  @Override
  public int retries() {
    return retries;
  }
}
