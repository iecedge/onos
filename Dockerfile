# First stage is the build environment
FROM debian:stretch as builder

RUN apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates wget python maven git curl automake bison flex g++ git libboost-all-dev libevent-dev libtool make pkg-config thrift-compiler patch software-properties-common gnupg build-essential gperf ruby perl libsqlite3-dev libfontconfig1-dev libicu-dev libfreetype6 libpng-dev libjpeg-dev libx11-dev libxext-dev zip libssl1.0-dev unzip automake ninja-build golang

RUN apt-get update && \
    add-apt-repository ppa:webupd8team/java -y && \
    apt-get update && \
    echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 select true" | debconf-set-selections && \
    apt-get install oracle-java8-installer oracle-java8-set-default -y --allow-unauthenticated

RUN git clone git://github.com/ariya/phantomjs.git \
    && cd phantomjs \
    && git checkout 2.1 \
    && git submodule init \
    && git submodule update \
    && cd /phantomjs \
    && python ./build.py --qt-config "-I /usr/include/openssl-1.0/ -L/usr/lib/openssl-1.0/" \
    && apt-get install -y libssl-dev nodejs phantomjs procps

RUN cd / \
    && export PROTOC_VERSION=3.0.2 \
    && git clone https://github.com/google/protobuf.git \
    && cd protobuf \
    && git checkout v$PROTOC_VERSION \
    && ./autogen.sh \
    && ./configure \
    && make \
    && make install \
    && export LD_LIBRARY_PATH=/usr/local/lib:$LD_LIBRARY_PATH \
    && echo /usr/local/lib >> /etc/ld.so.conf \
    && ldconfig

COPY . /onos

RUN export QT_QPA_PLATFORM=minimal \
    && export ONOS_ROOT=/onos \
    && sed -i 's/jar -xf/unzip/g' /onos/tools/test/bin/onos-stage-apps \
    && cd /onos \
    && /onos/tools/build/onos-buck build onos --show-output -v 7


# Second stage is the runtime environment
FROM debian:stretch

RUN apt-get update \
  && apt-get install -y curl software-properties-common \
    && add-apt-repository ppa:webupd8team/java -y \
    && apt-get update \
    && echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 select true" | debconf-set-selections \
    && apt-get install oracle-java8-installer oracle-java8-set-default -y --allow-unauthenticated \
    && mkdir -p /onos

# Install ONOS
COPY --from=builder /onos/buck-out/gen/tools/package/onos-package/onos.tar.gz .

# Configure ONOS to log to stdout
RUN tar zxvf onos.tar.gz -C /onos/ \
    && rm onos.tar.gz
    # && sed -ibak '/log4j.rootLogger=/s/$/, stdout/' $(ls -d apache-karaf-*)/etc/org.ops4j.pax.logging.cfg \


# Ports
# 6653 - OpenFlow
# 6640 - OVSDB
# 8181 - GUI
# 8101 - ONOS CLI
# 9876 - ONOS intra-cluster communication
EXPOSE 6653 6640 8181 8101 9876

# Get ready to run command
WORKDIR /onos/onos-1.11.2
ENTRYPOINT ["./bin/onos-service"]
CMD ["server"]