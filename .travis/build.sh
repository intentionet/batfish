#!/usr/bin/env bash

if [[ $(uname) == 'Darwin' && $(which gfind) ]]; then
   GNU_FIND=gfind
else
   GNU_FIND=find
fi

trap 'kill -9 $(pgrep -g $$ | grep -v $$) >& /dev/null' EXIT SIGINT SIGTERM

. tools/batfish_functions.sh


# Build batfish and run the Maven unit tests.
batfish_test_all || exit 1

# Configure arguments for allinone throughout later runs.
export ALLINONE_JAVA_ARGS="-enableassertions -DbatfishCoordinatorPropertiesPath=${BATFISH_ROOT}/.travis/travis_coordinator.properties"

exit_code=0
echo -e "\n  ..... Running parsing tests"
allinone -cmdfile tests/parsing-tests/commands || exit_code=$?

echo -e "\n  ..... Running parsing tests with error"
allinone -cmdfile tests/parsing-errors-tests/commands || exit_code=$?

echo -e "\n  ..... Running basic client tests"
allinone -cmdfile tests/basic/commands || exit_code=$?

echo -e "\n  ..... Running role functionality tests"
allinone -cmdfile tests/roles/commands || exit_code=$?

echo -e "\n  ..... Running jsonpath tests"
allinone -cmdfile tests/jsonpath-addons/commands || exit_code=$?
allinone -cmdfile tests/jsonpathtotable/commands || exit_code=$?

echo -e "\n  ..... Running ui-focused client tests"
allinone -cmdfile tests/ui-focused/commands || exit_code=$?

echo -e "\n  ..... Running aws client tests"
allinone -cmdfile tests/aws/commands || exit_code=$?

echo -e "\n  ..... Running java-smt client tests"
allinone -cmdfile tests/java-smt/commands || exit_code=$?

echo -e "\n  ..... Running watchdog tests"
allinone -cmdfile tests/watchdog/commands -batfishmode watchdog || exit_code=$?
sleep 5

#Test running separately
coordinator &
batfish -runmode workservice -register -coordinatorhost localhost -loglevel output &

echo -e "\n  ..... Running java demo tests"
if ! batfish_client -cmdfile demos/example/commands -coordinatorhost localhost > demos/example/commands.ref.testout; then
   echo "DEMO FAILED!" 1>&2
   exit_code=1
elif diff -q demos/example/commands.ref{,testout} &> /dev/null; then
   rm demos/example/commands.ref.testout
fi

echo -e "\n .... Failed tests: "
$GNU_FIND -name *.testout

echo -e "\n .... Diffing failed tests:"
for i in $($GNU_FIND -name *.testout); do
   echo -e "\n $i"; diff -u ${i%.testout} $i
done

exit $exit_code
