FROM gradle:jdk11

COPY . /usr/local/src/

WORKDIR /usr/local/src

RUN gradle setup
RUN ./gradlew installRelayer
ENTRYPOINT ["/usr/local/src/dist/build/install/tuweni-relayer/bin/hobbits-relayer"]
