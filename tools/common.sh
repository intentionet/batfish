#!/usr/bin/env bash

export BATFISH_ROOT="$(dirname "$BATFISH_TOOLS_PATH")"
export PROJECTS_PATH="$BATFISH_ROOT/projects"

# Use Maven to print the current Batfish java version
export BATFISH_VERSION="$(grep -1 batfish-parent "${PROJECTS_PATH}/pom.xml" | grep version | sed 's/[<>]/|/g' | cut -f3 -d\|)"

export BATFISH_PATH="$PROJECTS_PATH/batfish"
export BATFISH_TEST_RIG_PATH="$BATFISH_ROOT/networks"
export BATFISH="$BATFISH_PATH/batfish"
export FLATTEN="$BATFISH_PATH/flatten"

export BATFISH_CLIENT_PATH="$PROJECTS_PATH/batfish-client"
export BATFISH_CLIENT="$BATFISH_CLIENT_PATH/batfish-client"

export BATFISH_DOCS_ROOT="$BATFISH_ROOT/docs"

export COORDINATOR_PATH="$PROJECTS_PATH/coordinator"
export COORDINATOR="$COORDINATOR_PATH/coordinator"

export ALLINONE_PATH="$PROJECTS_PATH/allinone"
export ALLINONE="$ALLINONE_PATH/allinone"

export COMMON_PATH="$PROJECTS_PATH/batfish-common-protocol"
export COMMON_JAR="$COMMON_PATH/target/batfish-common-protocol-${BATFISH_VERSION}.jar"

export QUESTION_PATH="$PROJECTS_PATH/question"
export BATFISH_QUESTION_PLUGIN_DIR="$PROJECTS_PATH/question/target/"

export ALLINONE_COMPLETION_FILE="$BATFISH_TOOLS_PATH/completion-allinone.tmp"
export BATFISH_COMPLETION_FILE="$BATFISH_TOOLS_PATH/completion-batfish.tmp"
export BATFISH_CLIENT_COMPLETION_FILE="$BATFISH_TOOLS_PATH/completion-batfish-client.tmp"
export COORDINATOR_COMPLETION_FILE="$BATFISH_TOOLS_PATH/completion-coordinator.tmp"

batfish() {
   # if cygwin, shift and replace each parameter
   if batfish_cygwin; then
      local NUMARGS=$#
      local IGNORE_CURRENT_ARG=no;
      for i in $(seq 1 ${NUMARGS}); do
         local CURRENT_ARG=$1
         local NEW_ARG="$(cygpath -w -- ${CURRENT_ARG})"
         set -- "$@" "$NEW_ARG"
         shift
      done
   fi
   if [ "$BATFISH_PRINT_CMDLINE" = "yes" ]; then
      echo "$BATFISH $BATFISH_COMMON_ARGS $@" >&2
   fi
   "$BATFISH" ${BATFISH_COMMON_ARGS} "$@"
}
export -f batfish

batfish_build() {
   bash -c '_batfish_build "$@"' _batfish_build "$@" || return 1
}
export -f batfish_build

_batfish_build() {
   _pre_build || return 1
   mvn install -pl batfish -am || return 1
   if [ "$BATFISH_COMPLETION_FILE" -ot "$BATFISH_PATH/target/batfish-${BATFISH_VERSION}.jar" -a -e "$BATFISH_PATH/target/batfish-${BATFISH_VERSION}.jar" ]; then
      echo -n "Generating bash completion file (after batfish_build) ..."
      BATFISH_PRINT_CMDLINE=no batfish -help | grep -o '^ *-[a-zA-Z0-9]*' | tr -d ' ' | tr '\n' ' ' > "$BATFISH_COMPLETION_FILE"
      . "${BATFISH_TOOLS_PATH}/completion-batfish.sh"
      echo "OK"
   fi
}
export -f _batfish_build

