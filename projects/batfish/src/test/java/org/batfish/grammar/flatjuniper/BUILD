load("@rules_java//java:defs.bzl", "java_library")
load("@batfish//skylark:junit.bzl", "junit_tests")

package(
    default_testonly = True,
    default_visibility = ["//visibility:public"],
)

java_library(
    name = "testlib",
    srcs = [
        "FlatJuniperRecoveryExtractor.java",
    ],
    deps = [
        "//projects/batfish/src/main/antlr4/org/batfish/grammar/flatjuniper",
        "@maven//:org_antlr_antlr4_runtime",
    ],
)

junit_tests(
    name = "tests",
    srcs = glob([
        "**/*Test.java",
    ]),
    resources = [
        "//projects/batfish/src/test/resources/org/batfish/grammar/juniper/testconfigs",
        "//projects/batfish/src/test/resources/org/batfish/grammar/juniper/testrigs",
    ],
    tags = ["cpu:4"],
    deps = [
        ":testlib",
        "//projects/batfish",
        "//projects/batfish:batfish_testlib",
        "//projects/batfish-common-protocol:common",
        "//projects/batfish-common-protocol/src/test/java/org/batfish/common/matchers",
        "//projects/batfish-common-protocol/src/test/java/org/batfish/datamodel/matchers",
        "//projects/batfish/src/main/antlr4/org/batfish/grammar/flatjuniper",
        "//projects/batfish/src/main/java/org/batfish/grammar/flatjuniper",
        "//projects/batfish/src/main/java/org/batfish/representation/juniper",
        "@maven//:com_google_guava_guava",
        "@maven//:junit_junit",
        "@maven//:org_antlr_antlr4_runtime",
        "@maven//:org_apache_commons_commons_lang3",
        "@maven//:org_hamcrest_hamcrest",
    ],
)
