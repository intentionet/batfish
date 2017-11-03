#!/usr/bin/env bash

set -e -x

if [[ $(uname) == 'Darwin' ]]; then
   GNU_FIND=gfind
else
   GNU_FIND=find
fi

GJF_VERSION=1.5
JAR_NAME="google-java-format-${GJF_VERSION}-all-deps.jar"
JAR="${HOME}/.m2/repository/com/google/googlejavaformat/google-java-format/${GJF_VERSION}/${JAR_NAME}"
mvn dependency:get -Dartifact=com.google.googlejavaformat:google-java-format:${GJF_VERSION}:jar:all-deps

echo
echo 'Checking that all java files are formatted correctly'
echo

# Create a file containing names of all relevant files
${GNU_FIND} projects -regex '.*/src/main/.*\.java' -or -regex '.*/src/test/.*\.java' > files_to_check
java -jar ${JAR} --dry-run --set-exit-if-changed @files_to_check \
  || (echo "\n\nThe files listed above are not formatted correctly. Use google-java-format (https://github.com/google/google-java-format#google-java-format) version ${GJF_VERSION} to correct these issues. We recommend you install the Eclipse or IntelliJ plugins." && exit 1)
