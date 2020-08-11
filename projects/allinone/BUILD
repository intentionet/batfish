load("@rules_java//java:defs.bzl", "java_binary", "java_library")
load("@batfish//skylark:junit.bzl", "junit_tests")
load("@batfish//skylark:pmd_test.bzl", "pmd_test")

package(default_visibility = ["//visibility:public"])

java_binary(
    name = "allinone_main",
    main_class = "org.batfish.allinone.Main",
    runtime_deps = [
        ":allinone",
        "//projects/question",
        "@maven//:io_jaegertracing_jaeger_thrift",
        "@maven//:org_apache_logging_log4j_log4j_core",
        "@maven//:org_apache_logging_log4j_log4j_slf4j_impl",
    ],
)

java_binary(
    name = "allinone_with_minesweeper_main",
    main_class = "org.batfish.allinone.Main",
    runtime_deps = [
        ":allinone",
        "//projects/minesweeper",
        "//projects/question",
        "@maven//:org_apache_logging_log4j_log4j_slf4j_impl",
    ],
)

java_library(
    name = "allinone",
    srcs = glob([
        "src/main/**/*.java",
    ]),
    resources = glob(["src/main/resources/**"]),
    deps = [
        "//projects/batfish",
        "//projects/batfish-client:client",
        "//projects/batfish-common-protocol:common",
        "//projects/coordinator",
        "@maven//:com_google_guava_guava",
        "@maven//:io_jaegertracing_jaeger_core",
        "@maven//:io_opentracing_opentracing_util",
    ],
)

junit_tests(
    name = "AaaAuthenticationLoginTest",
    size = "small",
    srcs = [
        "src/test/java/org/batfish/question/aaaauthenticationlogin/AaaAuthenticationLoginTest.java",
    ],
    resources = glob(["src/test/resources/org/batfish/allinone/testconfigs/*"]),
    deps = [
        "//projects/batfish",
        "//projects/batfish:batfish_testlib",
        "//projects/batfish-common-protocol:common",
        "//projects/batfish-common-protocol/src/test/java/org/batfish/datamodel/matchers",
        "//projects/question",
        "@maven//:junit_junit",
        "@maven//:org_hamcrest_hamcrest",
    ],
)

junit_tests(
    name = "CompareSameNameTest",
    size = "small",
    srcs = [
        "src/test/java/org/batfish/question/CompareSameNameTest.java",
    ],
    deps = [
        "//projects/batfish",
        "//projects/batfish:batfish_testlib",
        "//projects/batfish-common-protocol:common",
        "//projects/question",
        "@maven//:com_google_guava_guava",
        "@maven//:junit_junit",
        "@maven//:org_hamcrest_hamcrest",
    ],
)

junit_tests(
    name = "DetectLoopsTest",
    size = "small",
    srcs = [
        "src/test/java/org/batfish/question/loop/DetectLoopsTest.java",
    ],
    deps = [
        "//projects/batfish",
        "//projects/batfish:batfish_testlib",
        "//projects/batfish-common-protocol:common",
        "//projects/batfish-common-protocol:common_testlib",
        "//projects/batfish-common-protocol/src/test/java/org/batfish/datamodel/matchers",
        "//projects/question",
        "@maven//:junit_junit",
        "@maven//:org_hamcrest_hamcrest",
    ],
)

junit_tests(
    name = "F5BigipVipConfigurationTest",
    size = "small",
    srcs = [
        "src/test/java/org/batfish/question/f5_bigip/F5BigipVipConfigurationTest.java",
    ],
    resources = glob(["src/test/resources/org/batfish/allinone/testconfigs/*"]),
    deps = [
        "//projects/batfish",
        "//projects/batfish:batfish_testlib",
        "//projects/batfish-common-protocol:common",
        "//projects/batfish-common-protocol/src/test/java/org/batfish/datamodel/matchers",
        "//projects/question",
        "@maven//:com_google_guava_guava",
        "@maven//:junit_junit",
        "@maven//:org_hamcrest_hamcrest",
    ],
)

