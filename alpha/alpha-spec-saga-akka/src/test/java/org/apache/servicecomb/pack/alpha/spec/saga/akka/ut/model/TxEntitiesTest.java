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

package org.apache.servicecomb.pack.alpha.spec.saga.akka.ut.model;

import static org.junit.Assert.assertTrue;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.model.TxEntities;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.model.TxEntity;
import org.junit.Test;

public class TxEntitiesTest {

  @Test
  public void testForEachReverse(){
    TxEntity t1 = TxEntity.builder().beginTime(new Date(System.currentTimeMillis())).build();
    TxEntity t2 = TxEntity.builder().beginTime(new Date(System.currentTimeMillis()+10)).build();
    TxEntity t3 = TxEntity.builder().beginTime(t2.getBeginTime()).build(); // beginTime same t2
    TxEntity t4 = TxEntity.builder().beginTime(new Date(System.currentTimeMillis()+20)).build();
    List<TxEntity> entities = Lists.newArrayList(t4, t3, t2, t1);
    // t1,t2,t3,t4
    TxEntities txEntities = new TxEntities();
    ListIterator<TxEntity> iterator = entities.listIterator(entities.size());
    while(iterator.hasPrevious()){
      entities.forEach(t->txEntities.put(UUID.randomUUID().toString(),iterator.previous()));
    }
    // t4,t3,t2,t1
    List<TxEntity> reverseEntities = new ArrayList();
    txEntities.forEachReverse((k,v)->{
      reverseEntities.add(v);
    });
    assertTrue(Iterables.elementsEqual(entities, reverseEntities));
  }
}