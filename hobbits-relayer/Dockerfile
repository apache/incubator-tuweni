FROM gradle:jdk11

RUN apt-get update && apt-get install -y netcat

COPY . /usr/local/src/

WORKDIR /usr/local/src

RUN gradle setup
RUN ./gradlew installRelayer
ENTRYPOINT ["/usr/local/src/dist/build/install/tuweni-relayer/bin/hobbits-relayer"]
