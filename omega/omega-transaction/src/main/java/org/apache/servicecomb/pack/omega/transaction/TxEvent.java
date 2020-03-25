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

package org.apache.servicecomb.pack.omega.transaction;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import org.apache.servicecomb.pack.common.Environment;
import org.apache.servicecomb.pack.common.EventType;

public class TxEvent {
  private final long timestamp;
  private final EventType type;
  private final String globalTxId;
  private final String localTxId;
  private final String parentTxId;
  private final String compensationMethod;
  private final int timeout;
  private final Object[] payloads;

  private final String retryMethod;
  private final int forwardRetries;
  private final int forwardTimeout;
  private final int reverseRetries;
  private final int reverseTimeout;
  private final int retryDelayInMilliseconds;

  public TxEvent(EventType type, String globalTxId, String localTxId, String parentTxId,
      String compensationMethod,
      int timeout, String retryMethod, int forwardRetries, int forwardTimeout, int reverseRetries,
      int reverseTimeout, int retryDelayInMilliseconds, Object... payloads) {
    this.timestamp = System.currentTimeMillis();
    this.type = type;
    this.globalTxId = globalTxId;
    this.localTxId = localTxId;
    this.parentTxId = parentTxId;
    this.compensationMethod = compensationMethod;
    this.timeout = timeout;
    this.retryMethod = retryMethod;
    this.forwardRetries = forwardRetries;
    this.forwardTimeout = forwardTimeout;
    this.reverseRetries = reverseRetries;
    this.reverseTimeout = reverseTimeout;
    this.retryDelayInMilliseconds = retryDelayInMilliseconds;
    this.payloads = payloads;
  }

  public long timestamp() {
    return timestamp;
  }

  public String globalTxId() {
    return globalTxId;
  }

  public String localTxId() {
    return localTxId;
  }

  public String parentTxId() {
    return parentTxId;
  }

  public Object[] payloads() {
    return payloads;
  }

  public EventType type() {
    return type;
  }

  public String compensationMethod() {
    return compensationMethod;
  }

  public int timeout() {
    return timeout;
  }

  public String retryMethod() {
    return retryMethod;
  }

  public int forwardRetries() {
    return forwardRetries;
  }

  public int forwardTimeout() {
    return forwardTimeout;
  }

  public int reverseRetries() {
    return reverseRetries;
  }

  public int reverseTimeout() {
    return reverseTimeout;
  }

  public int retryDelayInMilliseconds() {
    return retryDelayInMilliseconds;
  }

  @Override
  public String toString() {
    return type.name() + "{" +
        "globalTxId='" + globalTxId + '\'' +
        ", localTxId='" + localTxId + '\'' +
        ", parentTxId='" + parentTxId + '\'' +
        ", compensationMethod='" + compensationMethod + '\'' +
        ", timeout=" + timeout + '\'' +
        ", retryMethod='" + retryMethod + '\'' +
        ", forwardRetries=" + forwardRetries + '\'' +
        ", forwardTimeout=" + forwardTimeout + '\'' +
        ", reverseRetries=" + reverseRetries + '\'' +
        ", reverseTimeout=" + reverseTimeout + '\'' +
        ", payloads=" + Arrays.toString(payloads) +
        '}';
  }

  protected static String stackTrace(Throwable e) {
    StringWriter writer = new StringWriter();
    e.printStackTrace(new PrintWriter(writer));
    String stackTrace = writer.toString();
    if (stackTrace.length() > Environment.getInstance().getPayloadsMaxLength()) {
      stackTrace = stackTrace.substring(0, Environment.getInstance().getPayloadsMaxLength());
    }
    return stackTrace;
  }
}