batfish_build_all() {
   bash -c '_batfish_build_all "$@"' _batfish_build_all "$@" || return 1
   if [ "$ALLINONE_COMPLETION_FILE" -ot "$ALLINONE_PATH/target/allinone-${BATFISH_VERSION}.jar" -a -e "$ALLINONE_PATH/target/allinone-${BATFISH_VERSION}.jar" ]; then
      echo -n "Generating bash completion file for allinone (via batfish_build_all) ..."
      BATFISH_PRINT_CMDLINE=no allinone -help | grep -o '^ *-[a-zA-Z0-9]*' | tr -d ' ' | tr '\n' ' ' > "$ALLINONE_COMPLETION_FILE"
      . "${BATFISH_TOOLS_PATH}/completion-allinone.sh"
      echo "OK"
   fi
   if [ "$BATFISH_COMPLETION_FILE" -ot "$BATFISH_PATH/target/batfish-${BATFISH_VERSION}.jar" -a -e "$BATFISH_PATH/target/batfish-${BATFISH_VERSION}.jar" ]; then
      echo -n "Generating bash completion file for batfish (via batfish_build_all) ..."
      BATFISH_PRINT_CMDLINE=no batfish -help | grep -o '^ *-[a-zA-Z0-9]*' | tr -d ' ' | tr '\n' ' ' > "$BATFISH_COMPLETION_FILE"
      echo "OK"
   fi
   if [ "$BATFISH_CLIENT_COMPLETION_FILE" -ot "$BATFISH_CLIENT_PATH/target/batfish-client-${BATFISH_VERSION}.jar" -a -e "$BATFISH_CLIENT_PATH/target/batfish-client-${BATFISH_VERSION}.jar" ]; then
      echo -n "Generating bash completion file for batfish-client (via batfish_build_all) ..."
      BATFISH_PRINT_CMDLINE=no batfish_client -help | grep -o '^ *-[a-zA-Z0-9]*' | tr -d ' ' | tr '\n' ' ' > "$BATFISH_CLIENT_COMPLETION_FILE"
      echo "OK"
   fi
   if [ "$COORDINATOR_COMPLETION_FILE" -ot "$COORDINATOR_PATH/target/coordinator-${BATFISH_VERSION}.jar" -a -e "$COORDINATOR_PATH/target/coordinator-${BATFISH_VERSION}.jar" ]; then
      echo -n "Generating bash completion file for coordinator (via batfish_build_all) ..."
      BATFISH_PRINT_CMDLINE=no coordinator -help | grep -o '^ *-[a-zA-Z0-9]*' | tr -d ' ' | tr '\n' ' ' > "$COORDINATOR_COMPLETION_FILE"
      echo "OK"
   fi
}
export -f batfish_build_all

_batfish_build_all() {
   _pre_build || return 1
   cd "${PROJECTS_PATH}"
   mvn install || return 1
}
export -f _batfish_build_all

batfish_rebuild_all() {
   bash -c '_batfish_rebuild_all "$@"' _batfish_rebuild_all "$@" || return 1
}
export -f batfish_rebuild_all

_batfish_rebuild_all() {
   _pre_build || return 1
   cd "${PROJECTS_PATH}"
   mvn -DskipTests clean install || return 1
}
export -f _batfish_rebuild_all

batfish_test_all() {
   bash -c '_batfish_test_all "$@"' _batfish_test_all "$@" || return 1
}
export -f batfish_test_all

_batfish_test_all() {
   _pre_build || return 1
   cd "${PROJECTS_PATH}"
   mvn clean install -P '!fast' || return 1
}
export -f _batfish_test_all

batfish_confirm() {
   # call with a prompt string or use a default
   read -r -p "${1:-Are you sure? [y/N]} " response < /dev/tty
   case ${response} in
      [yY][eE][sS]|[yY])
         true
      ;;
      *)
         false
      ;;
   esac
}
export -f batfish_confirm

batfish_cygwin() {
   [[ $(uname) == *"CYGWIN"* ]]
}
export -f batfish_cygwin

batfish_date() {
   { hostname; echo -n ': '; date ; } | tr -d '\n'
}
export -f batfish_date

batfish_expect_args() {
   local EXPECTED_NUMARGS=$1
   local ACTUAL_NUMARGS=$2
   if [ "$EXPECTED_NUMARGS" -ne "$ACTUAL_NUMARGS" ]; then
      echo "${FUNCNAME[1]}: Expected $EXPECTED_NUMARGS arguments, but got $ACTUAL_NUMARGS" >&2
      return 1
   fi   
}
export -f batfish_expect_args

batfish_expect_min_args() {
   local EXPECTED_NUMARGS=$1
   local ACTUAL_NUMARGS=$2
   if [ "$EXPECTED_NUMARGS" -gt "$ACTUAL_NUMARGS" ]; then
      echo "${FUNCNAME[1]}: Expected at least $EXPECTED_NUMARGS arguments, but got $ACTUAL_NUMARGS" >&2
      return 1
   fi
}
export -f batfish_expect_min_args

