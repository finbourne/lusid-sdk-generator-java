#!/bin/bash

set -EeTuo pipefail

failure() {
    local lineno=$1
    local msg=$2
    echo "Failed at $lineno: $msg"
}
trap 'failure ${LINENO} "$BASH_COMMAND"' ERR

if [[ ${#1} -eq 0 ]]; then
    echo
    echo "[ERROR] generate folder file path not specified"
    exit 1
fi

if [[ ${#2} -eq 0 ]]; then
    echo
    echo "[ERROR] output folder file path not specified"
    exit 1
fi

if [[ ${#3} -eq 0 ]]; then
    echo
    echo "[ERROR] swagger file not specified"
    exit 1
fi

gen_root=$1
output_folder=$2
swagger_file=$3
config_file_name=$4
sdk_output_folder=$output_folder/sdk
JAVA_OPTS=${JAVA_OPTS:--Dlog.level=info}

transformed_swagger=$(jq '$swagger[] + {"x-group-parameters":true}' --slurpfile swagger $3 --null-input)

echo $transformed_swagger > $swagger_file

if [[ -z $config_file_name || ! -f $gen_root/$config_file_name ]] ; then
    echo "[INFO] '$config_file_name' not found, using default config.json"
    config_file_name=config.json
fi

echo "[INFO] root generation : $gen_root"
echo "[INFO] output folder   : $output_folder"
echo "[INFO] swagger file    : $swagger_file"
echo "[INFO] config file     : $config_file_name"

ignore_file_name=.openapi-generator-ignore
config_file=$gen_root/$config_file_name
ignore_file=$output_folder/$ignore_file_name

#   remove all previously generated files
shopt -s extglob
echo "[INFO] removing previous sdk"
rm -rf $sdk_output_folder/docs
rm -rf $sdk_output_folder/target
rm -rf $sdk_output_folder/src/
shopt -u extglob

mkdir -p $sdk_output_folder
cp $ignore_file $sdk_output_folder

echo "[INFO] generating sdk version: ${PACKAGE_VERSION}"

java ${JAVA_OPTS} -jar /opt/openapi-generator/modules/openapi-generator-cli/target/openapi-generator-cli.jar generate \
    -i $swagger_file \
    -g java \
    -o $sdk_output_folder \
    -c $config_file \
    -t $gen_root/templates \
    --additional-properties=application=${APPLICATION_NAME},meta_request_id_header_key=${META_REQUEST_ID_HEADER_KEY} \
    --type-mappings Double=java.math.BigDecimal

# remove redundant generated build files
shopt -s extglob
set +e
rm -f $sdk_output_folder/.openapi-generator-ignore
rm -rf $sdk_output_folder/.openapi-generator/
shopt -u extglob

rm -f $output_folder/.openapi-generator-ignore
rm -f $output_folder/api
rm -f $sdk_output_folder/pom.xml.versionsBackup
set -e

mkdir -p $output_folder/docs/
cp -R /tmp/docs/docs/* $output_folder/docs/
mkdir -p $output_folder/.github/
cp -R /tmp/workflows/github/* $output_folder/.github/
