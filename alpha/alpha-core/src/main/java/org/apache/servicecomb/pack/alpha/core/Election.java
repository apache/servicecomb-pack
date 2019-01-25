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

package org.apache.servicecomb.pack.alpha.core;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "Election")
public class Election {

    @Id
    private String name;
    private Date lock_until;
    private Date locked_at;
    private String locked_by;

    public Election() {

    }

    public Election(Election election) {
        this(election.name,
                election.lock_until,
                election.locked_at,
                election.locked_by);
    }

    public Election(
            String name,
            Date lock_until,
            Date locked_at,
            String locked_by) {
        this.name = name;
        this.lock_until = lock_until;
        this.locked_at = locked_at;
        this.locked_by = locked_by;
    }

    public String getName() {
        return name;
    }

    public Date getLock_until() {
        return lock_until;
    }

    public Date getLocked_at() {
        return locked_at;
    }

    public String getLocked_by() {
        return locked_by;
    }

    @Override
    public String toString() {
        return "Election{" +
                "name='" + name + '\'' +
                ", lock_until=" + lock_until +
                ", locked_at=" + locked_at +
                ", locked_by='" + locked_by + '\'' +
                '}';
    }
}
