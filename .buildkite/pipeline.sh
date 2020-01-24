#!/usr/bin/env bash
### Build and quick lint
set -euo pipefail

BATFISH_ARTIFACTS_PLUGIN_VERSION="${BATFISH_ARTIFACTS_PLUGIN_VERSION:-v1.2.0}"
BATFISH_DOCKER_PLUGIN_VERSION="${BATFISH_DOCKER_PLUGIN_VERSION:-v3.3.0}"
BATFISH_DOCKER_CI_BASE_IMAGE="${BATFISH_DOCKER_CI_BASE_IMAGE:-batfish/ci-base:latest}"

cat <<EOF
steps:
EOF

###### WAIT before starting any of the jobs.
cat <<EOF
  - wait
EOF

###### Initial checks plus building the jar
cat <<EOF
  - label: ":java: Formatting"
    key: format
    command: "tools/fix_java_format.sh --check"
    plugins:
      - docker#${BATFISH_DOCKER_PLUGIN_VERSION}:
          image: ${BATFISH_DOCKER_CI_BASE_IMAGE}
          always-pull: true
  - label: ":json: Templates"
    key: template
    command:
      - "python3 -m virtualenv .venv"
      - ". .venv/bin/activate"
      - "python3 -m pip install pytest"
      - "cd tests && pytest"
    plugins:
      - docker#${BATFISH_DOCKER_PLUGIN_VERSION}:
          image: ${BATFISH_DOCKER_CI_BASE_IMAGE}
          always-pull: true
  - label: ":bazel: Build allinone.jar"
    key: jar
    command:
      - "python3 -m virtualenv .venv"
      - ". .venv/bin/activate"
      - "bazel build -- //projects/allinone:allinone_main_deploy.jar
      - "mkdir workspace"
      - "cp $$(bazel info bazel-bin)/projects/allinone/allinone_main_deploy.jar workspace/allinone.jar"
    artifact_paths:
      - workspace/allinone.jar
    plugins:
      - docker#${BATFISH_DOCKER_PLUGIN_VERSION}:
          image: ${BATFISH_DOCKER_CI_BASE_IMAGE}
          always-pull: true
          volumes:
            - $HOME/.bazelrc:/home/batfish/.bazelrc
EOF

###### Build tests and code static analysis
cat <<EOF
  - label: ":mvn: :junit: :coverage: Tests + Coverage"
    depends_on:
      - format
      - template
    command:
      - mvn -f projects/pom.xml test -DskipTests=false -Djacoco.skip=false
      - mkdir -p workspace
      - rsync -zarv --prune-empty-dirs --include '*/' --include 'jacoco*.exec' --exclude '*' projects/ workspace/
    artifact_paths:
      - workspace/**/jacoco.exec
    plugins:
      - docker#${BATFISH_DOCKER_PLUGIN_VERSION}:
          image: ${BATFISH_DOCKER_CI_BASE_IMAGE}
          always-pull: true
  - label: ":mvn: Checkstyle, findbugs, dependency"
    depends_on:
      - format
      - template
    command: "mvn -f projects/pom.xml verify -Dcheckstyle.skip=false -Dmdep.analyze.skip=false -Dfindbugs.skip=false"
    plugins:
      - docker#${BATFISH_DOCKER_PLUGIN_VERSION}:
          image: ${BATFISH_DOCKER_CI_BASE_IMAGE}
          always-pull: true
  - label: ":mvn: Javadoc"
    depends_on:
      - format
      - template
    command: "mvn -f projects/pom.xml verify -Dmaven.javadoc.skip=false"
    skip: True
    plugins:
      - docker#${BATFISH_DOCKER_PLUGIN_VERSION}:
          image: ${BATFISH_DOCKER_CI_BASE_IMAGE}
          always-pull: true
  - label: ":mvn: PMD"
    depends_on:
      - format
      - template
    command: "mvn -f projects/pom.xml verify -Dpmd.skip=false"
    plugins:
      - docker#${BATFISH_DOCKER_PLUGIN_VERSION}:
          image: ${BATFISH_DOCKER_CI_BASE_IMAGE}
          always-pull: true
EOF

###### Ensure the code still compiles with Bazel
cat <<EOF
  - label: ":bazel: Bazel"
    depends_on:
      - format
      - template
    command:
      - "python3 -m virtualenv .venv"
      - ". .venv/bin/activate"
      - "bazel build -- //..."
    plugins:
      - docker#${BATFISH_DOCKER_PLUGIN_VERSION}:
          image: ${BATFISH_DOCKER_CI_BASE_IMAGE}
          always-pull: true
          volumes:
            - $HOME/.bazelrc:/home/batfish/.bazelrc
EOF

###### Ref tests
# TODO: consider parallel builds for this?
# https://buildkite.com/docs/tutorials/parallel-builds#parallel-jobs
for cmd in $(find tests -name commands); do
  cat <<EOF
  - label: ":batfish: ${cmd} ref tests"
    depends_on: jar
    command: ".buildkite/ref_test.sh ${cmd}"
    plugins:
      - docker#${BATFISH_DOCKER_PLUGIN_VERSION}:
          image: ${BATFISH_DOCKER_CI_BASE_IMAGE}
          always-pull: true
      - artifacts#${BATFISH_ARTIFACTS_PLUGIN_VERSION}:
          download: workspace/allinone.jar
    artifact_paths:
      - workspace/**/jacoco.exec
EOF
done

###### Code coverage
cat <<EOF
  - wait
  - label: ":coverage: Report coverage"
    command:
      - ".buildkite/jacoco_report.sh"
    plugins:
      - docker#${BATFISH_DOCKER_PLUGIN_VERSION}:
          image: "${BATFISH_DOCKER_CI_BASE_IMAGE}"
          always-pull: true
          propagate-environment: true
      - artifacts#${BATFISH_ARTIFACTS_PLUGIN_VERSION}:
          download:
            - "workspace/**/jacoco.exec"
            - "workspace/allinone.jar"
EOF
