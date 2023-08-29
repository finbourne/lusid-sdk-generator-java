# Generate SDK's from a swagger.json file.
#
#  Ensure that you set the following environment variables to an appropriate value before running
#    PACKAGE_NAME
#    PROJECT_NAME
#    ASSEMBLY_VERSION
#    PACKAGE_VERSION
#    APPLICATION_NAME
#    META_REQUEST_ID_HEADER_KEY
#    NUGET_PACKAGE_LOCATION

export APPLICATION_NAME := `echo ${APPLICATION_NAME:-lusid}`
export PACKAGE_NAME := `echo ${PACKAGE_NAME:-lusid-sdk}`
export PROJECT_NAME := `echo ${PROJECT_NAME:-lusid}`
export PACKAGE_VERSION := `echo ${PACKAGE_VERSION:-2.0.0}`
export META_REQUEST_ID_HEADER_KEY := `echo ${META_REQUEST_ID_HEADER_KEY:-lusid-meta-requestid}`
export JAVA_PACKAGE_LOCATION := `echo ${JAVA_PACKAGE_LOCATION:-~/.java/maven/local-packages}`

swagger_path := "./swagger.json"

swagger_url := "https://example.lusid.com/api/swagger/v0/swagger.json"

get-swagger:
    echo {{swagger_url}}
    curl -s {{swagger_url}} > swagger.json

build-docker-images: 
    docker build -t lusid-sdk-gen-java:latest --ssh default=$SSH_AUTH_SOCK -f Dockerfile generate

generate-local:
    envsubst < generate/config-template.json > generate/.config.json
    docker run --rm \
        -e JAVA_OPTS="-Dlog.level=error" \
        -e APPLICATION_NAME=${APPLICATION_NAME} \
        -e META_REQUEST_ID_HEADER_KEY=${META_REQUEST_ID_HEADER_KEY} \
        -e PACKAGE_VERSION=${PACKAGE_VERSION} \
        -e PACKAGE_NAME=${PACKAGE_NAME} \
        -e PROJECT_NAME=${PROJECT_NAME} \
        -v {{justfile_directory()}}/generate/:/usr/src/generate/ \
        -v {{justfile_directory()}}/generate/.openapi-generator-ignore:/usr/src/generate/.output/.openapi-generator-ignore \
        -v {{justfile_directory()}}/{{swagger_path}}:/tmp/swagger.json \
        lusid-sdk-gen-java:latest -- ./generate/generate.sh ./generate ./generate/.output /tmp/swagger.json .config.json
    rm -f generate/.output/.openapi-generator-ignore

link-tests:
    ln -s {{justfile_directory()}}/test_sdk/src/test/ {{justfile_directory()}}/generate/.output/sdk/src/test  

# for local testing - assumes maven on path, doesn't use docker to play friendly with IDEs.
test-local:
    @just generate-local
    @just link-tests
    mvn -f generate/.output/sdk verify

test:
    @just generate-local
    mkdir -p {{justfile_directory()}}/generate/.output/sdk/src/test/
    cp -R {{justfile_directory()}}/test_sdk/src/test/ {{justfile_directory()}}/generate/.output/sdk/src/test/
    docker run -it --rm \
        -e FBN_TOKEN_URL=${FBN_TOKEN_URL} \
        -e FBN_USERNAME=${FBN_USERNAME} \
        -e FBN_PASSWORD=${FBN_PASSWORD} \
        -e FBN_CLIENT_ID=${FBN_CLIENT_ID} \
        -e FBN_CLIENT_SECRET=${FBN_CLIENT_SECRET} \
        -e FBN_LUSID_API_URL=${FBN_LUSID_API_URL} \
        -e FBN_APP_NAME=${FBN_APP_NAME} \
        -e FBN_ACCESS_TOKEN=${FBN_ACCESS_TOKEN} \
        -v {{justfile_directory()}}/generate/.output/sdk:/usr/src/sdk \
        -w /usr/src/sdk \
        maven:3.8.7-openjdk-18-slim \
        /bin/bash -c "cd /usr/src/sdk && mvn clean; mvn test"
    
generate TARGET_DIR:
    @just generate-local
    
    # need to remove the created content before copying over the top of it.
    # this prevents deleted content from hanging around indefinitely.
    rm -rf {{TARGET_DIR}}/src
    rm -rf {{TARGET_DIR}}/sdk/docs
    
    mv -R generate/.output/* {{TARGET_DIR}}

# Generate an SDK from a swagger.json and copy the output to the TARGET_DIR
generate-cicd TARGET_DIR:
    mkdir -p {{TARGET_DIR}}
    mkdir -p ./generate/.output
    envsubst < generate/config-template.json > generate/.config.json
    cp ./generate/.openapi-generator-ignore ./generate/.output/.openapi-generator-ignore

    ./generate/generate.sh ./generate ./generate/.output {{swagger_path}} .config.json
    rm -f generate/.output/.openapi-generator-ignore

    # need to remove the created content before copying over the top of it.
    # this prevents deleted content from hanging around indefinitely.
    rm -rf {{TARGET_DIR}}/sdk/${APPLICATION_NAME}
    rm -rf {{TARGET_DIR}}/sdk/docs
    
    cp -R generate/.output/. {{TARGET_DIR}}
    echo "copied output to {{TARGET_DIR}}"
    ls {{TARGET_DIR}}

publish-only-local:
    mkdir -p ${JAVA_PACKAGE_LOCATION}
    docker run \
        -e APPLICATION_NAME=${APPLICATION_NAME} \
        -e META_REQUEST_ID_HEADER_KEY=${META_REQUEST_ID_HEADER_KEY} \
        -e PACKAGE_VERSION=${PACKAGE_VERSION} \
        -e PACKAGE_NAME=${PACKAGE_NAME} \
        -e PROJECT_NAME=${PROJECT_NAME} \
        -v $(pwd)/generate/.output:/usr/src \
        -v $(pwd)/publish/publish-local.sh:/usr/src/publish.sh \
        -v ${JAVA_PACKAGE_LOCATION}:/tmp/local-maven-repo \
        lusid-sdk-gen-java:latest -- "/usr/src/publish.sh"

publish-only:
    docker run \
        -e PACKAGE_VERSION=${PACKAGE_VERSION} \
        -v $(pwd)/generate/.output:/usr/src \
        -v $(pwd)/publish/publish.sh:/usr/src/publish.sh \
        lusid-sdk-gen-java:latest -- "/usr/src/publish.sh"

generate-and-publish TARGET_DIR:
    @just generate {{TARGET_DIR}}
    @just publish-only

generate-and-publish-local:
    @just generate-local
    @just publish-only-local

publish-cicd SRC_DIR:
    echo "PACKAGE_VERSION to publish: ${PACKAGE_VERSION}"
    
    mvn -e -f {{SRC_DIR}}/pom.xml test-compile compile
    mvn -f {{SRC_DIR}}/pom.xml versions:set -DnewVersion=${PACKAGE_VERSION}
    mvn -f {{SRC_DIR}}/pom.xml -s {{SRC_DIR}}/settings.xml -P$server_id clean install deploy -Dmaven.test.skip=true
