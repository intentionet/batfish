package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache-2.0

java_import(
    name = "jsonpath",
    jars = ["json-path-2.4.0-batfish-internal.jar"],
    srcjar = "json-path-2.4.0-batfish-internal-sources.jar",
    runtime_deps = [
        "@jackson_databind//:compile",
        "@json_smart//:compile",
        "@slf4j_api//:compile",
    ],
)
