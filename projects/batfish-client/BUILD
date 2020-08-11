load("@rules_java//java:defs.bzl", "java_binary", "java_library")
load("@batfish//skylark:junit.bzl", "junit_tests")
load("@batfish//skylark:pmd_test.bzl", "pmd_test")

package(default_visibility = ["//visibility:public"])

java_binary(
    name = "client_main",
    main_class = "org.batfish.client.Main",
    runtime_deps = [
        ":client",
        "@maven//:io_jaegertracing_jaeger_thrift",
        "@maven//:org_apache_logging_log4j_log4j_slf4j_impl",
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
        "@maven//:com_fasterxml_jackson_core_jackson_annotations",
        "@maven//:com_fasterxml_jackson_core_jackson_core",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
        "@maven//:commons_io_commons_io",
        "@maven//:io_github_java_diff_utils_java_diff_utils",
        "@maven//:io_jaegertracing_jaeger_core",
        "@maven//:io_opentracing_opentracing_api",
        "@maven//:io_opentracing_opentracing_util",
        "@maven//:javax_ws_rs_javax_ws_rs_api",
        "@maven//:org_apache_commons_commons_lang3",
        "@maven//:org_codehaus_jettison_jettison",
        "@maven//:org_glassfish_jersey_media_jersey_media_multipart",
        "@maven//:org_jline_jline",
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
        "//projects/batfish-common-protocol:common",
        "@maven//:com_fasterxml_jackson_core_jackson_annotations",
        "@maven//:com_google_guava_guava",
    ],
)

junit_tests(
    name = "client_tests",
    size = "small",
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
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_google_guava_guava",
        "@maven//:io_github_java_diff_utils_java_diff_utils",
        "@maven//:junit_junit",
        "@maven//:org_apache_commons_commons_lang3",
        "@maven//:org_codehaus_jettison_jettison",
        "@maven//:org_hamcrest_hamcrest",
    ],
)

pmd_test(
    name = "pmd",
    lib = ":client",
)
