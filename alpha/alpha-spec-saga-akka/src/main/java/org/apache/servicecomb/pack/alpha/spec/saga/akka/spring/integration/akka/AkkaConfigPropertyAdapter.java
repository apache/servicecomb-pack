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

package org.apache.servicecomb.pack.alpha.spec.saga.akka.spring.integration.akka;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

public class AkkaConfigPropertyAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String PROPERTY_SOURCE_NAME = "akkaConfig.";
  public static final String REDIS_NAME = "akkaConfig.akka-persistence-redis.redis.";
  static final String AKKA_CLUSTER_SEED_NODES_KEY = "akka.cluster.seed-nodes";
  static final String AKKA_ESTENSIONS_KEY = "akka.extensions";
  static final String AKKA_LOGGERS_KEY = "akka.loggers";

  public static Map<String, Object> getPropertyMap(ConfigurableEnvironment environment) {
    final Map<String, Object> propertyMap = new HashMap<>();
    final List<String> seedNodes = new ArrayList<>();
    final List<String> extensions = new ArrayList<>();
    final List<String> loggers = new ArrayList<>();
    for (final PropertySource source : environment.getPropertySources()) {
      if (isEligiblePropertySource(source)) {
        final EnumerablePropertySource enumerable = (EnumerablePropertySource) source;
        LOG.debug("Adding properties from property source " + source.getName());
        for (final String name : enumerable.getPropertyNames()) {
          if (name.startsWith(PROPERTY_SOURCE_NAME) && !propertyMap.containsKey(name)) {
            String key = name.substring(PROPERTY_SOURCE_NAME.length());
            String value = environment.getProperty(name);
            if (key.startsWith(AKKA_CLUSTER_SEED_NODES_KEY)) {
              seedNodes.add(value);
            } else if (key.startsWith(AKKA_ESTENSIONS_KEY)) {
              extensions.add(value);
            } else if (key.startsWith(AKKA_LOGGERS_KEY)) {
              loggers.add(value);
            } else {
              if (LOG.isTraceEnabled()) {
                LOG.trace("Adding property {}={}" + key, value);
              }
              
              propertyMap.put(key, value);
              
              if(name.startsWith(REDIS_NAME) && !propertyMap.containsKey(name)){
                String readJournalKey = ("akka-persistence-redis.read-journal.redis.").concat(name.substring(REDIS_NAME.length()));
                String journalKey = ("akka-persistence-redis.journal.redis.").concat(name.substring(REDIS_NAME.length()));
                String snapshotKey = ("akka-persistence-redis.snapshot.redis.").concat(name.substring(REDIS_NAME.length()));
                propertyMap.put( readJournalKey, value);
                propertyMap.put( journalKey, value);
                propertyMap.put( snapshotKey, value);
             }
              
            }
          }
        }
      }
      propertyMap.put(AKKA_CLUSTER_SEED_NODES_KEY, seedNodes);
      propertyMap.put(AKKA_ESTENSIONS_KEY, extensions);
      propertyMap.put(AKKA_LOGGERS_KEY, loggers);
    }

    return Collections.unmodifiableMap(propertyMap);
  }

  public static boolean isEligiblePropertySource(PropertySource source) {
    // Exclude system environment properties and system property sources
    // because they are already included in the default configuration
    final String name = source.getName();
    return (source instanceof EnumerablePropertySource) &&
        !(
            name.equalsIgnoreCase(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME) ||
                name.equalsIgnoreCase(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)
        );
  }
}
