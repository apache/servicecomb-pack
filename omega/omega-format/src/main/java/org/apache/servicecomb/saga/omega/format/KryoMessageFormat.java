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

import org.apache.servicecomb.saga.omega.transaction.OmegaException;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;

public class KryoMessageFormat implements MessageFormat {

  private static final int DEFAULT_BUFFER_SIZE = 4096;

  private static final KryoFactory factory = new KryoFactory() {
    @Override
    public Kryo create() {
      return new Kryo();
    }
  };

  private static final KryoPool pool = new KryoPool.Builder(factory).softReferences().build();

  @Override
  public byte[] serialize(Object[] objects) {
    Output output = new Output(DEFAULT_BUFFER_SIZE, -1);

    Kryo kryo = pool.borrow();
    kryo.writeObjectOrNull(output, objects, Object[].class);
    pool.release(kryo);

    return output.toBytes();
  }

  @Override
  public Object[] deserialize(byte[] message) {
    try {
      Input input = new Input(new ByteArrayInputStream(message));

      Kryo kryo = pool.borrow();
      Object[] objects = kryo.readObjectOrNull(input, Object[].class);
      pool.release(kryo);

      return objects;
    } catch (KryoException e) {
      throw new OmegaException("Unable to deserialize message", e);
    }
  }
}
