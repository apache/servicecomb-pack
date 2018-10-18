#!/usr/bin/env bash
## ---------------------------------------------------------------------------
## Licensed to the Apache Software Foundation (ASF) under one or more
## contributor license agreements.  See the NOTICE file distributed with
## this work for additional information regarding copyright ownership.
## The ASF licenses this file to You under the Apache License, Version 2.0
## (the "License"); you may not use this file except in compliance with
## the License.  You may obtain a copy of the License at
##
##      http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing, software
## distributed under the License is distributed on an "AS IS" BASIS,
## WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
## See the License for the specific language governing permissions and
## limitations under the License.
## ---------------------------------------------------------------------------
#bin/sh
echo "$TRAVIS_EVENT_TYPE"
if [[ "$TRAVIS_EVENT_TYPE" == "cron" ]]
then
  echo "Don't do anything here for the cron job!"
else
  echo "Running the unit tests and integration tests here!"
  mvn clean install -Pjacoco -Pdocker -P${SPRING_BOOT_PROFILE} coveralls:report
  mvn clean verify -f saga-demo -Pdemo -Pdocker -P${SPRING_BOOT_PROFILE} -Ddocker.useColor=false -Ddocker.showLogs
  mvn clean verify -f acceptance-tests -Pdemo -Pdocker -P${SPRING_BOOT_PROFILE} -Ddocker.useColor=false -Ddocker.showLogs
fi
