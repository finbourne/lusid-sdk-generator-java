#!/bin/bash

set -eETuo pipefail

failure() {
  local lineno="$1"
  local msg="$2"
  echo "Failed at $lineno: $msg"
}
trap 'failure ${LINENO} "$BASH_COMMAND"' ERR

file=$1 # generate/.output/sdk/src/main/java/com/finbourne/luminesce/model/FilterModel.java
find=$2 # com.finbourne.luminesce.model.Type; 
replace=$3 # private com.finbourne.luminesce.model.Type type;

# need the GNU version of sed on a mac
if [[ $(uname) == Darwin ]]; then
    if gsed --version > /dev/null; then
        shopt -s expand_aliases
        alias sed=gsed
    else
        echo "GNU sed required for this script, please add it. See https://formulae.brew.sh/formula/gnu-sed"
        exit 1
    fi
fi

echo "Running fix for luminesce"

# Check the exit status
if [ $? -eq 0 ]; then
    echo "sed command ran successfully"
else
    echo "sed command failed"
fi
# check that the expected text exists in the file
if sed -i "s|$find|$replace|" "$file"; then
    echo "sed command ran successfully, replacement done."
    exit 0
else
    echo "Failed to find $find in $file"
    exit 1
fi
