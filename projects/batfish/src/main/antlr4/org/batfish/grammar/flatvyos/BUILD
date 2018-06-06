package(default_visibility = ["//visibility:public"])

java_library(
    name = "flatvyos",
    srcs = [
        ":FlatVyosLexer.java",
        ":FlatVyosParser.java",
        ":FlatVyosParserBaseListener.java",
        ":FlatVyosParserListener.java",
    ],
    deps = [
        "//projects/batfish-common-protocol:parser_common",
        "@antlr4_runtime//:compile",
    ],
)

genrule(
    name = "flatvyos_generated",
    srcs = glob([
        "*.g4",
    ]),
    outs = [
        "FlatVyosLexer.java",
        "FlatVyosParser.java",
        "FlatVyosParserBaseListener.java",
        "FlatVyosParserListener.java",
    ],
    cmd = """
java -cp $(location //third_party/org/antlr4/antlr4-complete) \
    org.antlr.v4.Tool \
    -Xexact-output-dir \
       $(location FlatVyosLexer.g4) \
       $(location FlatVyosParser.g4) \
    -package org.batfish.grammar.flatvyos \
    -encoding UTF-8 \
    -Werror \
    -o $(@D)""",
    tools = ["//third_party/org/antlr4/antlr4-complete"],
)
