package(default_visibility = ["//visibility:public"])

load("//skylark:junit.bzl", "junit_tests")

java_binary(
    name = "allinone_main",
    main_class = "org.batfish.allinone.Main",
    runtime_deps = [
        ":allinone",
    ],
)

java_library(
    name = "allinone",
    srcs = glob([
        "src/main/**/*.java",
    ]),
    resources = glob(["src/main/resources/**"]),
    runtime_deps = [
    ],
    deps = [
        "//projects/batfish",
        "//projects/batfish-client:client",
        "//projects/batfish-common-protocol:common",
        "//projects/coordinator",
        "@commons_lang3//:compile",
        "@guava//:compile",
        "@jaeger_core//:compile",
    ],
)

junit_tests(
    name = "allinone_tests",
    size = "small",
    srcs = glob([
        "src/test/**/*Test.java",
    ]),
    deps = [
        ":allinone",
        "//projects/batfish",
        "//projects/batfish:batfish_testlib",
        "//projects/batfish-common-protocol:common",
        "//projects/batfish-common-protocol:common_testlib",
        "//projects/question",
        "@guava//:compile",
        "@hamcrest//:compile",
        "@junit//:compile",
    ],
)
