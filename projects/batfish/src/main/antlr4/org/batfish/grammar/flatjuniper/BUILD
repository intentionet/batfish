package(default_visibility = ["//visibility:public"])

java_library(
    name = "flatjuniper",
    srcs = [
        ":FlatJuniperLexer.java",
        ":FlatJuniperParser.java",
        ":FlatJuniperParserBaseListener.java",
        ":FlatJuniperParserListener.java",
    ],
    deps = [
        "//projects/batfish-common-protocol:common",
        "@antlr4//:compile",
    ],
)

genrule(
    name = "flatjuniper_generated",
    srcs = glob([
        "*.g4",
    ]),
    outs = [
        "FlatJuniperLexer.java",
        "FlatJuniperParser.java",
        "FlatJuniperParserBaseListener.java",
        "FlatJuniperParserListener.java",
    ],
    cmd = """
java -cp $(location //third_party/org/antlr4/antlr4-complete) \
    org.antlr.v4.Tool \
    -Xexact-output-dir \
       $(location FlatJuniperLexer.g4) \
       $(location FlatJuniperParser.g4) \
    -package org.batfish.grammar.flatjuniper \
    -encoding UTF-8 \
    -o $(@D)""",
    tools = ["//third_party/org/antlr4/antlr4-complete"],
)
