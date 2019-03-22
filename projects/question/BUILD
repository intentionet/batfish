package(default_visibility = ["//visibility:public"])

load("@batfish//skylark:junit.bzl", "junit_tests")

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
        "//projects/lib/jsonpath",
        "@auto_service//:compile",
        "@commons_lang3//:compile",
        "@jackson_annotations//:compile",
        "@jackson_core//:compile",
        "@jackson_databind//:compile",
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
        "@guava//:compile",
        "@hamcrest//:compile",
        "@jsr305//:compile",
        "@junit//:compile",
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
        "@slf4j_jdk14//:runtime",
    ],
    deps = [
        ":question",
        ":question_testlib",
        "//projects/batfish-common-protocol:common",
        "//projects/batfish-common-protocol:common_testlib",
        "//projects/bdd",
        "//projects/lib/jsonpath",
        "@auto_service//:compile",
        "@commons_lang3//:compile",
        "@guava//:compile",
        "@guava_testlib//:compile",
        "@hamcrest//:compile",
        "@jackson_databind//:compile",
        "@junit//:compile",
    ],
)
