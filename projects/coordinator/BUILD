load("@rules_java//java:defs.bzl", "java_binary", "java_library")
load("@batfish//skylark:junit.bzl", "junit_tests")
load("@batfish//skylark:pmd_test.bzl", "pmd_test")

package(default_visibility = ["//visibility:public"])

java_binary(
    name = "coordinator_main",
    main_class = "org.batfish.coordinator.Main",
    runtime_deps = [
        ":coordinator",
        "@maven//:org_apache_logging_log4j_log4j_slf4j_impl",
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
        "@maven//:io_jaegertracing_jaeger_thrift",
        "@maven//:javax_activation_activation",
        "@maven//:javax_xml_bind_jaxb_api",
        "@maven//:org_glassfish_jersey_inject_jersey_hk2",
    ],
    deps = [
        "//projects/batfish-common-protocol:common",
        "@maven//:com_fasterxml_jackson_core_jackson_annotations",
        "@maven//:com_fasterxml_jackson_core_jackson_core",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_errorprone_error_prone_annotations",
        "@maven//:com_google_guava_guava",
        "@maven//:commons_io_commons_io",
        "@maven//:io_jaegertracing_jaeger_core",
        "@maven//:io_opentracing_contrib_opentracing_jaxrs2",
        "@maven//:io_opentracing_opentracing_api",
        "@maven//:io_opentracing_opentracing_util",
        "@maven//:javax_annotation_javax_annotation_api",
        "@maven//:javax_ws_rs_javax_ws_rs_api",
        "@maven//:org_codehaus_jettison_jettison",
        "@maven//:org_glassfish_grizzly_grizzly_http_server",
        "@maven//:org_glassfish_jersey_containers_jersey_container_grizzly2_http",
        "@maven//:org_glassfish_jersey_core_jersey_common",
        "@maven//:org_glassfish_jersey_core_jersey_server",
        "@maven//:org_glassfish_jersey_media_jersey_media_json_jackson",
        "@maven//:org_glassfish_jersey_media_jersey_media_json_jettison",
        "@maven//:org_glassfish_jersey_media_jersey_media_multipart",
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
        "//projects/batfish-common-protocol:common_testlib",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
        "@maven//:javax_ws_rs_javax_ws_rs_api",
        "@maven//:junit_junit",
        "@maven//:org_glassfish_grizzly_grizzly_http_server",
        "@maven//:org_glassfish_jersey_core_jersey_client",
        "@maven//:org_glassfish_jersey_core_jersey_server",
        "@maven//:org_glassfish_jersey_media_jersey_media_json_jackson",
        "@maven//:org_glassfish_jersey_test_framework_jersey_test_framework_core",
        "@maven//:org_glassfish_jersey_test_framework_providers_jersey_test_framework_provider_grizzly2",
        "@maven//:org_hamcrest_hamcrest",
    ],
)

junit_tests(
    name = "coordinator_tests",
    size = "medium",
    srcs = glob([
        "src/test/java/**/*Test.java",
    ]),
    runtime_deps = [
        "@maven//:org_xerial_sqlite_jdbc",
    ],
    deps = [
        ":coordinator",
        ":coordinator_testlib",
        "//projects/batfish-common-protocol:common",
        "//projects/batfish-common-protocol:common_testlib",
        "@maven//:com_fasterxml_jackson_core_jackson_core",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_guava_guava_testlib",
        "@maven//:commons_io_commons_io",
        "@maven//:javax_ws_rs_javax_ws_rs_api",
        "@maven//:junit_junit",
        "@maven//:org_codehaus_jettison_jettison",
        "@maven//:org_glassfish_jersey_core_jersey_client",
        "@maven//:org_glassfish_jersey_core_jersey_server",
        "@maven//:org_glassfish_jersey_media_jersey_media_json_jettison",
        "@maven//:org_glassfish_jersey_media_jersey_media_multipart",
        "@maven//:org_glassfish_jersey_test_framework_jersey_test_framework_core",
        "@maven//:org_hamcrest_hamcrest",
    ],
)

pmd_test(
    name = "pmd",
    lib = ":coordinator",
)
