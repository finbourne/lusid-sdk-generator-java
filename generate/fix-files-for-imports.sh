#!/bin/bash

set -eETuo pipefail

failure() {
  local lineno="$1"
  local msg="$2"
  echo "Failed at $lineno: $msg"
}
trap 'failure ${LINENO} "$BASH_COMMAND"' ERR

justfile_dir=$1
package_name=$2
application=$3

while read -r item; do
    echo "item='$item'"
    file="$(echo "$item" | jq -r '.file')"
    find="$(echo "$item" | jq -r '.find')"
    replace="$(echo "$item" | jq -r '.replace')"
    bash "$justfile_dir/generate/fix-import-for-luminesce.sh" "$justfile_dir/generate/.output/sdk/src/main/java/com/finbourne/$application/model/$file.java" "$find" "$replace"
done <<< "$(jq -rc '.items[]' "$justfile_dir/generate/luminesce-import-fix-list.json")"