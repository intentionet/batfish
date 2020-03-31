package(
    default_testonly = True,
    default_visibility = ["//visibility:private"],
)

load("@batfish//skylark:junit.bzl", "junit_tests")

junit_tests(
    name = "tests",
    srcs = glob(["*Test.java"]),
    deps = [
        "//projects/batfish-common-protocol:common",
        "//projects/batfish-common-protocol/src/test/java/org/batfish/datamodel/matchers",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_guava_guava_testlib",
        "@maven//:junit_junit",
        "@maven//:org_hamcrest_hamcrest",
    ],
)
