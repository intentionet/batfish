package(default_visibility = ["//visibility:public"])

java_library(
    name = "cisco",
    srcs = [
        ":CiscoLexer.java",
        ":CiscoParser.java",
        ":CiscoParserBaseListener.java",
        ":CiscoParserListener.java",
    ],
    deps = [
        "//projects/batfish-common-protocol:parser_common",
        "@antlr4_runtime//:compile",
    ],
)

genrule(
    name = "cisco_generated",
    srcs = glob([
        "*.g4",
    ]),
    outs = [
        "CiscoLexer.java",
        "CiscoParser.java",
        "CiscoParserBaseListener.java",
        "CiscoParserListener.java",
    ],
    cmd = """
java -cp $(location //third_party/org/antlr4/antlr4-complete) \
    org.antlr.v4.Tool \
    -Xexact-output-dir \
       $(location CiscoLexer.g4) \
       $(location CiscoParser.g4) \
    -package org.batfish.grammar.cisco \
    -encoding UTF-8 \
    -Werror \
    -o $(@D)""",
    tools = ["//third_party/org/antlr4/antlr4-complete"],
)
