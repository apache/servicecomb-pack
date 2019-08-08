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

package org.apache.servicecomb.pack.alpha.ui.vo;

public class SystemInfoDTO {
  private String upTime;
  private int cpus;
  private float systemLoad;
  private long threadsLive;
  private long threadsDaemon;
  private long threadsPeak;
  private long gcCount;
  private float gcTime;

  public String getUpTime() {
    return upTime;
  }

  public void setUpTime(String upTime) {
    this.upTime = upTime;
  }

  public int getCpus() {
    return cpus;
  }

  public void setCpus(int cpus) {
    this.cpus = cpus;
  }

  public float getSystemLoad() {
    return systemLoad;
  }

  public void setSystemLoad(float systemLoad) {
    this.systemLoad = systemLoad;
  }

  public long getThreadsLive() {
    return threadsLive;
  }

  public void setThreadsLive(long threadsLive) {
    this.threadsLive = threadsLive;
  }

  public long getThreadsDaemon() {
    return threadsDaemon;
  }

  public void setThreadsDaemon(long threadsDaemon) {
    this.threadsDaemon = threadsDaemon;
  }

  public long getThreadsPeak() {
    return threadsPeak;
  }

  public void setThreadsPeak(long threadsPeak) {
    this.threadsPeak = threadsPeak;
  }

  public long getGcCount() {
    return gcCount;
  }

  public void setGcCount(long gcCount) {
    this.gcCount = gcCount;
  }

  public float getGcTime() {
    return gcTime;
  }

  public void setGcTime(float gcTime) {
    this.gcTime = gcTime;
  }
}
