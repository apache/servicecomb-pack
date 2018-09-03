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

package org.apache.servicecomb.saga.format;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.servicecomb.saga.core.Operation;
import org.apache.servicecomb.saga.core.SagaDefinition;
import org.apache.servicecomb.saga.core.SagaException;
import org.apache.servicecomb.saga.core.SagaRequest;
import org.apache.servicecomb.saga.core.SagaResponse;
import org.apache.servicecomb.saga.core.SuccessfulSagaResponse;
import org.apache.servicecomb.saga.core.application.interpreter.FromJsonFormat;
import org.apache.servicecomb.saga.transports.SQLTransport;
import org.apache.servicecomb.saga.transports.TransportFactory;
import org.junit.Test;

import com.seanyinx.github.unit.scaffolding.AssertUtils;

public class JacksonFromJsonFormatForSQLTest {
  private static final String requestJson = "{\n"
      + "    \"requests\":[\n"
      + "        {\n"
      + "            \"id\":\"first-sql-sharding-1\",\n"
      + "            \"type\":\"sql\",\n"
      + "            \"datasource\":\"ds_0\",\n"
      + "            \"parents\":[],\n"
      + "            \"transaction\":{\n"
      + "                \"sql\":\"INSERT INTO TABLE ds_0.tb_0 (id, value) values (?, ?)\",\n"
      + "                \"params\":[[\"1\", \"xxx\"]]\n"
      + "            },\n"
      + "            \"compensation\":{\n"
      + "                \"sql\":\"DELETE FROM ds_0.tb_0 WHERE id=?\",\n"
      + "                \"params\":[[\"1\"]]\n"
      + "            }\n"
      + "        },\n"
      + "        {\n"
      + "            \"id\":\"first-sql-sharding-2\",\n"
      + "            \"type\":\"sql\",\n"
      + "            \"datasource\":\"ds_0\",\n"
      + "            \"parents\":[],\n"
      + "            \"transaction\":{\n"
      + "                \"sql\":\"INSERT INTO TABLE ds_0.tb_1 (id, value) values (?, ?)\",\n"
      + "                \"params\":[[\"2\", \"xxx\"]]\n"
      + "            },\n"
      + "            \"compensation\":{\n"
      + "                \"sql\":\"DELETE FROM ds_0.tb_1 WHERE id=?\",\n"
      + "                \"params\":[[\"2\"]]\n"
      + "            }\n"
      + "        },\n"
      + "        {\n"
      + "            \"id\":\"second-sql-sharding-1\",\n"
      + "            \"type\":\"sql\",\n"
      + "            \"datasource\":\"ds_1\",\n"
      + "            \"parents\":[\"first-sql-sharding-1\",\"first-sql-sharding-2\"],\n"
      + "            \"transaction\":{\n"
      + "                \"sql\":\"INSERT INTO TABLE ds_1.tb_2 (id, value) values (?, ?)\",\n"
      + "                \"params\":[[\"3\", \"xxx\"]]\n"
      + "            },\n"
      + "            \"compensation\":{\n"
      + "                \"sql\":\"DELETE FROM ds_1.tb_2 WHERE id=?\",\n"
      + "                \"params\":[[\"3\"]]\n"
      + "            }\n"
      + "        },\n"
      + "        {\n"
      + "            \"id\":\"second-sql-sharding-2\",\n"
      + "            \"type\":\"sql\",\n"
      + "            \"datasource\":\"ds_1\",\n"
      + "            \"parents\":[\"first-sql-sharding-1\",\"first-sql-sharding-2\"],\n"
      + "            \"transaction\":{\n"
      + "                \"sql\":\"INSERT INTO TABLE ds_1.tb_3 (id, value) values (?, ?)\",\n"
      + "                \"params\":[[\"4\", \"xxx\"]]\n"
      + "            },\n"
      + "            \"compensation\":{\n"
      + "                \"sql\":\"DELETE FROM ds_1.tb_3 WHERE id=?\",\n"
      + "                \"params\":[[\"4\"]]\n"
      + "            }\n"
      + "        }\n"
      + "    ]\n"
      + "}\n";

  private final SagaResponse responseDefault = new SuccessfulSagaResponse(uniquify("responseDefault"));

  private final SQLTransport sqlTransport = new SQLTransport() {
    @Override
    public SagaResponse with(String datasource, String sql, List<List<Object>> params) {
      if (null == sql || sql.trim().length() == 0) {
        return responseDefault;
      }

      for (List<Object> each : params) {
        for (Object param : each) {
          sql = sql.replaceFirst("\\?", param.toString());
        }
      }

      return new SuccessfulSagaResponse(datasource + " execute sql : " + sql);
    }
  };

