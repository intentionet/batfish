package(default_visibility = ["//visibility:public"])

java_library(
    name = "eos",
    srcs = [
        ":EosRoutingTableLexer.java",
        ":EosRoutingTableParser.java",
        ":EosRoutingTableParserBaseListener.java",
        ":EosRoutingTableParserListener.java",
    ],
    deps = [
        "//projects/batfish-common-protocol:parser_common",
        "@antlr4_runtime//:compile",
    ],
)

genrule(
    name = "eos_generated",
    srcs = glob([
        "*.g4",
    ]),
    outs = [
        "EosRoutingTableLexer.java",
        "EosRoutingTableParser.java",
        "EosRoutingTableParserBaseListener.java",
        "EosRoutingTableParserListener.java",
    ],
    cmd = """
java -cp $(location //third_party/org/antlr4/antlr4-complete) \
    org.antlr.v4.Tool \
    -Xexact-output-dir \
       $(location EosRoutingTableLexer.g4) \
       $(location EosRoutingTableParser.g4) \
    -package org.batfish.grammar.routing_table.eos \
    -encoding UTF-8 \
    -Werror \
    -o $(@D)""",
    tools = ["//third_party/org/antlr4/antlr4-complete"],
)
