#!/bin/bash
#
# Copyright 2018 The Batfish Open Source Project. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
USAGE='update_javadoc.sh'
DESCRIPTION='
  Builds the Batfish javadoc and prepares a commit upgrading the batfish.org
  website.'

function usage() {
  echo "$USAGE" "$DESCRIPTION" >&2
}

function die() {
  echo "$1"
  exit 1
}

[ $# -eq 0 ] || { usage; exit 1; }

dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )" || die "Could not determine directory"
cd ${dir}/..

git fetch -q origin master &&
    git diff origin/master --exit-code -- projects/ || die "Build branch is not clean"
sha=$(git rev-parse --verify HEAD)
branch="update-javadoc-${sha}"

bazel build //projects:javadoc || die "Could not build Javadoc"
javadoc_zip="$(bazel info bazel-bin)/projects/javadoc.zip"

output_dir="$(mktemp -d)" || die "Could not create tmp directory"
git clone --depth 1 --single-branch \
    git@github.com:batfish/batfish.github.io.git \
    ${output_dir} || die "Could not clone batfish.github.io"
pushd ${output_dir} &&
    git checkout -b ${branch} &&
    git rm -r docs/ &&
    unzip ${javadoc_zip} -d docs/ &&
    git add docs/ &&
    git commit -m "Update javadoc at ${sha}" || die "Could not update javadoc"

echo "Prepared commit to update javadoc at ${output_dir}"
