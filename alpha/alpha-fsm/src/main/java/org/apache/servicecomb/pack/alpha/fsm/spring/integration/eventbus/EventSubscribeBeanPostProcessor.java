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

package org.apache.servicecomb.pack.alpha.fsm.spring.integration.eventbus;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class EventSubscribeBeanPostProcessor implements BeanPostProcessor {

  @Autowired(required = false)
  @Qualifier("sagaEventBus")
  EventBus eventBus;

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName)
      throws BeansException {
    return bean;
  }

  /**
   * If the spring bean's method defines @Subscribe, then register the spring bean into the Guava
   * Event
   */
  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName)
      throws BeansException {
    if(eventBus !=null){
      Method[] methods = bean.getClass().getMethods();
      for (Method method : methods) {
        Annotation[] annotations = method.getAnnotations();
        for (Annotation annotation : annotations) {
          if (annotation.annotationType().equals(Subscribe.class)) {
            eventBus.register(bean);
            return bean;
          }
        }
      }
    }
    return bean;
  }
}