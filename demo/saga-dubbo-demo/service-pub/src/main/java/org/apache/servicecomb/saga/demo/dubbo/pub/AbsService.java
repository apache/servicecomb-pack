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
package org.apache.servicecomb.saga.demo.dubbo.pub;

import org.springframework.jdbc.core.JdbcTemplate;

public abstract class AbsService {

  @javax.annotation.Resource(name = "jdbcTemplate")
  protected JdbcTemplate jdbcTemplate;

  public abstract String getServiceName();

  public abstract String getTableName();

  protected void doRunBusi() {
    this.jdbcTemplate
        .update(String.format("update %s set vstatus=? where service = ?", getTableName()), "run", getServiceName());
  }

  protected void doCancelBusi() {
    this.jdbcTemplate
        .update(String.format("update %s set vstatus=? where service = ?", getTableName()), "cancel", getServiceName());
  }
}
