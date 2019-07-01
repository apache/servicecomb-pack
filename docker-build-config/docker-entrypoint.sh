#!/bin/sh

wait_for_services() {
  for svc in ${WAIT_FOR_SERVICES//,/ }
  do
    host=$(printf "%s\n" "$svc"| cut -d : -f 1)
    port=$(printf "%s\n" "$svc"| cut -d : -f 2)
    timeout=$(printf "%s\n" "$svc"| cut -d : -f 3)
    echo "Waiting for service $host:$port(timeout: $timeout seconds) ready"
    /wait-for $host:$port -t $timeout
    result=$?
    if [ $result -eq 0 ] ; then
      echo "Service $host:$port is ready"
    else
      echo "Service $host:$port is not ready"
      exit 1
    fi
  done
}

wait_for_services

exec java $JAVA_OPTS -jar /maven/saga/${project.artifactId}-${project.version}-exec.jar
