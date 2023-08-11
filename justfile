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
    mkdir -p /tmp/${PROJECT_NAME}_${PACKAGE_VERSION}
    envsubst < generate/config-template.json > generate/.config.json
    docker run \
        -e JAVA_OPTS="-Dlog.level=error" \
        -e APPLICATION_NAME=${APPLICATION_NAME} \
        -e META_REQUEST_ID_HEADER_KEY=${META_REQUEST_ID_HEADER_KEY} \
        -e PACKAGE_VERSION=${PACKAGE_VERSION} \
        -e PACKAGE_NAME=${PACKAGE_NAME} \
        -e PROJECT_NAME=${PROJECT_NAME} \
        -v $(pwd)/generate/:/usr/src/generate/ \
        -v $(pwd)/generate/.openapi-generator-ignore:/usr/src/generate/.output/.openapi-generator-ignore \
        -v $(pwd)/{{swagger_path}}:/tmp/swagger.json \
        lusid-sdk-gen-java:latest -- ./generate/generate.sh ./generate ./generate/.output /tmp/swagger.json .config.json
    rm -f generate/.output/.openapi-generator-ignore
    
generate TARGET_DIR:
    @just generate-local
    
    # need to remove the created content before copying over the top of it.
    # this prevents deleted content from hanging around indefinitely.
    rm -rf {{TARGET_DIR}}/src
    rm -rf {{TARGET_DIR}}/sdk/docs
    
    mv -R generate/.output/* {{TARGET_DIR}}

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

test:
    ./test/test.sh
