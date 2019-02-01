export ONOS_VERSION=$(grep ONOS_VERSION onos.defs | xargs)
export IMAGE_TAG="${ONOS_VERSION#*= }"

docker manifest create --amend cachengo/onos:$IMAGE_TAG cachengo/onos-x86_64:$IMAGE_TAG cachengo/onos-aarch64:$IMAGE_TAG
docker manifest push cachengo/onos:$IMAGE_TAG