junit_tests(
    name = "MultipathConsistencyTest",
    size = "small",
    srcs = [
        "src/test/java/org/batfish/question/multipath/MultipathConsistencyTest.java",
    ],
    deps = [
        "//projects/batfish",
        "//projects/batfish:batfish_testlib",
        "//projects/batfish-common-protocol:common",
        "//projects/batfish-common-protocol/src/test/java/org/batfish/datamodel/matchers",
        "//projects/question",
        "@maven//:com_google_guava_guava",
        "@maven//:junit_junit",
        "@maven//:org_hamcrest_hamcrest",
    ],
)

junit_tests(
    name = "SpecifiersReachabilityTest",
    size = "small",
    srcs = [
        "src/test/java/org/batfish/question/specifiers/SpecifiersReachabilityTest.java",
    ],
    resources = glob(["src/test/resources/org/batfish/allinone/testrigs/specifiers-reachability/**"]),
    deps = [
        "//projects/batfish",
        "//projects/batfish:batfish_testlib",
        "//projects/batfish-common-protocol:common",
        "//projects/batfish-common-protocol/src/test/java/org/batfish/datamodel/matchers",
        "//projects/question",
        "@maven//:com_google_guava_guava",
        "@maven//:junit_junit",
        "@maven//:org_hamcrest_hamcrest",
    ],
)

junit_tests(
    name = "DifferentialReachabilityTest",
    size = "small",
    srcs = [
        "src/test/java/org/batfish/question/differentialreachability/DifferentialReachabilityTest.java",
    ],
    deps = [
        "//projects/batfish",
        "//projects/batfish:batfish_testlib",
        "//projects/batfish-common-protocol:common",
        "//projects/batfish-common-protocol/src/test/java/org/batfish/datamodel/matchers",
        "//projects/question",
        "@maven//:com_google_guava_guava",
        "@maven//:junit_junit",
        "@maven//:org_hamcrest_hamcrest",
    ],
)

junit_tests(
    name = "TracerouteAnswererHelperTest",
    size = "small",
    srcs = [
        "src/test/java/org/batfish/question/traceroute/TracerouteAnswererHelperTest.java",
    ],
    resources = glob(["src/test/resources/org/batfish/allinone/testrigs/specifiers-reachability/**"]),
    deps = [
        "//projects/batfish:batfish_testlib",
        "//projects/batfish-common-protocol:common",
        "//projects/batfish-common-protocol/src/test/java/org/batfish/datamodel/matchers",
        "//projects/question",
        "@maven//:com_google_guava_guava",
        "@maven//:junit_junit",
        "@maven//:org_hamcrest_hamcrest",
    ],
)

junit_tests(
    name = "TracerouteTest",
    size = "small",
    srcs = [
        "src/test/java/org/batfish/question/traceroute/TracerouteTest.java",
    ],
    resources = glob(["src/test/resources/org/batfish/allinone/testrigs/specifiers-reachability/**"]),
    deps = [
        "//projects/batfish",
        "//projects/batfish:batfish_testlib",
        "//projects/batfish-common-protocol:common",
        "//projects/batfish-common-protocol/src/test/java/org/batfish/datamodel/matchers",
        "//projects/question",
        "@maven//:com_google_guava_guava",
        "@maven//:junit_junit",
        "@maven//:org_hamcrest_hamcrest",
    ],
)

junit_tests(
    name = "TraceroutePolicyBasedRoutingTest",
    size = "small",
    srcs = [
        "src/test/java/org/batfish/question/traceroute/TraceroutePolicyBasedRoutingTest.java",
    ],
    deps = [
        "//projects/batfish",
        "//projects/batfish:batfish_testlib",
        "//projects/batfish-common-protocol:common",
        "//projects/batfish-common-protocol/src/test/java/org/batfish/datamodel/matchers",
        "//projects/question",
        "@maven//:com_google_guava_guava",
        "@maven//:junit_junit",
        "@maven//:org_hamcrest_hamcrest",
    ],
)

junit_tests(
    name = "smt_tests",
    size = "small",
    srcs = glob([
        "src/test/java/org/batfish/minesweeper/smt/*.java",
    ]),
    tags = ["z3"],
    deps = [
        "//projects/batfish",
        "//projects/batfish:batfish_testlib",
        "//projects/batfish-common-protocol:common",
        "//projects/minesweeper",
        "//projects/minesweeper:matchers",
        "@maven//:com_google_guava_guava",
        "@maven//:junit_junit",
        "@maven//:org_hamcrest_hamcrest",
    ],
)

pmd_test(
    name = "pmd",
    lib = ":allinone",
)
