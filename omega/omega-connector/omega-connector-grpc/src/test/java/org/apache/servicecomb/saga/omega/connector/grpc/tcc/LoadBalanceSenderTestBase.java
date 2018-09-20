/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.servicecomb.saga.omega.connector.grpc.tcc;

import io.grpc.Server;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class LoadBalanceSenderTestBase {

  protected static final int[] ports = {8080, 8090};

  protected static final Map<Integer, Server> servers = new HashMap<>();

  protected static final Map<Integer, Integer> delays = new HashMap<Integer, Integer>() {{
    put(8080, 0);
    put(8090, 800);
  }};

  protected static final Map<Integer, Queue<String>> connected = new HashMap<Integer, Queue<String>>() {{
    put(8080, new ConcurrentLinkedQueue<String>());
    put(8090, new ConcurrentLinkedQueue<String>());
  }};

  protected static final Map<Integer, Queue<Object>> eventsMap = new HashMap<Integer, Queue<Object>>() {{
    put(8080, new ConcurrentLinkedQueue<>());
    put(8090, new ConcurrentLinkedQueue<>());
  }};
}
