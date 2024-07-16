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
export PACKAGE_VERSION := `echo ${PACKAGE_VERSION:-2.9999.0}`
export META_REQUEST_ID_HEADER_KEY := `echo ${META_REQUEST_ID_HEADER_KEY:-lusid-meta-requestid}`
export JAVA_PACKAGE_LOCATION := `echo ${JAVA_PACKAGE_LOCATION:-~/.java/maven/local-packages}`

# needed for tests
export FBN_ACCESS_TOKEN := `echo ${FBN_ACCESS_TOKEN:-access-token}`
export FBN_TOKEN_URL := `echo ${FBN_TOKEN_URL:-https://lusid.com}`
export FBN_USERNAME := `echo ${FBN_USERNAME:-username}`
export FBN_PASSWORD := `echo ${FBN_PASSWORD:-password}`
export FBN_CLIENT_ID := `echo ${FBN_CLIENT_ID:-client-id}`
export FBN_CLIENT_SECRET := `echo ${FBN_CLIENT_SECRET:-client-secret}`
export EXCLUDE_TESTS := `echo ${EXCLUDE_TESTS:-true}`

swagger_path := "./swagger.json"

swagger_url := "https://example.lusid.com/api/swagger/v0/swagger.json"

get-swagger:
    echo {{swagger_url}}
    curl -s {{swagger_url}} > swagger.json

build-docker-images: 
    docker build -t lusid-sdk-gen-java:latest --ssh default=$SSH_AUTH_SOCK -f Dockerfile .

generate-local:
    envsubst < generate/config-template.json > generate/.config.json
    rm -r generate/.output || true
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

move-for-testing-local:
    cp -R {{justfile_directory()}}/test_sdk/src/test/ {{justfile_directory()}}/generate/.output/sdk/src/test

    # these files have been copied from the lusid sdk tests
    # rename to match the application being tested
    mv {{justfile_directory()}}/generate/.output/sdk/src/test/integration/java/com/finbourne/TO_BE_REPLACED {{justfile_directory()}}/generate/.output/sdk/src/test/integration/java/com/finbourne/${PROJECT_NAME}
    mv {{justfile_directory()}}/generate/.output/sdk/src/test/unit/java/com/finbourne/TO_BE_REPLACED {{justfile_directory()}}/generate/.output/sdk/src/test/unit/java/com/finbourne/${PROJECT_NAME}
    find {{justfile_directory()}}/generate/.output/sdk/src/test -type f -exec sed -i -e "s/TO_BE_REPLACED/${PROJECT_NAME}/g" {} \;

    mv {{justfile_directory()}}/generate/.output/sdk/pom.dev.xml {{justfile_directory()}}/generate/.output/sdk/pom.xml

move-for-testing GENERATED_DIR:
    mkdir -p .test_temp
    cp -R {{GENERATED_DIR}}/sdk .test_temp/sdk
    cp -R {{justfile_directory()}}/test_sdk/src/test/ .test_temp/sdk/src/test/

    # these files have been copied from the lusid sdk tests
    # rename to match the application being tested
    mv .test_temp/sdk/src/test/integration/java/com/finbourne/TO_BE_REPLACED .test_temp/sdk/src/test/integration/java/com/finbourne/${PLACEHOLDER_VALUE_FOR_TESTS}
    mv .test_temp/sdk/src/test/unit/java/com/finbourne/TO_BE_REPLACED .test_temp/sdk/src/test/unit/java/com/finbourne/${PLACEHOLDER_VALUE_FOR_TESTS}
    find .test_temp/sdk/src/test -type f -exec sed -i -e "s/TO_BE_REPLACED/${PLACEHOLDER_VALUE_FOR_TESTS}/g" {} \;

    # use the pom.dev.xml file
    mv .test_temp/sdk/pom.dev.xml .test_temp/sdk/pom.xml

# for local testing - assumes maven on path, doesn't use docker to play friendly with IDEs.
test-local:
    @just generate-local
    @just move-for-testing-local
    echo "{\"api\":{\"personalAccessToken\":\"$FBN_ACCESS_TOKEN\",\"tokenUrl\":\"$FBN_TOKEN_URL\",\"username\":\"$FBN_USERNAME\",\"password\":\"$FBN_PASSWORD\",\"clientId\":\"$FBN_CLIENT_ID\",\"clientSecret\":\"$FBN_CLIENT_SECRET\",\"apiUrl\":\"NOT_USED\"}}" > generate/.output/sdk/secrets.json
    cp generate/.output/sdk/secrets.json generate/.output/sdk/secrets-pat.json
    mvn -f generate/.output/sdk verify

# to be run after $(just generate-cicd {{GENERATED_DIR}})
test-cicd GENERATED_DIR:
    @just move-for-testing {{GENERATED_DIR}}
    echo "{\"api\":{\"personalAccessToken\":\"$FBN_ACCESS_TOKEN\",\"tokenUrl\":\"$FBN_TOKEN_URL\",\"username\":\"$FBN_USERNAME\",\"password\":\"$FBN_PASSWORD\",\"clientId\":\"$FBN_CLIENT_ID\",\"clientSecret\":\"$FBN_CLIENT_SECRET\",\"apiUrl\":\"NOT_USED\"}}" > .test_temp/sdk/secrets.json
    cp .test_temp/sdk/secrets.json .test_temp/sdk/secrets-pat.json
    mvn -f .test_temp/sdk test

test:
    @just test-local
    docker run -it --rm \
        -v {{justfile_directory()}}/generate/.output/sdk:/usr/src/sdk \
        -w /usr/src/sdk \
        maven:3.8.7-openjdk-18-slim \
        /bin/bash -c "cd /usr/src/sdk && mvn clean && mvn test"
    
generate TARGET_DIR:
    @just generate-local
    
    # need to remove the created content before copying over the top of it.
    # this prevents deleted content from hanging around indefinitely.
    rm -rf {{TARGET_DIR}}/sdk/src/main
    rm -rf {{TARGET_DIR}}/docs
    cp -R generate/.output/* {{TARGET_DIR}}

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

    if [ "$EXCLUDE_TESTS" != "false" ]; then rm generate/.output/sdk/pom.dev.xml; fi
    cp -R generate/.output/. {{TARGET_DIR}}
    echo "copied output to {{TARGET_DIR}}"
    ls {{TARGET_DIR}}

publish-only-local:
    mkdir -p ${JAVA_PACKAGE_LOCATION}
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
    docker run -it --rm \
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
    #!/bin/bash -ex
    echo "PACKAGE_VERSION to publish: ${PACKAGE_VERSION}"
    
    # mvn -e -f {{SRC_DIR}}/pom.xml test-compile compile
    mvn -f {{SRC_DIR}}/pom.xml versions:set -DnewVersion=${PACKAGE_VERSION}
    mvn -f {{SRC_DIR}}/pom.xml -s {{SRC_DIR}}/settings.xml clean deploy -Dmaven.test.skip=true ${extra_mvn_commandline_options}
