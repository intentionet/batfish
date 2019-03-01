package(default_visibility = ["//visibility:public"])

load("@batfish//skylark:junit.bzl", "junit_tests")

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
        "@commons_io//:compile",
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
        "//projects/batfish-common-protocol:common",
        "@grizzly_server//:compile",
        "@guava//:compile",
        "@jersey_media_jackson//:compile",
        "@jersey_test_framework//:compile",
        "@jersey_test_framework_grizzly2//:compile",
        "@jsr305//:compile",
    ],
)

junit_tests(
    name = "coordinator_tests",
    size = "medium",
    srcs = glob([
        "src/test/java/**/*Test.java",
    ]),
    runtime_deps = [
        "@sqlite_jdbc//:compile",
    ],
    deps = [
        ":coordinator",
        ":coordinator_testlib",
        "//projects/batfish-common-protocol:common",
        "//projects/batfish-common-protocol:common_testlib",
        "//projects/question",
        "@commons_io//:compile",
        "@guava//:compile",
        "@guava_testlib//:compile",
        "@hamcrest//:compile",
        "@jackson_core//:compile",
        "@jaxrs_api//:compile",
        "@jersey_client//:compile",
        "@jersey_media_jackson//:compile",
        "@jersey_media_jettison//:compile",
        "@jersey_media_multipart//:compile",
        "@jersey_server//:compile",
        "@jersey_test_framework//:compile",
        "@jettison//:compile",
        "@junit//:compile",
    ],
)