  private final TransportFactory<SQLTransport> transportFactory = new TransportFactory<SQLTransport>() {
    @Override
    public SQLTransport getTransport() {
      return sqlTransport;
    }
  };

  private final FromJsonFormat<SagaDefinition> format = new JacksonFromJsonFormat(transportFactory);

  private final Function<SagaRequest, String> getRequestId = new Function<SagaRequest, String>() {
    @Override
    public String apply(SagaRequest sagaRequest) {
      return sagaRequest.id();
    }
  };

  private final Function<SagaRequest, String> getRequestServiceName = new Function<SagaRequest, String>() {
    @Override
    public String apply(SagaRequest sagaRequest) {
      return sagaRequest.serviceName();
    }
  };

  private final Function<SagaRequest, String> getRequestType = new Function<SagaRequest, String>() {
    @Override
    public String apply(SagaRequest sagaRequest) {
      return sagaRequest.type();
    }
  };

  @Test
  public void addTransportToDeserializedRequests() {
    SagaRequest[] requests = format.fromJson(requestJson).requests();

    assertThat(collect(requests, getRequestId),
        contains("first-sql-sharding-1", "first-sql-sharding-2", "second-sql-sharding-1", "second-sql-sharding-2"));
    assertThat(collect(requests, getRequestServiceName), contains("ds_0", "ds_0", "ds_1", "ds_1"));
    assertThat(collect(requests, getRequestType),
        contains(Operation.TYPE_SQL, Operation.TYPE_SQL, Operation.TYPE_SQL, Operation.TYPE_SQL));

    SagaResponse sagaResponse = null;

    sagaResponse = requests[0].transaction().send(requests[0].serviceName());
    assertThat(sagaResponse.body(), is("ds_0 execute sql : INSERT INTO TABLE ds_0.tb_0 (id, value) values (1, xxx)"));
    sagaResponse = requests[0].compensation().send(requests[0].serviceName());
    assertThat(sagaResponse.body(), is("ds_0 execute sql : DELETE FROM ds_0.tb_0 WHERE id=1"));

    sagaResponse = requests[1].transaction().send(requests[1].serviceName());
    assertThat(sagaResponse.body(), is("ds_0 execute sql : INSERT INTO TABLE ds_0.tb_1 (id, value) values (2, xxx)"));
    sagaResponse = requests[1].compensation().send(requests[1].serviceName());
    assertThat(sagaResponse.body(), is("ds_0 execute sql : DELETE FROM ds_0.tb_1 WHERE id=2"));

    sagaResponse = requests[2].transaction().send(requests[2].serviceName());
    assertThat(sagaResponse.body(), is("ds_1 execute sql : INSERT INTO TABLE ds_1.tb_2 (id, value) values (3, xxx)"));
    sagaResponse = requests[2].compensation().send(requests[2].serviceName());
    assertThat(sagaResponse.body(), is("ds_1 execute sql : DELETE FROM ds_1.tb_2 WHERE id=3"));

    sagaResponse = requests[3].transaction().send(requests[3].serviceName());
    assertThat(sagaResponse.body(), is("ds_1 execute sql : INSERT INTO TABLE ds_1.tb_3 (id, value) values (4, xxx)"));
    sagaResponse = requests[3].compensation().send(requests[3].serviceName());
    assertThat(sagaResponse.body(), is("ds_1 execute sql : DELETE FROM ds_1.tb_3 WHERE id=4"));

    assertArrayEquals(new String[] {"first-sql-sharding-1", "first-sql-sharding-2"}, requests[2].parents());
    assertArrayEquals(new String[] {"first-sql-sharding-1", "first-sql-sharding-2"}, requests[3].parents());
  }

  @Test
  public void blowsUpWhenJsonIsInvalid() throws IOException {
    String invalidRequest = "invalid-json";

    try {
      format.fromJson(invalidRequest);
      AssertUtils.expectFailing(SagaException.class);
    } catch (SagaException e) {
      assertThat(e.getMessage(), is("Failed to interpret JSON invalid-json"));
    }
  }

  private <T> Collection<T> collect(SagaRequest[] requests, Function<SagaRequest, T> mapper) {
    List<T> result = new LinkedList<T>();
    for (SagaRequest request : requests) {
      result.add(mapper.apply(request));
    }
    return result;
  }

  private interface Function<T, R> {
    R apply(T t);
  }
}
