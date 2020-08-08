load("@rules_java//java:defs.bzl", "java_library")
load("@batfish//skylark:junit.bzl", "junit_tests")
load("@batfish//skylark:pmd_test.bzl", "pmd_test")

package(default_visibility = ["//visibility:public"])

filegroup(
    name = "parser_files",
    srcs = [
        "src/main/java/org/batfish/common/BatfishException.java",
        "src/main/java/org/batfish/common/DebugBatfishException.java",
        "src/main/java/org/batfish/common/ErrorDetails.java",
        "src/main/java/org/batfish/common/ParseTreeSentences.java",
        "src/main/java/org/batfish/common/WillNotCommitException.java",
        "src/main/java/org/batfish/common/util/BatfishObjectMapper.java",
        "src/main/java/org/batfish/common/util/serialization/BatfishThirdPartyDeserializers.java",
        "src/main/java/org/batfish/common/util/serialization/BatfishThirdPartySerializationModule.java",
        "src/main/java/org/batfish/common/util/serialization/BatfishThirdPartySerializers.java",
        "src/main/java/org/batfish/common/util/serialization/guava/RangeSetDeserializer.java",
        "src/main/java/org/batfish/common/util/serialization/guava/RangeSetDeserializerConverter.java",
        "src/main/java/org/batfish/common/util/serialization/guava/RangeSetSerializer.java",
        "src/main/java/org/batfish/common/util/serialization/guava/RangeSetSerializerConverter.java",
        "src/main/java/org/batfish/datamodel/answers/AnswerElement.java",
        "src/main/java/org/batfish/datamodel/answers/AnswerSummary.java",
        "src/main/java/org/batfish/grammar/BatfishANTLRErrorStrategy.java",
        "src/main/java/org/batfish/grammar/BatfishCombinedParser.java",
        "src/main/java/org/batfish/grammar/BatfishGrammarErrorListener.java",
        "src/main/java/org/batfish/grammar/BatfishLexer.java",
        "src/main/java/org/batfish/grammar/BatfishLexerErrorListener.java",
        "src/main/java/org/batfish/grammar/BatfishLexerRecoveryStrategy.java",
        "src/main/java/org/batfish/grammar/BatfishParseException.java",
        "src/main/java/org/batfish/grammar/BatfishParseTreeWalker.java",
        "src/main/java/org/batfish/grammar/BatfishParser.java",
        "src/main/java/org/batfish/grammar/BatfishParserATNSimulator.java",
        "src/main/java/org/batfish/grammar/BatfishParserErrorListener.java",
        "src/main/java/org/batfish/grammar/GrammarSettings.java",
        "src/main/java/org/batfish/grammar/ParseTreePrettyPrinter.java",
        "src/main/java/org/batfish/grammar/UnrecognizedLineToken.java",
        "src/main/java/org/batfish/grammar/flattener/FlattenerLineMap.java",
    ],
)

java_library(
    name = "parser_common",
    srcs = [":parser_files"],
    deps = [
        "@maven//:com_fasterxml_jackson_core_jackson_annotations",
        "@maven//:com_fasterxml_jackson_core_jackson_core",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_fasterxml_jackson_datatype_jackson_datatype_guava",
        "@maven//:com_fasterxml_jackson_datatype_jackson_datatype_jdk8",
        "@maven//:com_fasterxml_jackson_datatype_jackson_datatype_jsr310",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
        "@maven//:org_antlr_antlr4_runtime",
    ],
)

