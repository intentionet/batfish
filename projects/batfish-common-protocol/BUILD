package(default_visibility = ["//visibility:public"])

load("@batfish//skylark:junit.bzl", "junit_tests")

filegroup(
    name = "parser_files",
    srcs = [
        "src/main/java/org/batfish/common/BatfishException.java",
        "src/main/java/org/batfish/common/DebugBatfishException.java",
        "src/main/java/org/batfish/common/ParseTreeSentences.java",
        "src/main/java/org/batfish/common/util/BatfishObjectMapper.java",
        "src/main/java/org/batfish/datamodel/answers/AnswerElement.java",
        "src/main/java/org/batfish/datamodel/answers/AnswerSummary.java",
        "src/main/java/org/batfish/grammar/BatfishANTLRErrorStrategy.java",
        "src/main/java/org/batfish/grammar/BatfishCombinedParser.java",
        "src/main/java/org/batfish/grammar/BatfishGrammarErrorListener.java",
        "src/main/java/org/batfish/grammar/BatfishLexer.java",
        "src/main/java/org/batfish/grammar/BatfishLexerErrorListener.java",
        "src/main/java/org/batfish/grammar/BatfishLexerRecoveryStrategy.java",
        "src/main/java/org/batfish/grammar/BatfishParser.java",
        "src/main/java/org/batfish/grammar/BatfishParserATNSimulator.java",
        "src/main/java/org/batfish/grammar/BatfishParserErrorListener.java",
        "src/main/java/org/batfish/grammar/GrammarSettings.java",
        "src/main/java/org/batfish/grammar/ParseTreePrettyPrinter.java",
    ],
)

java_library(
    name = "parser_common",
    srcs = [":parser_files"],
    deps = [
        "@antlr4_runtime//:compile",
        "@guava//:compile",
        "@jackson_annotations//:compile",
        "@jackson_core//:compile",
        "@jackson_guava//:compile",
        "@jackson_jdk8//:compile",
        "@jackson_jsr310//:compile",
    ],
)

java_library(
    name = "common",
    srcs = glob([
        "src/main/**/*.java",
    ]),
    resources = [
        "src/generated/resources/org/batfish/common/common.properties",
        "src/main/resources/org/batfish/common/util/salt",
    ],
    plugins = [
        "//:auto_service_plugin",
    ],
    runtime_deps = [
        "@commons_beanutils//:runtime",
    ],
    deps = [
        ":parser_common",
        "//projects/lib/jackson-jsonschema:jackson_jsonschema",
        "//projects/lib/jsonpath",
        "//projects/lib/z3",
        "@antlr4_runtime//:compile",
        "@auto_service//:compile",
        "@commons_cli//:compile",
        "@commons_configuration2//:compile",
        "@commons_io//:compile",
        "@commons_lang3//:compile",
        "@errorprone_annotations//:compile",
        "@grizzly_framework//:compile",
        "@grizzly_server//:compile",
        "@guava//:compile",
        "@jackson_annotations//:compile",
        "@jackson_core//:compile",
        "@jackson_databind//:compile",
        "@jackson_guava//:compile",
        "@jackson_jdk8//:compile",
        "@jackson_jsr310//:compile",
        "@jaxrs_api//:compile",
        "@jersey_container_grizzly2//:compile",
        "@jersey_server//:compile",
        "@jettison//:compile",
        "@jsonassert//:compile",
        "@jsr305//:compile",
        "@lz4//:compile",
        "@maven_artifact//:compile",
        "@opentracing_api//:compile",
        "@opentracing_contrib_jaxrs//:compile",
        "@opentracing_util//:compile",
        "@xstream//:compile",
    ],
)

java_library(
    name = "common_testlib",
    testonly = True,
    srcs = glob(
        [
            "src/test/java/**/*.java",
        ],
        exclude = ["src/test/java/**/*Test.java"],
    ),
    deps = [
        ":common",
        ":recovery_grammar",
        "@antlr4_runtime//:compile",
        "@commons_lang3//:compile",
        "@guava//:compile",
        "@hamcrest//:compile",
        "@jackson_core//:compile",
        "@jsr305//:compile",
        "@junit//:compile",
    ],
)

junit_tests(
    name = "common_tests",
    size = "small",
    srcs = glob([
        "src/test/java/**/*Test.java",
    ]),
    resources = glob([
        "src/test/resources/**",
    ]),
    deps = [
        ":common",
        ":common_testlib",
        ":recovery_grammar",
        "@antlr4_runtime//:compile",
        "@guava//:compile",
        "@guava_testlib//:compile",
        "@hamcrest//:compile",
        "@jackson_annotations//:compile",
        "@jackson_core//:compile",
        "@jackson_databind//:compile",
        "@opentracing_api//:compile",
        "@opentracing_mock//:compile",
        "@opentracing_util//:compile",
    ],
)

java_library(
    name = "recovery_grammar",
    srcs = [
        ":RecoveryLexer.java",
        ":RecoveryParser.java",
        ":RecoveryParserBaseListener.java",
        ":RecoveryParserListener.java",
    ],
    deps = [
        ":common",
        "@antlr4_runtime//:compile",
    ],
)

genrule(
    name = "recovery_grammar_src",
    srcs = glob([
        "src/test/antlr4/org/batfish/grammar/recovery/*.g4",
    ]),
    outs = [
        "RecoveryLexer.java",
        "RecoveryParser.java",
        "RecoveryParserBaseListener.java",
        "RecoveryParserListener.java",
    ],
    cmd = """
java -cp $(location //third_party/org/antlr4/antlr4-complete) \
    org.antlr.v4.Tool \
    -Xexact-output-dir \
       $(location src/test/antlr4/org/batfish/grammar/recovery/RecoveryLexer.g4) \
       $(location src/test/antlr4/org/batfish/grammar/recovery/RecoveryParser.g4) \
    -package org.batfish.grammar.recovery \
    -encoding UTF-8 \
    -Werror \
    -o $(@D)""",
    tools = ["//third_party/org/antlr4/antlr4-complete"],
)

genrule(
    name = "common_properties",
    srcs = [
        "src/main/resources/org/batfish/common/common.properties",
        "//projects:pom.xml",
    ],
    outs = [
        "src/generated/resources/org/batfish/common/common.properties",
    ],
    cmd = """
BATFISH_VERSION="$$(grep -1 batfish-parent $(location //projects:pom.xml) | grep version | sed 's/[<>]/|/g' | cut -f3 -d\|)"
sed s/\$${project.version}/$${BATFISH_VERSION}/ $(location src/main/resources/org/batfish/common/common.properties) > "$@"
""",
)
