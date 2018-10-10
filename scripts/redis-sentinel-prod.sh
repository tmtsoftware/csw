#!/usr/bin/env bash
#
# Starts redis sentinel required by CSW and registers them with the location service.
# This script uses the csw-location-agent app to start Redis Sentinel and register it with the Location Service
#
#    csw-services.sh start    :to start redis sentinel and register it to location service as EventServer and AlarmServer
#    csw-services.sh stop     :to stop redis sentinel and unregister it from the location service
#

# Setting default values
sentinel_port=26379

script_name=$0

logDir=/tmp/csw/logs
test -d ${logDir} || mkdir -p ${logDir}

# We need at least this version of Redis
minRedisVersion=4.0
redisSentinel=/usr/local/bin/redis-sentinel
redisClient=`echo ${redisSentinel} | sed -e 's/-sentinel/-cli/'`

sentinelLogFile=${logDir}/sentinel.log
sentinelPidFile=${logDir}/sentinel.pid
sentinelPortFile=${logDir}/sentinel.port

sentinelConf="./conf/redis-sentinel/sentinel.conf"

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

function start_sentinel() {
    if checkIfRedisIsInstalled ; then
        echo "Starting Redis Sentinel on port: [$sentinel_port] ..."
        nohup ./csw-location-agent -DclusterSeeds=${clusterSeeds} --name "EventServer,AlarmServer" --command "$redisSentinel ${sentinelConf} --port ${sentinel_port}" --port "${sentinel_port}"> ${sentinelLogFile} 2>&1 &
        echo $! > ${sentinelPidFile}
        echo ${sentinel_port} > ${sentinelPortFile}
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

             start_sentinel

             echo "================================================================="
             echo "All the logs are stored at location: [$logDir]"
             echo "================================================================="
            ;;
        stop)
            # Stop Redis
            if [ ! -f ${sentinelPidFile} ]
            then
                echo "Redis Sentinel $sentinelPidFile does not exist, process is not running."
            else
                local PID=$(cat ${sentinelPidFile})
                local sentinelPort=$(cat ${sentinelPortFile})
                echo "Stopping Redis Sentinel..."
                ${redisClient} -p ${sentinelPort} shutdown
                while [ -x /proc/${PID} ]
                do
                    echo "Waiting for Redis Sentinel to shutdown ..."
                    sleep 1
                done
                echo "Redis Sentinel stopped."
                rm -f ${sentinelLogFile} ${sentinelPidFile} ${sentinelPortFile}
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