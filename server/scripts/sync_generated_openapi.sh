#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd)"
generated_src="$root_dir/api/out/erlang-server/src"
generated_priv="$root_dir/api/out/erlang-server/priv/openapi.json"
target_src="$root_dir/server/src/generated"
target_priv="$root_dir/server/priv"

mkdir -p "$target_src" "$target_priv"

cp "$generated_src"/openapi_*.erl "$target_src"/
cp "$generated_priv" "$target_priv"/openapi.json

echo "Synced generated OpenAPI Erlang files into $target_src"
