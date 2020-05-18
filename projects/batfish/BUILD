load("@rules_java//java:defs.bzl", "java_binary", "java_library")

package(default_visibility = ["//visibility:public"])

load("@batfish//skylark:junit.bzl", "junit_tests")

java_binary(
    name = "batfish_main",
    main_class = "org.batfish.main.Driver",
    runtime_deps = [
        ":batfish",
    ],
)

java_library(
    name = "batfish",
    srcs = glob([
        "src/main/**/*.java",
    ]),
    plugins = [
        "//:auto_service_plugin",
    ],
    resources = [
        "src/main/resources/org/batfish/config/batfish.properties",
        "src/main/resources/org/batfish/representation/aws/ip-ranges.json",
    ],
    runtime_deps = [
        "//projects/question",
        "@maven//:io_jaegertracing_jaeger_thrift",
    ],
    deps = [
        "//projects/batfish-common-protocol:common",
        "//projects/batfish/src/main/antlr4/org/batfish/grammar/flatvyos",
        "//projects/batfish/src/main/antlr4/org/batfish/grammar/iptables",
        "//projects/batfish/src/main/antlr4/org/batfish/grammar/mrv",
        "//projects/batfish/src/main/antlr4/org/batfish/grammar/palo_alto_nested",
        "//projects/batfish/src/main/antlr4/org/batfish/grammar/routing_table/eos",
        "//projects/batfish/src/main/antlr4/org/batfish/grammar/routing_table/ios",
        "//projects/batfish/src/main/antlr4/org/batfish/grammar/routing_table/nxos",
        "//projects/batfish/src/main/antlr4/org/batfish/grammar/vyos",
        "//projects/batfish/src/main/java/org/batfish/grammar/arista",
        "//projects/batfish/src/main/java/org/batfish/grammar/cisco",
        "//projects/batfish/src/main/java/org/batfish/grammar/cisco_nxos",
        "//projects/batfish/src/main/java/org/batfish/grammar/cisco_xr",
        "//projects/batfish/src/main/java/org/batfish/grammar/cumulus_concatenated",
        "//projects/batfish/src/main/java/org/batfish/grammar/cumulus_nclu",
        "//projects/batfish/src/main/java/org/batfish/grammar/f5_bigip_structured",
        "//projects/batfish/src/main/java/org/batfish/grammar/flatjuniper",
        "//projects/batfish/src/main/java/org/batfish/grammar/juniper",
        "//projects/batfish/src/main/java/org/batfish/grammar/palo_alto",
        "//projects/batfish/src/main/java/org/batfish/representation/aws",
        "//projects/bdd",
        "//projects/symbolic",
        "@maven//:com_fasterxml_jackson_core_jackson_annotations",
        "@maven//:com_fasterxml_jackson_core_jackson_core",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_google_auto_service_auto_service_annotations",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
        "@maven//:commons_io_commons_io",
        "@maven//:io_jaegertracing_jaeger_core",
        "@maven//:io_opentracing_contrib_opentracing_jaxrs2",
        "@maven//:io_opentracing_opentracing_api",
        "@maven//:io_opentracing_opentracing_util",
        "@maven//:javax_ws_rs_javax_ws_rs_api",
        "@maven//:org_antlr_antlr4_runtime",
        "@maven//:org_apache_commons_commons_collections4",
        "@maven//:org_apache_commons_commons_configuration2",
        "@maven//:org_apache_commons_commons_lang3",
        "@maven//:org_codehaus_jettison_jettison",
        "@maven//:org_glassfish_grizzly_grizzly_http_server",
        "@maven//:org_glassfish_jersey_containers_jersey_container_grizzly2_http",
        "@maven//:org_glassfish_jersey_core_jersey_server",
        "@maven//:org_glassfish_jersey_media_jersey_media_json_jettison",
        "@maven//:org_jgrapht_jgrapht_core",
    ],
)

java_library(
    name = "batfish_testlib",
    testonly = True,
    srcs = glob(
        [
            "src/test/**/*.java",
        ],
        exclude = ["src/test/**/*Test.java"],
    ),
    deps = [
        ":batfish",
        "//projects/batfish-common-protocol:common",
        "//projects/batfish-common-protocol/src/test/java/org/batfish/datamodel/matchers",
        "//projects/bdd",
        "//projects/symbolic",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
        "@maven//:junit_junit",
        "@maven//:org_apache_commons_commons_collections4",
        "@maven//:org_hamcrest_hamcrest",
    ],
)

junit_tests(
    name = "batfish_tests",
    size = "medium",
    srcs = glob([
        "src/test/**/*Test.java",
    ]),
    resources = [
        "//projects/batfish/src/test/resources",
    ],
    deps = [
        ":batfish",
        ":batfish_testlib",
        "//projects/batfish-common-protocol:common",
        "//projects/batfish-common-protocol:common_testlib",
        "//projects/batfish-common-protocol/src/test/java/org/batfish/common/bdd:matchers",
        "//projects/batfish-common-protocol/src/test/java/org/batfish/common/matchers",
        "//projects/batfish-common-protocol/src/test/java/org/batfish/datamodel/matchers",
        "//projects/batfish-common-protocol/src/test/java/org/batfish/identifiers",
        "//projects/bdd",
        "//projects/symbolic",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_guava_guava_testlib",
        "@maven//:commons_io_commons_io",
        "@maven//:junit_junit",
        "@maven//:org_hamcrest_hamcrest",
    ],
)

load("@batfish//skylark:pmd_test.bzl", "pmd_test")

pmd_test(
    name = "pmd",
    lib = ":batfish",
)

pmd_test(
    name = "testlib_pmd",
    lib = ":batfish",
)