batfish_javadocs() {
   echo "Generating batfish project javadocs"
   batfish_build_all doc
   cp -r ${COMMON_PATH}/doc ${BATFISH_ROOT}/doc/batfish-common-protocol/
   cp -r ${BATFISH_PATH}/doc ${BATFISH_ROOT}/doc/batfish/
   cp -r ${BATFISH_CLIENT_PATH}/doc ${BATFISH_ROOT}/doc/batfish-client/
   cp -r ${COORDINATOR_PATH}/doc ${BATFISH_ROOT}/doc/coordinator/
   cp -r ${ALLINONE_PATH}/doc ${BATFISH_ROOT}/doc/allinone/
}
export -f batfish_javadocs

batfish_prepare_test_rig() {
   batfish_date
   echo ": START: Prepare test-rig"
   batfish_expect_args 3 $# || return 1
   local TEST_RIG=$1
   local BASE=$2
   local NAME=$3
   mkdir -p ${BASE}/testrigs/${NAME}/testrig || return 1
   mkdir -p ${BASE}/testrigs/${NAME}/environments/default/env_default
   cp -r ${TEST_RIG}/. ${BASE}/testrigs/${NAME}/testrig/.
   batfish_date
   echo ": END: Prepare test-rig"
}
export -f batfish_prepare_test_rig

batfish_reload() {
   . ${BATFISH_SOURCED_SCRIPT}
}
export -f batfish_reload

batfish_unit_tests_parser() {
   local UNIT_TEST_NAME=unit-tests
   local UNIT_TEST_DIR=${BATFISH_TEST_RIG_PATH}/${UNIT_TEST_NAME}
   batfish_date
   echo ": START UNIT TEST: Vendor configuration parser"
   batfish_prepare_test_rig ${UNIT_TEST_DIR} $PWD ${UNIT_TEST_NAME}
   batfish -containerdir $PWD -testrig ${UNIT_TEST_NAME} -sv "$@" -haltonparseerror -haltonconverterror -urf false
   batfish_date
   echo ": END UNIT TEST: Vendor configuration parser"
}
export -f batfish_unit_tests_parser

int_to_ip() {
   batfish_expect_args 1 $# || return 1
   local INPUT=$1
   local OCTET_0=$(( INPUT % 256 ))
   local OCTET_1=$(( (INPUT / 256) % 256 ))
   local OCTET_2=$(( (INPUT / 65536) % 256 ))
   local OCTET_3=$(( INPUT / 16777216 ))
   echo "${OCTET_3}.${OCTET_2}.${OCTET_1}.${OCTET_0}"
}
export -f int_to_ip

ip_to_int() {
   batfish_expect_args 1 $# || return 1
   local INPUT=$1
   local OCTET_0=$(echo "$INPUT" | cut -d'.' -f 4)
   local OCTET_1=$(echo "$INPUT" | cut -d'.' -f 3)
   local OCTET_2=$(echo "$INPUT" | cut -d'.' -f 2)
   local OCTET_3=$(echo "$INPUT" | cut -d'.' -f 1)
   echo $((${OCTET_3} * 16777216 + ${OCTET_2} * 65536 + ${OCTET_1} * 256 + ${OCTET_0}))
}
export -f ip_to_int

# if the 'time' binary is not available (e.g. on cygwin it is a bash builtin), define it
if [ -z "$(which time 2>&1)" ]; then
   batfish_time() {
      bash -c "time $@"
   }
else
   batfish_time() {
      time "$@"
   }
fi
export -f batfish_time

coordinator() {
   # if cygwin, shift and replace each parameter
   if batfish_cygwin; then
      local NUMARGS=$#
      for i in $(seq 1 ${NUMARGS}); do
         local CURRENT_ARG=$1
         local NEW_ARG="$(cygpath -w -- ${CURRENT_ARG})"
         set -- "$@" "$NEW_ARG"
         shift
      done
   fi
   if [ "$COORDINATOR_PRINT_CMDLINE" = "yes" ]; then
      echo "$COORDINATOR $COORDINATOR_COMMON_ARGS $@"
   fi
   ${COORDINATOR} ${COORDINATOR_COMMON_ARGS} "$@"
}
export -f coordinator

_pre_build() {
  cd ${PROJECTS_PATH} || return 1
}
export -f _pre_build

batfish_client() {
   # if cygwin, shift and replace each parameter
   if batfish_cygwin; then
      local NUMARGS=$#
      for i in $(seq 1 ${NUMARGS}); do
         local CURRENT_ARG=$1
         local NEW_ARG="$(cygpath -w -- ${CURRENT_ARG})"
         set -- "$@" "$NEW_ARG"
         shift
      done
   fi
   if [ "$BATFISH_CLIENT_PRINT_CMDLINE" = "yes" ]; then
      echo "$BATFISH_CLIENT $BATFISH_CLIENT_COMMON_ARGS $@"
   fi
   ${BATFISH_CLIENT} ${BATFISH_CLIENT_COMMON_ARGS} "$@"
}
export -f batfish_client

