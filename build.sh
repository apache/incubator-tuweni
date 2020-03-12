#!/bin/bash

COMMAND=$1
if [ -z "$COMMAND" ];then
  COMMAND="build"
fi
docker run -it -v `pwd`:/home/gradle gradle:5.0-jdk11 gradle --no-daemon -Dorg.gradle.jvmargs="-Xmx2g -XX:MaxMetaspaceSize=512m" $COMMAND