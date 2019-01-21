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

package org.apache.servicecomb.pack.omega.properties;

public class OmegaClientProperties {

  private static final long DEFAULT_RECONNECT_DELAY_MILLISECONDS = 3000;

  private static final long DEFAULT_TIMEOUT_SECONDS = 8;

  private long reconnectDelayMilliSeconds = DEFAULT_RECONNECT_DELAY_MILLISECONDS;

  private long timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

  public long getReconnectDelayMilliSeconds() {
    return reconnectDelayMilliSeconds;
  }

  public void setReconnectDelayMilliSeconds(long reconnectDelayMilliSeconds) {
    this.reconnectDelayMilliSeconds = reconnectDelayMilliSeconds;
  }

  public long getTimeoutSeconds() {
    return timeoutSeconds;
  }

  public void setTimeoutSeconds(long timeoutSeconds) {
    this.timeoutSeconds = timeoutSeconds;
  }
}

