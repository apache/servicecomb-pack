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

package org.apache.servicecomb.saga.omega.format;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.servicecomb.saga.omega.transaction.TxEvent;
import org.apache.servicecomb.saga.omega.transaction.MessageDeserializer;
import org.apache.servicecomb.saga.omega.transaction.MessageSerializer;
import org.apache.servicecomb.saga.omega.transaction.OmegaException;

public class NativeMessageFormat implements MessageSerializer, MessageDeserializer {
  @Override
  public byte[] serialize(TxEvent event) {
    try {
      return serialize(event.payloads());
    } catch (OmegaException e) {
      throw new OmegaException("Unable to serialize event with global tx id " + event.globalTxId(), e);
    }
  }

  @Override
  public byte[] serialize(Object[] objects) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      try (ObjectOutputStream outputStream = new ObjectOutputStream(out)) {
        outputStream.writeObject(objects);
        return out.toByteArray();
      }
    } catch (IOException e) {
      throw new OmegaException("Unable to serialize object", e);
    }
  }

  @Override
  public Object[] deserialize(byte[] message) {
    try (ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(message))) {
      return (Object[]) inputStream.readObject();
    } catch (IOException | ClassNotFoundException e) {
      throw new OmegaException("Unable to deserialize message", e);
    }
  }
}