java_library(
    name = "common",
    srcs = glob([
        "src/main/**/*.java",
    ]),
    plugins = [
        "//:auto_service_plugin",
    ],
    resources = [
        "src/generated/resources/org/batfish/common/common.properties",
        "src/main/resources/org/batfish/common/util/salt",
    ],
    runtime_deps = [
        "@maven//:commons_beanutils_commons_beanutils",
        "@maven//:org_apache_logging_log4j_log4j_core",
    ],
    deps = [
        "//projects/bdd",
        "@maven//:com_fasterxml_jackson_core_jackson_annotations",
        "@maven//:com_fasterxml_jackson_core_jackson_core",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_fasterxml_jackson_datatype_jackson_datatype_guava",
        "@maven//:com_fasterxml_jackson_datatype_jackson_datatype_jdk8",
        "@maven//:com_fasterxml_jackson_datatype_jackson_datatype_jsr310",
        "@maven//:com_google_auto_service_auto_service_annotations",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_errorprone_error_prone_annotations",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_re2j_re2j",
        "@maven//:com_ibm_icu_icu4j",
        "@maven//:commons_cli_commons_cli",
        "@maven//:commons_io_commons_io",
        "@maven//:io_opentracing_contrib_opentracing_jaxrs2",
        "@maven//:io_opentracing_opentracing_api",
        "@maven//:io_opentracing_opentracing_util",
        "@maven//:javax_ws_rs_javax_ws_rs_api",
        "@maven//:org_antlr_antlr4_runtime",
        "@maven//:org_apache_commons_commons_configuration2",
        "@maven//:org_apache_commons_commons_lang3",
        "@maven//:org_apache_commons_commons_text",
        "@maven//:org_apache_logging_log4j_log4j_api",
        "@maven//:org_codehaus_jettison_jettison",
        "@maven//:org_glassfish_grizzly_grizzly_framework",
        "@maven//:org_glassfish_grizzly_grizzly_http_server",
        "@maven//:org_glassfish_jersey_containers_jersey_container_grizzly2_http",
        "@maven//:org_glassfish_jersey_core_jersey_server",
        "@maven//:org_jgrapht_jgrapht_core",
        "@maven//:org_lz4_lz4_java",
        "@maven//:org_parboiled_parboiled_core",
        "@maven//:org_parboiled_parboiled_java",
        "@maven//:org_skyscreamer_jsonassert",
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
        ":recovery_inline_alts_grammar",
        ":recovery_rule_alts_grammar",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
        "@maven//:org_antlr_antlr4_runtime",
        "@maven//:org_apache_commons_commons_configuration2",
        "@maven//:org_hamcrest_hamcrest",
        "@maven//:org_parboiled_parboiled_core",
        "@maven//:org_parboiled_parboiled_java",
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
        "//projects/batfish-common-protocol/src/test/java/org/batfish/common/bdd:matchers",
        "//projects/batfish-common-protocol/src/test/java/org/batfish/common/matchers",
        "//projects/batfish-common-protocol/src/test/java/org/batfish/datamodel/matchers",
        "//projects/bdd",
        "@maven//:com_fasterxml_jackson_core_jackson_annotations",
        "@maven//:com_fasterxml_jackson_core_jackson_core",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_guava_guava_testlib",
        "@maven//:commons_io_commons_io",
        "@maven//:io_opentracing_opentracing_api",
        "@maven//:io_opentracing_opentracing_mock",
        "@maven//:io_opentracing_opentracing_noop",
        "@maven//:javax_ws_rs_javax_ws_rs_api",
        "@maven//:junit_junit",
        "@maven//:org_antlr_antlr4_runtime",
        "@maven//:org_apache_commons_commons_lang3",
        "@maven//:org_hamcrest_hamcrest",
        "@maven//:org_parboiled_parboiled_core",
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
        ":parser_common",
        "@maven//:org_antlr_antlr4_runtime",
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
java -cp $(location @antlr4_tool//jar) \
    org.antlr.v4.Tool \
    -Xexact-output-dir \
       $(location src/test/antlr4/org/batfish/grammar/recovery/RecoveryLexer.g4) \
       $(location src/test/antlr4/org/batfish/grammar/recovery/RecoveryParser.g4) \
    -package org.batfish.grammar.recovery \
    -encoding UTF-8 \
    -Werror \
    -o $(@D)""",
    tools = ["@antlr4_tool//jar"],
)

java_library(
    name = "recovery_inline_alts_grammar",
    srcs = [
        ":RecoveryInlineAltsLexer.java",
        ":RecoveryInlineAltsParser.java",
        ":RecoveryInlineAltsParserBaseListener.java",
        ":RecoveryInlineAltsParserListener.java",
    ],
    deps = [
        ":parser_common",
        "@maven//:org_antlr_antlr4_runtime",
    ],
)

genrule(
    name = "recovery_inline_alts_grammar_src",
    srcs = glob([
        "src/test/antlr4/org/batfish/grammar/recovery_inline_alts/*.g4",
    ]),
    outs = [
        "RecoveryInlineAltsLexer.java",
        "RecoveryInlineAltsParser.java",
        "RecoveryInlineAltsParserBaseListener.java",
        "RecoveryInlineAltsParserListener.java",
    ],
    cmd = """
java -cp $(location @antlr4_tool//jar) \
    org.antlr.v4.Tool \
    -Xexact-output-dir \
       $(location src/test/antlr4/org/batfish/grammar/recovery_inline_alts/RecoveryInlineAltsLexer.g4) \
       $(location src/test/antlr4/org/batfish/grammar/recovery_inline_alts/RecoveryInlineAltsParser.g4) \
    -package org.batfish.grammar.recovery_inline_alts \
    -encoding UTF-8 \
    -Werror \
    -o $(@D)""",
    tools = ["@antlr4_tool//jar"],
)

java_library(
    name = "recovery_rule_alts_grammar",
    srcs = [
        ":RecoveryRuleAltsLexer.java",
        ":RecoveryRuleAltsParser.java",
        ":RecoveryRuleAltsParserBaseListener.java",
        ":RecoveryRuleAltsParserListener.java",
    ],
    deps = [
        ":parser_common",
        "@maven//:org_antlr_antlr4_runtime",
    ],
)

genrule(
    name = "recovery_rule_alts_grammar_src",
    srcs = glob([
        "src/test/antlr4/org/batfish/grammar/recovery_rule_alts/*.g4",
    ]),
    outs = [
        "RecoveryRuleAltsLexer.java",
        "RecoveryRuleAltsParser.java",
        "RecoveryRuleAltsParserBaseListener.java",
        "RecoveryRuleAltsParserListener.java",
    ],
    cmd = """
java -cp $(location @antlr4_tool//jar) \
    org.antlr.v4.Tool \
    -Xexact-output-dir \
       $(location src/test/antlr4/org/batfish/grammar/recovery_rule_alts/RecoveryRuleAltsLexer.g4) \
       $(location src/test/antlr4/org/batfish/grammar/recovery_rule_alts/RecoveryRuleAltsParser.g4) \
    -package org.batfish.grammar.recovery_rule_alts \
    -encoding UTF-8 \
    -Werror \
    -o $(@D)""",
    tools = ["@antlr4_tool//jar"],
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
BATFISH_VERSION="$$(grep -1 batfish-parent $(location //projects:pom.xml) | grep version | sed 's/[<>]/|/g' | cut -f3 -d\\|)"
sed s/\\$${project.version}/$${BATFISH_VERSION}/ $(location src/main/resources/org/batfish/common/common.properties) > "$@"
""",
)

pmd_test(
    name = "pmd",
    lib = ":common",
)
