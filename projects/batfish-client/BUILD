package(default_visibility = ["//visibility:public"])

load("//skylark:junit.bzl", "junit_tests")

java_binary(
    name = "client_main",
    main_class = "org.batfish.client.Main",
    runtime_deps = [
        ":client",
        "@slf4j_jdk14//:runtime",
    ],
)

java_library(
    name = "client",
    srcs = glob([
        "src/main/**/*.java",
    ]),
    resources = glob(["src/main/resources/**"]),
    runtime_deps = [
        "//projects/question",
    ],
    deps = [
        "//projects/batfish-common-protocol:common",
        "//projects/lib/jackson-jsonschema:jackson_jsonschema",
        "@commons_io//:compile",
        "@commons_lang3//:compile",
        "@guava//:compile",
        "@jackson_annotations//:compile",
        "@jackson_core//:compile",
        "@jackson_databind//:compile",
        "@jaeger_core//:compile",
        "@jaxrs_api//:compile",
        "@jersey_media_multipart//:compile",
        "@jettison//:compile",
        "@jline3//:compile",
        "@jsr305//:compile",
        "@opentracing_api//:compile",
        "@opentracing_util//:compile",
        "@scala_library//:compile",
    ],
)

java_library(
    name = "client_testlib",
    testonly = True,
    srcs = glob(
        [
            "src/test/java/**/*.java",
        ],
        exclude = ["src/test/java/**/*Test.java"],
    ),
    deps = [
        ":client",
        "//projects/batfish-common-protocol:common",
        "@guava//:compile",
        "@jackson_annotations//:compile",
    ],
)

junit_tests(
    name = "client_tests",
    srcs = glob([
        "src/test/java/**/*Test.java",
    ]),
    resources = glob([
        "src/test/resources/**",
    ]),
    deps = [
        ":client",
        ":client_testlib",
        "//projects/batfish-common-protocol:common",
        "@commons_lang3//:compile",
        "@guava//:compile",
        "@jackson_databind//:compile",
        "@jettison//:compile",
    ],
    size = "small",
)
