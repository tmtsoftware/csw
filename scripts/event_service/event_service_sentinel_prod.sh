#!/usr/bin/env bash
#
# Starts event service required by CSW and registers them with the location service.
# This script uses the csw-location-agent app to start Event Service and register it with the Location Service
#
#    csw-services.sh start    :to start event service and register it to location service
#    csw-services.sh stop     :to stop event service and unregister it from the location service
#

# Setting default values
event_port=26379

script_name=$0

logDir=/tmp/csw-prod/logs
test -d ${logDir} || mkdir -p ${logDir}

# We need at least this version of Redis
minRedisVersion=3.2.5
redisSentinel=/usr/local/bin/redis-sentinel
redisClient=`echo ${redisSentinel} | sed -e 's/-sentinel/-cli/'`

eventLogFile=${logDir}/event.log
eventPidFile=${logDir}/event.pid
eventPortFile=${logDir}/event.port

sortVersion="sort -V"

# Make sure we have the min redis version
function get_version {
    test "$(printf '%s\n' "$@" | ${sortVersion} | head -n 1)" != "$1";
}

function checkIfRedisIsInstalled {
    # Look in the default location first, since installing from the source puts it there, otherwise look in the path
    if test ! -x ${redisSentinel} ; then redisSentinel=redis-server ; fi
    if ! type ${redisSentinel} &> /dev/null; then
      echo "[ERROR] Can't find $redisSentinel. Please install Redis version [$minRedisVersion] or greater."
      return 1
    else
        redis_version=`${redisSentinel} --version | awk '{sub(/-.*/,"",$3);print $3}' | sed -e 's/v=//'`
        if get_version ${minRedisVersion} ${redis_version}; then
             echo "[ERROR] Required Redis version is [$minRedisVersion], but only version [$redis_version] was found"
             return 1
        else
            return 0
        fi
    fi
}

function start_event() {
    if checkIfRedisIsInstalled ; then
        echo "[EVENT] Starting Event Service on port: [$event_port] ..."
        nohup ./csw-location-agent -DclusterSeeds=${clusterSeeds} --name "EventServer" --command "$redisSentinel ../conf/sentinel.conf --port ${event_port}" --port "${event_port}"> ${eventLogFile} 2>&1 &
        echo $! > ${eventPidFile}
        echo ${event_port} > ${eventPortFile}
    else
        exit 1
    fi
}

function usage {
    echo
    echo -e "usage: $script_name COMMAND \n"

    echo "Commands:"
    echo "  start      Starts all csw services if no options provided"
    echo "  stop       Stops all csw services, it does not take any arguments"
    exit 1
}

function parse_cmd_args {

    case "$1" in
        start)
            shift

             start_event

             echo "================================================================="
             echo "All the logs are stored at location: [$logDir]"
             echo "================================================================="
            ;;
        stop)
            # Stop Redis
            if [ ! -f ${eventPidFile} ]
            then
                echo "[EVENT] Event $eventPidFile does not exist, process is not running."
            else
                local PID=$(cat ${eventPidFile})
                local redisPort=$(cat ${eventPortFile})
                echo "[EVENT] Stopping Event Service..."
                ${redisClient} -p ${redisPort} shutdown
                while [ -x /proc/${PID} ]
                do
                    echo "[EVENT] Waiting for Event Service to shutdown ..."
                    sleep 1
                done
                echo "[EVENT] Event Service stopped."
                rm -f ${eventLogFile} ${eventPidFile} ${eventPortFile}
            fi
            ;;
        *)
            echo "[ERROR] Please use start or stop as first argument, find usage below: "
            usage
            ;;
    esac
}

# Parse command line arguments
parse_cmd_args "$@"