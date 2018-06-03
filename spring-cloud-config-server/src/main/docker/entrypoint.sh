#!/usr/bin/env bash

set -e

if [ -z "${GIT_HOST}" ]; then echo "GIT_HOST not defined."; exit 1; fi
if [ -z "${GIT_SSH_PORT}" ]; then GIT_SSH_PORT="22"; fi
# ping ${GIT_HOST} -c 1
if [ ! -z "${GIT_HOST}" ]; then echo "wait ${GIT_HOST}:${GIT_SSH_PORT} for 30 seconds."; waitforit -address=tcp://${GIT_HOST}:${GIT_SSH_PORT} -timeout=30; fi
# rabbit mq is password protected, so try http management port
if [ ! -z "${SPRING_RABBITMQ_HOST}" ] && [ ! -z "${SPRING_RABBITMQ_PORT}" ]; then echo "wait ${SPRING_RABBITMQ_HOST}:1${SPRING_RABBITMQ_PORT} for 30 seconds."; waitforit -address==tcp://${SPRING_RABBITMQ_HOST}:1${SPRING_RABBITMQ_PORT} -timeout=30; fi


JAVA_OPTS="${JAVA_OPTS} -Djava.security.egd=file:/dev/urandom";
JAVA_OPTS="${JAVA_OPTS} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}";
. /opt/java_debug_monitor_profiler.sh

# if command starts with an option, prepend java
if [ "${1:0:1}" == '-' ]; then
    set -- java "$@" -jar *-exec.jar
elif [ "${1:0:1}" != '/' ]; then
    set -- java ${JAVA_OPTS} -jar *-exec.jar "$@"
fi

exec "$@"
