package(default_visibility = ["//visibility:public"])

load("//skylark:junit.bzl", "junit_tests")

java_binary(
    name = "coordinator_main",
    main_class = "org.batfish.coordinator.Main",
    runtime_deps = [
        ":coordinator",
        "@slf4j_jdk14//:runtime",
    ],
)

java_library(
    name = "coordinator",
    srcs = glob([
        "src/main/**/*.java",
    ]),
    resources = glob(["src/main/resources/**"]),
    runtime_deps = [
        "//projects/question",
    ],
    deps = [
        "//projects/batfish-common-protocol:common",
        "//projects/question",
        "@azure_storage//:compile",
        "@commons_io//:compile",
        "@commons_lang3//:compile",
        "@grizzly_server//:compile",
        "@guava//:compile",
        "@jackson_core//:compile",
        "@jackson_databind//:compile",
        "@jaeger_core//:compile",
        "@javax_annotation//:compile",
        "@jaxrs_api//:compile",
        "@jersey_common//:compile",
        "@jersey_container_grizzly2//:compile",
        "@jersey_media_jackson//:compile",
        "@jersey_media_jettison//:compile",
        "@jersey_media_multipart//:compile",
        "@jersey_server//:compile",
        "@jettison//:compile",
        "@jsr305//:compile",
        "@opentracing_api//:compile",
        "@opentracing_contrib_jaxrs//:compile",
        "@opentracing_util//:compile",
    ],
)

java_library(
    name = "coordinator_testlib",
    testonly = True,
    srcs = glob(
        [
            "src/test/**/*.java",
        ],
        exclude = ["src/test/**/*Test.java"],
    ),
    resources = glob(["src/test/resources/**"]),
    deps = [
        ":coordinator",
    ],
)

junit_tests(
    name = "coordinator_tests",
    srcs = glob([
        "src/test/java/**/*Test.java",
    ]),
    runtime_deps = [
        "@jersey_test_framework_grizzly2//:compile",
        "@sqlite_jdbc//:compile",
    ],
    deps = [
        ":coordinator",
        ":coordinator_testlib",
        "//projects/batfish-common-protocol:common",
        "//projects/question",
        "@commons_io//:compile",
        "@guava//:compile",
        "@hamcrest//:compile",
        "@jackson_core//:compile",
        "@jaxrs_api//:compile",
        "@jersey_client//:compile",
        "@jersey_media_jackson//:compile",
        "@jersey_server//:compile",
        "@jersey_test_framework//:compile",
        "@jettison//:compile",
        "@junit//:compile",
    ],
    size = "small",
)
