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
package org.apache.servicecomb.pack.alpha.spec.saga.akka.properties;

import java.util.Map;

public class ChannelKafkaProperties {
  private String topic;
  private String bootstrapServers;
  private int numPartitions;
  private short replicationFactor;
  private Map<String,String> consumer;
  private Map<String,String> producer;

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public String getBootstrapServers() {
    return bootstrapServers;
  }

  public void setBootstrapServers(String bootstrapServers) {
    this.bootstrapServers = bootstrapServers;
  }

  public int getNumPartitions() {
    return numPartitions;
  }

  public void setNumPartitions(int numPartitions) {
    this.numPartitions = numPartitions;
  }

  public short getReplicationFactor() {
    return replicationFactor;
  }

  public void setReplicationFactor(short replicationFactor) {
    this.replicationFactor = replicationFactor;
  }

  public Map<String, String> getConsumer() {
    return consumer;
  }

  public void setConsumer(Map<String, String> consumer) {
    this.consumer = consumer;
  }

  public Map<String, String> getProducer() {
    return producer;
  }

  public void setProducer(Map<String, String> producer) {
    this.producer = producer;
  }
}