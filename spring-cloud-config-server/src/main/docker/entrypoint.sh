#!/usr/bin/env bash

set -e

if [ -z "${GIT_PREFIX}" ]; then echo "GIT_PREFIX not defined."; exit 1; fi
if [[ "${GIT_PREFIX}" == https* ]]; then
#https://<host>:<port>/<group>/<project>.git
#https://<host>/<group>/<project>.git
    GIT_HOST_PORT=$(echo ${GIT_PREFIX} | awk -F/ '{print $3}')
    GIT_HOST=$(echo ${GIT_HOST_PORT} | awk -F: '{print $1}')
    GIT_PORT=$(echo ${GIT_HOST_PORT} | awk -F: '{print $2}')
    if [ -z "${GIT_PORT}" ]; then GIT_PORT="443"; fi
elif [[ "${GIT_PREFIX}" == http* ]]; then
#http://<host>:<port>/<group>/<project>.git
#http://<host>/<group>/<project>.git
    GIT_HOST_PORT=$(echo ${GIT_PREFIX} | awk -F/ '{print $3}')
    GIT_HOST=$(echo ${GIT_HOST_PORT} | awk -F: '{print $1}')
    GIT_PORT=$(echo ${GIT_HOST_PORT} | awk -F: '{print $2}')
    if [ -z "${GIT_PORT}" ]; then GIT_PORT="80"; fi
elif [[ "${GIT_PREFIX}" == ssh* ]]; then
#ssh://git@<host>:<port>/<group>/<project>.git
    GIT_HOST_PORT=$(echo ${GIT_PREFIX} | awk -F/ '{print $3}' | awk -F@ '{print $2}')
    GIT_HOST=$(echo ${GIT_HOST_PORT} | awk -F: '{print $1}')
    GIT_PORT=$(echo ${GIT_HOST_PORT} | awk -F: '{print $2}')
    if [ -z "${GIT_PORT}" ]; then GIT_PORT="22"; fi
else
#git@<host>:<group>/<project>.git
    GIT_HOST=$(echo ${GIT_PREFIX} | awk -F@ '{print $2}' | awk -F: '{print $1}')
    GIT_PORT="22"
fi
# ping ${GIT_HOST} -c 1
echo "wait ${GIT_HOST}:${GIT_PORT} for 30 seconds."; waitforit -address=tcp://${GIT_HOST}:${GIT_PORT} -timeout=30;


# rabbit mq is password protected, so try http management port
if [ ! -z "${SPRING_RABBITMQ_HOST}" ] && [ ! -z "${SPRING_RABBITMQ_PORT}" ]; then echo "wait ${SPRING_RABBITMQ_HOST}:1${SPRING_RABBITMQ_PORT} for 30 seconds."; waitforit -address==tcp://${SPRING_RABBITMQ_HOST}:1${SPRING_RABBITMQ_PORT} -timeout=30; fi


JAVA_OPTS="${JAVA_OPTS} -Djava.security.egd=file:/dev/urandom";
if [ -n "${SPRING_PROFILES_ACTIVE}" ]; then JAVA_OPTS="${JAVA_OPTS} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}"; fi
. /opt/java_debug_monitor_profiler.sh

# if command starts with an option, prepend java
if [ "${1:0:1}" == '-' ]; then
    set -- java "$@" -jar *-exec.jar
elif [ "${1:0:1}" != '/' ]; then
    set -- java ${JAVA_OPTS} -jar *-exec.jar "$@"
fi

exec "$@"
