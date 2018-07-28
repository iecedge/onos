export ONOS_VERSION="$(grep ONOS_VERSION onos.defs | xargs)"
export IMAGE_TAG="${ONOS_VERSION#*= }"
export AARCH=`uname -m`
docker build -t cachengo/onos-$AARCH:$IMAGE_TAG .
docker push cachengo/onos-$AARCH:$IMAGE_TAG