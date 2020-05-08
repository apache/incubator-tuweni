FROM gradle:6.3-jdk11

RUN apt-get update && apt-get install -y libsodium-dev && apt-get clean