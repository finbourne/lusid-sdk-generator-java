FROM maven:3.6.3-jdk-11 as maven

RUN apt-get update && apt-get install -y jq

RUN mkdir -p /usr/src/
WORKDIR /usr/src/

ENTRYPOINT [ "/bin/bash"]