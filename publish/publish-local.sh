#!/bin/bash -e

mvn -e -f sdk/pom.xml test-compile compile

mvn -e -f sdk/pom.xml package

find . -name "*.jar"

mvn org.apache.maven.plugins:maven-install-plugin:3.1.1:install-file  \
  -Dfile=sdk/target/${PACKAGE_NAME}-${PACKAGE_VERSION}.jar \
  -DgroupId=com.finbourne \
  -DartifactId=${PACKAGE_NAME} \
  -Dversion=${PACKAGE_VERSION} \
  -Dpackaging=jar \
  -DlocalRepositoryPath=/tmp/local-maven-repo

ls -tla /tmp/local-maven-repo