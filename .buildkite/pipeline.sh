#!/usr/bin/env bash
### Build and quick lint
set -e
cat <<EOF
steps:
  - label: look around env
    command:
      - whoami
      - ls -lat
      - cat /etc/group
  - label: "Check Java formatting"
    command:
      - ls -latr
      - whoami
      - ".buildkite/check_java_format.sh"
    plugins:
      - docker#v2.1.0:
          image: "batfish/ci-base:latest"
  - label: "Check Python templates"
    command:
      - "python3 -m virtualenv .venv"
      - ". .venv/bin/activate"
      - "python3 -m pip install pytest"
      - "cd tests && pytest"
    plugins:
      - docker#v2.1.0:
          image: "batfish/ci-base:latest"
  - label: "Build"
    command:
      - "mkdir workspace"
      - "mvn -f projects package"
      - "cp projects/allinone/target/allinone-bundle-*.jar workspace/allinone.jar"
    artifact_paths:
      - workspace/allinone.jar
    plugins:
      - docker#v2.1.0:
          image: "batfish/ci-base:latest"
  - wait
EOF
