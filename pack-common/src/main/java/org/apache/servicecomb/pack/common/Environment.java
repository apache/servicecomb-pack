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

package org.apache.servicecomb.pack.common;

import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Environment {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static Environment instance = new Environment();
  private static final int PAYLOADS_MAX_LENGTH = 10240;
  private int payloadsMaxLength = 0;

  public Environment() {
    if (payloadsMaxLength == 0) {
      String val = System.getenv("PAYLOADS_MAX_LENGTH");
      if (val == null || val.trim().length() == 0) {
        payloadsMaxLength = PAYLOADS_MAX_LENGTH;
      } else {
        try {
          payloadsMaxLength = Integer.parseInt(val);
        } catch (NumberFormatException ex) {
          payloadsMaxLength = PAYLOADS_MAX_LENGTH;
          LOG.error(
              "Failed to parse environment variable PAYLOADS_MAX_LENGTH={}, use default value {}",
              val, PAYLOADS_MAX_LENGTH);
        }
      }
    }
  }

  public static Environment getInstance(){
    return instance;
  }

  public int getPayloadsMaxLength() {
    return this.payloadsMaxLength;
  }
}
