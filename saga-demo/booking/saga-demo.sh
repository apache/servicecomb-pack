#!/usr/bin/env bash

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

