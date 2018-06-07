#!/usr/bin/env bash

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

service=saga-demo

show_usage() {
  echo "Usage: $0 {up|up-alpha|up-demo|up-mysql|down}" >&2
}

fetch_version() {
  version="$(printf 'VER\t${project.version}' | mvn help:evaluate | grep '^VER' | cut -f2)"
}

if [[ -z $1 ]]; then
  show_usage
  exit 2
fi

case $1 in
  up)
    fetch_version
    echo "Starting ${service}:${version}"
    TAG=$version docker-compose up
    exit $?
  ;;

  up-alpha)
    fetch_version
    echo "Starting ${service}:${version}"
    TAG=$version docker-compose -f docker-compose-alpha.yaml up
    exit $?
  ;;

  up-demo)
    fetch_version
    echo "Starting ${service}:${version}"
    TAG=$version docker-compose -f docker-compose-demo.yaml up
    exit $?
  ;;

  up-mysql)
    fetch_version
    echo "Starting ${service}:${version}"
    TAG=$version docker-compose -f docker-compose.yaml -f docker-compose.mysql.yaml up
    exit $?
  ;;
  
  down)
    fetch_version
    echo "Stopping ${service}:${version}"
    TAG=$version docker-compose down
    exit $?
  ;;
  
  *)
    show_usage
    exit 2
  ;;
esac

