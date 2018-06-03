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
        "//projects/lib/jsonpath",
        "@auto_service//:compile",
        "@commons_lang3//:compile",
        "@jackson_annotations//:compile",
        "@jackson_core//:compile",
        "@jackson_databind//:compile",
    ],
)

junit_tests(
    name = "question_tests",
    srcs = glob([
        "src/test/java/**/*Test.java",
    ]),
    resources = glob([
        "src/test/resources/**",
    ]),
    runtime_deps = [
        "@slf4j_jdk14//:runtime",
    ],
    deps = [
        ":question",
        "//projects/batfish-common-protocol:common",
        "//projects/lib/jsonpath",
        "@guava//:compile",
        "@hamcrest//:compile",
        "@jackson_databind//:compile",
        "@junit//:compile",
    ],
    size = "small",
)
