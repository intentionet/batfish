load("@rules_java//java:defs.bzl", "java_library")
load("@batfish//skylark:junit.bzl", "junit_tests")
load("@batfish//skylark:pmd_test.bzl", "pmd_test")

package(default_visibility = ["//visibility:public"])

java_library(
    name = "question",
    srcs = glob([
        "src/main/**/*.java",
    ]),
    plugins = [
        "//:auto_service_plugin",
    ],
    resources = glob(["src/main/resources/**"]),
    runtime_deps = [
    ],
    deps = [
        "//projects/batfish-common-protocol:common",
        "//projects/bdd",
        "@maven//:com_fasterxml_jackson_core_jackson_annotations",
        "@maven//:com_fasterxml_jackson_core_jackson_core",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_google_auto_service_auto_service_annotations",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
        "@maven//:org_apache_commons_commons_lang3",
    ],
)

java_library(
    name = "question_testlib",
    testonly = True,
    srcs = glob(
        [
            "src/test/java/**/*.java",
        ],
        exclude = ["src/test/java/**/*Test.java"],
    ),
    deps = [
        ":question",
        "//projects/batfish-common-protocol:common",
        "//projects/batfish-common-protocol:common_testlib",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
        "@maven//:junit_junit",
        "@maven//:org_hamcrest_hamcrest",
    ],
)

junit_tests(
    name = "question_tests",
    size = "small",
    srcs = glob([
        "src/test/java/**/*Test.java",
    ]),
    plugins = [
        "//:auto_service_plugin",
    ],
    resources = glob([
        "src/test/resources/**",
    ]),
    runtime_deps = [
        "@maven//:org_apache_logging_log4j_log4j_slf4j_impl",
    ],
    deps = [
        ":question",
        ":question_testlib",
        "//projects/batfish-common-protocol:common",
        "//projects/batfish-common-protocol:common_testlib",
        "//projects/batfish-common-protocol/src/test/java/org/batfish/datamodel/matchers",
        "//projects/bdd",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
        "@maven//:junit_junit",
        "@maven//:org_hamcrest_hamcrest",
    ],
)

pmd_test(
    name = "pmd",
    lib = ":question",
)
