#!/bin/bash -e

if [[ (${#1} -eq 0) ]] ; then
    echo 
    echo "[ERROR] missing Maven user password"
    exit 1
fi

if [[ (${#2} -eq 0) ]] ; then
    echo
    echo "[ERROR] missing Maven server id"
    exit 1
fi

user_password=$1
server_id=$2

mvn -e -f sdk/pom.xml test-compile compile

cat > ./sdk/settings.xml <<EOF
<settings>
  <servers>
    <server>
      <id>$server_id</id>
      <username>maven-user</username>
      <password>$user_password</password>
    </server>
  </servers>
</settings>
EOF

sdk_version=$(cat lusid.json | jq -r '.info.version')

mvn -f sdk/pom.xml versions:set -DnewVersion=$sdk_version
mvn -f sdk/pom.xml -s sdk/settings.xml -P$server_id clean install deploy -Dmaven.test.skip=true

rm -f publish.sh