client_build() {
   bash -c '_client_build "$@"' _client_build "$@" || return 1
}
export -f client_build

_client_build() {
   _pre_build || return 1
   mvn install -DskipTests -pl batfish-client -am || return 1
   if [ "$BATFISH_CLIENT_COMPLETION_FILE" -ot "$BATFISH_CLIENT_PATH/target/batfish-client-${BATFISH_VERSION}.jar" -a -e "$BATFISH_CLIENT_PATH/target/batfish-client-${BATFISH_VERSION}.jar" ]; then
      echo -n "Generating bash completion file for batfish-client (via client_build) ..."
      BATFISH_PRINT_CMDLINE=no batfish_client -help | grep -o '^ *-[a-zA-Z0-9]*' | tr -d ' ' | tr '\n' ' ' > "$BATFISH_CLIENT_COMPLETION_FILE"
      echo "OK"
   fi
}
export -f _client_build

allinone() {
   # if cygwin, shift and replace each parameter
   if batfish_cygwin; then
      local NUMARGS=$#
      for i in $(seq 1 ${NUMARGS}); do
         local CURRENT_ARG=$1
         local NEW_ARG="$(cygpath -w -- ${CURRENT_ARG})"
         set -- "$@" "$NEW_ARG"
         shift
      done
   fi
   if [ "$ALLINONE_PRINT_CMDLINE" = "yes" ]; then
      echo "$ALLINONE $ALLINONE_COMMON_ARGS $@"
   fi
   "$ALLINONE" ${ALLINONE_COMMON_ARGS} "$@"
}
export -f allinone

allinone_build() {
   bash -c '_allinone_build "$@"' _allinone_build "$@" || return 1
}

_allinone_build() {
   _pre_build || return 1
   mvn install -pl allinone -am || return 1
   if [ "$ALLINONE_COMPLETION_FILE" -ot "$ALLINONE_PATH/target/allinone-${BATFISH_VERSION}.jar" -a -e "$ALLINONE_PATH/target/allinone-${BATFISH_VERSION}.jar" ]; then
      echo -n "Generating bash completion file for allinone (via allinone_build) ..."
      BATFISH_PRINT_CMDLINE=no allinone -help | grep -o '^ *-[a-zA-Z0-9]*' | tr -d ' ' | tr '\n' ' ' > "$ALLINONE_COMPLETION_FILE"
      . "${BATFISH_TOOLS_PATH}/completion-allinone.sh"
      echo "OK"
   fi
}
export -f _allinone_build

coordinator_build() {
   bash -c '_coordinator_build "$@"' _coordinator_build "$@" || return 1
}
export -f coordinator_build

_coordinator_build() {
   _pre_build || return 1
   mvn install -pl coordinator -am || return 1
   if [ "$COORDINATOR_COMPLETION_FILE" -ot "$COORDINATOR_PATH/target/coordinator-${BATFISH_VERSION}.jar" -a -e "$COORDINATOR_PATH/target/coordinator-${BATFISH_VERSION}.jar" ]; then
      echo -n "Generating bash completion file for coordinator (via coordinator_build) ..."
      BATFISH_PRINT_CMDLINE=no coordinator -help | grep -o '^ *-[a-zA-Z0-9]*' | tr -d ' ' | tr '\n' ' ' > "$COORDINATOR_COMPLETION_FILE"
      echo "OK"
   fi
}
export -f _coordinator_build

common_build() {
   bash -c '_common_build "$@"' _common_build "$@" || return 1
}
export -f common_build

_common_build() {
   _pre_build || return 1
   mvn install -pl batfish-common-protocol -am || return 1
}
export -f _common_build

flatten() {
   # if cygwin, shift and replace each parameter
   if batfish_cygwin; then
      local NUMARGS=$#
      local IGNORE_CURRENT_ARG=no;
      for i in $(seq 1 ${NUMARGS}); do
         local CURRENT_ARG=$1
         local NEW_ARG="$(cygpath -w -- ${CURRENT_ARG})"
         set -- "$@" "$NEW_ARG"
         shift
      done
   fi
   if [ "$BATFISH_PRINT_CMDLINE" = "yes" ]; then
      echo "$BATFISH $BATFISH_COMMON_ARGS $@" >&2
   fi
   "$FLATTEN" "$@"
}
export -f flatten


