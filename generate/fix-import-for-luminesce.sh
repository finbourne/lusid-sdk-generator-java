#!/bin/bash

set -eETuo pipefail

failure() {
  local lineno="$1"
  local msg="$2"
  echo "Failed at $lineno: $msg"
}
trap 'failure ${LINENO} "$BASH_COMMAND"' ERR

file=$1
find=$2
replace=$3

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
# check that the expected text exists in the file
if sed -i "s|$find|$replace|" "$file"; then
    echo "sed command ran successfully, replacement done."
    exit 0
else
    echo "Failed to find $find in $file"
    exit 1
fi
