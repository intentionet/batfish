package(default_visibility = ["//visibility:public"])

java_library(
    name = "ios",
    srcs = [
        ":IosRoutingTableLexer.java",
        ":IosRoutingTableParser.java",
        ":IosRoutingTableParserBaseListener.java",
        ":IosRoutingTableParserListener.java",
    ],
    deps = [
        "//projects/batfish-common-protocol:parser_common",
        "@antlr4_runtime//:compile",
    ],
)

genrule(
    name = "ios_generated",
    srcs = glob([
        "*.g4",
    ]),
    outs = [
        "IosRoutingTableLexer.java",
        "IosRoutingTableParser.java",
        "IosRoutingTableParserBaseListener.java",
        "IosRoutingTableParserListener.java",
    ],
    cmd = """
java -cp $(location //third_party/org/antlr4/antlr4-complete) \
    org.antlr.v4.Tool \
    -Xexact-output-dir \
       $(location IosRoutingTableLexer.g4) \
       $(location IosRoutingTableParser.g4) \
    -package org.batfish.grammar.routing_table.ios \
    -encoding UTF-8 \
    -Werror \
    -o $(@D)""",
    tools = ["//third_party/org/antlr4/antlr4-complete"],
)
