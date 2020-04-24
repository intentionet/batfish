load("@rules_java//java:defs.bzl", "java_library")

package(default_visibility = ["//visibility:public"])

load("@batfish//skylark:pmd_test.bzl", "pmd_test")

java_library(
    name = "f5_bigip_structured",
    srcs = glob(
        ["**/*.java"],
        exclude = ["BUILD"],
    ),
    deps = [
        "//projects/batfish-common-protocol:common",
        "//projects/batfish/src/main/antlr4/org/batfish/grammar/f5_bigip_structured",
        "//projects/batfish/src/main/java/org/batfish/grammar/f5_bigip_imish",
        "//projects/batfish/src/main/java/org/batfish/representation/f5_bigip",
        "@maven//:com_fasterxml_jackson_core_jackson_core",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
        "@maven//:org_antlr_antlr4_runtime",
    ],
)

pmd_test(
    name = "pmd",
    lib = ":f5_bigip_structured",
)
