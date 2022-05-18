#!/bin/bash

if [ "$JAVA_HOME" == "" ]; then
  echo "[E] JAVA_HOME environment variable not defined, aborting!"
  exit 1;
fi

MVN_VERSION=$(mvn --version)
if [ -z "$MVN_VERSION" ]; then
  echo "[E] Maven Installation not found, aborting!"
  exit 1;
fi


mvn clean package install dependency:copy-dependencies

java -cp "target/classes:target/dependency/*" Main --missing --dump

#python3 src/main/python/util.py