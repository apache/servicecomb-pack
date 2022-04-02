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
package org.apache.servicecomb.pack.alpha.spec.saga.akka.channel.redis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

public class MessageSerializer {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static MessageSerializerImpl serializer = null;

  public MessageSerializer() {
    serializer = new MessageSerializerImpl();
  }

  public Optional<byte[]> serializer(Object data) {
    return Optional.ofNullable(serializer.serialize(data));
  }

  public Optional<Object> deserialize(byte[] bytes) {
    return Optional.ofNullable(serializer.deserialize(bytes));
  }

  private class MessageSerializerImpl implements RedisSerializer<Object> {

    @Override
    public byte[] serialize(Object data) throws SerializationException {
      try {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayOutputStream);
        outputStream.writeObject(data);

        byte[] bytes = byteArrayOutputStream.toByteArray();

        outputStream.close();

        return bytes;
      } catch (Exception e) {
        LOG.error("serialize Exception = [{}]", e.getMessage(), e);
      }

      return null;
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
      try {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

        Object object = objectInputStream.readObject();

        objectInputStream.close();

        return object;
      } catch (Exception e) {
        LOG.error("deserialize Exception = [{}]", e.getMessage(), e);
      }

      return null;
    }
  }
}
