#!/bin/bash -e

cd sdk

javadoc=$(which javadoc)
mvn clean
mvn test -DjavadocExecutable=${javadoc}
mvn package -DjavadocExecutable=${javadoc} -Dmaven.test.skip=true

find . -name "*.jar"

mvn org.apache.maven.plugins:maven-install-plugin:3.1.1:install-file  -X \
  -Dfile=target/${PACKAGE_NAME}-${PACKAGE_VERSION}.jar \
  -DgroupId=com.finbourne \
  -DartifactId=${PACKAGE_NAME} \
  -Dversion=${PACKAGE_VERSION} \
  -Dpackaging=jar \
  -DlocalRepositoryPath=/tmp/local-maven-repo 

ls -tla /tmp/local-maven-repo