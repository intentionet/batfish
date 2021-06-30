load("@batfish//skylark:junit.bzl", "junit_tests")

package(
    default_testonly = True,
    default_visibility = ["//visibility:private"],
)

junit_tests(
    name = "tests",
    srcs = glob([
        "**/*Test.java",
    ]),
    runtime_deps = [
        "@maven//:org_apache_logging_log4j_log4j_core",
        "@maven//:org_apache_logging_log4j_log4j_slf4j_impl",
    ],
    deps = [
        "//projects/batfish-common-protocol:common",
        "//projects/batfish-common-protocol:common_testlib",
        "//projects/batfish-common-protocol/src/test/java/org/batfish/datamodel/matchers",
        "//projects/question",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
        "@maven//:javax_annotation_javax_annotation_api",
        "@maven//:junit_junit",
        "@maven//:org_hamcrest_hamcrest",
    ],
)
