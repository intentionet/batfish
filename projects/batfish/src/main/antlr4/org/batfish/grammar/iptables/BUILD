package(default_visibility = ["//visibility:public"])

java_library(
    name = "iptables",
    srcs = [
        ":IptablesLexer.java",
        ":IptablesParser.java",
        ":IptablesParserBaseListener.java",
        ":IptablesParserListener.java",
    ],
    deps = [
        "//projects/batfish-common-protocol:parser_common",
        "@antlr4_runtime//:compile",
    ],
)

genrule(
    name = "iptables_generated",
    srcs = glob([
        "*.g4",
    ]),
    outs = [
        "IptablesLexer.java",
        "IptablesParser.java",
        "IptablesParserBaseListener.java",
        "IptablesParserListener.java",
    ],
    cmd = """
java -cp $(location //third_party/org/antlr4/antlr4-complete) \
    org.antlr.v4.Tool \
    -Xexact-output-dir \
       $(location IptablesLexer.g4) \
       $(location IptablesParser.g4) \
    -package org.batfish.grammar.iptables \
    -encoding UTF-8 \
    -Werror \
    -o $(@D)""",
    tools = ["//third_party/org/antlr4/antlr4-complete"],
)
