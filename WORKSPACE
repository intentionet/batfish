workspace(name = "batfish")

load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_jar")

##########################################################
# The Maven artifacts depended-upon by our Java projects #
##########################################################

##########################################################
## First section: enable pubref_rules_maven at all
git_repository(
    name = "org_pubref_rules_maven",
    commit = "fce4f35b5a36109ed4faa820606c518870805bde",
    remote = "https://github.com/dhalperi/rules_maven",
)

load("@org_pubref_rules_maven//maven:rules.bzl", "maven_repositories", "maven_repository")

maven_repositories()

##########################################################
## Second section: per-library, what we depend on

# ANTLR 4 Runtime
maven_repository(
    name = "antlr4_runtime",
    transitive_deps = [
        "e27d8ab4f984f9d186f54da984a6ab1cccac755e:org.antlr:antlr4-runtime:4.7.2",
    ],
    deps = [
        "org.antlr:antlr4-runtime:4.7.2",
    ],
)

load("@antlr4_runtime//:rules.bzl", "antlr4_runtime_compile")

antlr4_runtime_compile()

# AutoService
maven_repository(
    name = "auto_service",
    force = [
        "com.google.errorprone:error_prone_annotations:2.3.1",
        "com.google.guava:guava:26.0-jre",
    ],
    transitive_deps = [
        "c6f7af0e57b9d69d81b05434ef9f3c5610d498c4:com.google.auto:auto-common:0.8",
        "44954d465f3b9065388bbd2fc08a3eb8fd07917c:com.google.auto.service:auto-service:1.0-rc4",
        "25ea2e8b0c338a877313bd4672d3fe056ea78f0d:com.google.code.findbugs:jsr305:3.0.2",
        "a6a2b2df72fd13ec466216049b303f206bd66c5d:com.google.errorprone:error_prone_annotations:2.3.1",
        "6a806eff209f36f635f943e16d97491f00f6bfab:com.google.guava:guava:26.0-jre",
        "ed28ded51a8b1c6b112568def5f4b455e6809019:com.google.j2objc:j2objc-annotations:1.1",
        "cea74543d5904a30861a61b4643a5f2bb372efc4:org.checkerframework:checker-qual:2.5.2",
        "775b7e22fb10026eed3f86e8dc556dfafe35f2d5:org.codehaus.mojo:animal-sniffer-annotations:1.14",
    ],
    deps = [
        "com.google.auto.service:auto-service:1.0-rc4",
    ],
)

load("@auto_service//:rules.bzl", "auto_service_compile")

auto_service_compile()

# Commons
maven_repository(
    name = "commons_beanutils",
    transitive_deps = [
        "c845703de334ddc6b4b3cd26835458cb1cba1f3d:commons-beanutils:commons-beanutils:1.9.3",
        "8ad72fe39fa8c91eaaf12aadb21e0c3661fe26d5:commons-collections:commons-collections:3.2.2",
        "4bfc12adfe4842bf07b657f0369c4cb522955686:commons-logging:commons-logging:1.2",
    ],
    deps = [
        "commons-beanutils:commons-beanutils:1.9.3",
    ],
)

load("@commons_beanutils//:rules.bzl", "commons_beanutils_compile")

commons_beanutils_compile()

#
maven_repository(
    name = "commons_cli",
    transitive_deps = [
        "c51c00206bb913cd8612b24abd9fa98ae89719b1:commons-cli:commons-cli:1.4",
    ],
    deps = [
        "commons-cli:commons-cli:1.4",
    ],
)

load("@commons_cli//:rules.bzl", "commons_cli_compile")

commons_cli_compile()

#
maven_repository(
    name = "commons_collections4",
    transitive_deps = [
        "54ebea0a5b653d3c680131e73fe807bb8f78c4ed:org.apache.commons:commons-collections4:4.2",
    ],
    deps = [
        "org.apache.commons:commons-collections4:4.2",
    ],
)

load("@commons_collections4//:rules.bzl", "commons_collections4_compile")

commons_collections4_compile()

#
maven_repository(
    name = "commons_configuration2",
    force = [
        "org.apache.commons:commons-lang3:3.8.1",
    ],
    transitive_deps = [
        "4bfc12adfe4842bf07b657f0369c4cb522955686:commons-logging:commons-logging:1.2",
        "97dcb24a1624e93d7ef2a434d3c1707ceba9d01b:org.apache.commons:commons-configuration2:2.3",
        "6505a72a097d9270f7a9e7bf42c4238283247755:org.apache.commons:commons-lang3:3.8.1",
    ],
    deps = [
        "org.apache.commons:commons-configuration2:2.3",
    ],
)

load("@commons_configuration2//:rules.bzl", "commons_configuration2_compile")

commons_configuration2_compile()

#
maven_repository(
    name = "commons_lang3",
    transitive_deps = [
        "6505a72a097d9270f7a9e7bf42c4238283247755:org.apache.commons:commons-lang3:3.8.1",
    ],
    deps = [
        "org.apache.commons:commons-lang3:3.8.1",
    ],
)

load("@commons_lang3//:rules.bzl", "commons_lang3_compile")

commons_lang3_compile()

#
maven_repository(
    name = "commons_io",
    transitive_deps = [
        "815893df5f31da2ece4040fe0a12fd44b577afaf:commons-io:commons-io:2.6",
    ],
    deps = [
        "commons-io:commons-io:2.6",
    ],
)

load("@commons_io//:rules.bzl", "commons_io_compile")

commons_io_compile()

# diffutils
maven_repository(
    name = "diffutils",
    transitive_deps = [
        "0647f913ef6c350d3b1bc1ffa48a74747a13e89f:com.github.wumpz:diffutils:2.2",
        "63998ced66e425d9e8bcd0c59f710c98f0c021ff:org.eclipse.jgit:org.eclipse.jgit:4.4.1.201607150455-r",
    ],
    deps = [
        "com.github.wumpz:diffutils:2.2",
    ],
)

load("@diffutils//:rules.bzl", "diffutils_compile")

diffutils_compile()

# errorprone-annotations
maven_repository(
    name = "errorprone_annotations",
    transitive_deps = [
        "a6a2b2df72fd13ec466216049b303f206bd66c5d:com.google.errorprone:error_prone_annotations:2.3.1",
    ],
    deps = [
        "com.google.errorprone:error_prone_annotations:2.3.1",
    ],
)

load("@errorprone_annotations//:rules.bzl", "errorprone_annotations_compile")

errorprone_annotations_compile()

# Grizzly
maven_repository(
    name = "grizzly_framework",
    transitive_deps = [
        "3dc308a8ee5f5cf7349148237a1e2c1c6c459ba1:org.glassfish.grizzly:grizzly-framework:2.4.3",
    ],
    deps = [
        "org.glassfish.grizzly:grizzly-framework:2.4.3",
    ],
)

load("@grizzly_framework//:rules.bzl", "grizzly_framework_compile")

grizzly_framework_compile()

#
maven_repository(
    name = "grizzly_server",
    transitive_deps = [
        "3dc308a8ee5f5cf7349148237a1e2c1c6c459ba1:org.glassfish.grizzly:grizzly-framework:2.4.3",
        "abe2f327ace7f46625f49ce2e20d033c28baddfa:org.glassfish.grizzly:grizzly-http:2.4.3",
        "5bb7c86c60f7642c2aed359823e4cca629014e9c:org.glassfish.grizzly:grizzly-http-server:2.4.3",
    ],
    deps = [
        "org.glassfish.grizzly:grizzly-http-server:2.4.3",
    ],
)

load("@grizzly_server//:rules.bzl", "grizzly_server_compile")

grizzly_server_compile()

# Guava
maven_repository(
    name = "guava",
    force = [
        "com.google.errorprone:error_prone_annotations:2.3.1",
    ],
    transitive_deps = [
        "25ea2e8b0c338a877313bd4672d3fe056ea78f0d:com.google.code.findbugs:jsr305:3.0.2",
        "a6a2b2df72fd13ec466216049b303f206bd66c5d:com.google.errorprone:error_prone_annotations:2.3.1",
        "6a806eff209f36f635f943e16d97491f00f6bfab:com.google.guava:guava:26.0-jre",
        "ed28ded51a8b1c6b112568def5f4b455e6809019:com.google.j2objc:j2objc-annotations:1.1",
        "cea74543d5904a30861a61b4643a5f2bb372efc4:org.checkerframework:checker-qual:2.5.2",
        "775b7e22fb10026eed3f86e8dc556dfafe35f2d5:org.codehaus.mojo:animal-sniffer-annotations:1.14",
    ],
    deps = [
        "com.google.guava:guava:26.0-jre",
    ],
)

load("@guava//:rules.bzl", "guava_compile")

guava_compile()

# Guava-testlib
maven_repository(
    name = "guava_testlib",
    exclude = {
        "com.google.guava:guava-testlib": [
            "org.hamcrest:hamcrest-core",
        ],
    },
    force = [
        "junit:junit:4.12",
        "com.google.errorprone:error_prone_annotations:2.3.1",
    ],
    transitive_deps = [
        "25ea2e8b0c338a877313bd4672d3fe056ea78f0d:com.google.code.findbugs:jsr305:3.0.2",
        "a6a2b2df72fd13ec466216049b303f206bd66c5d:com.google.errorprone:error_prone_annotations:2.3.1",
        "6a806eff209f36f635f943e16d97491f00f6bfab:com.google.guava:guava:26.0-jre",
        "3d6b3f4877cf8b203860333811af4764de93c8c1:com.google.guava:guava-testlib:26.0-jre",
        "ed28ded51a8b1c6b112568def5f4b455e6809019:com.google.j2objc:j2objc-annotations:1.1",
        "2973d150c0dc1fefe998f834810d68f278ea58ec:junit:junit:4.12",
        "cea74543d5904a30861a61b4643a5f2bb372efc4:org.checkerframework:checker-qual:2.5.2",
        "775b7e22fb10026eed3f86e8dc556dfafe35f2d5:org.codehaus.mojo:animal-sniffer-annotations:1.14",
        "0f1c8853ade0ecf707f5a261c830e98893983813:org.hamcrest:java-hamcrest:2.0.0.0",
    ],
    deps = [
        "com.google.guava:guava-testlib:26.0-jre",
        "org.hamcrest:java-hamcrest:2.0.0.0",
    ],
)

load("@guava_testlib//:rules.bzl", "guava_testlib_compile")

guava_testlib_compile()

# Hamcrest
maven_repository(
    name = "hamcrest",
    transitive_deps = [
        "0f1c8853ade0ecf707f5a261c830e98893983813:org.hamcrest:java-hamcrest:2.0.0.0",
    ],
    deps = [
        "org.hamcrest:java-hamcrest:2.0.0.0",
    ],
)

load("@hamcrest//:rules.bzl", "hamcrest_compile")

hamcrest_compile()

# icu4j
maven_repository(
    name = "icu4j",
    transitive_deps = [
        "385682b7fff53cd5ac2cad0fdb4658a7b97e9475:com.ibm.icu:icu4j:63.1",
    ],
    deps = [
        "com.ibm.icu:icu4j:63.1",
    ],
)

load("@icu4j//:rules.bzl", "icu4j_compile")

icu4j_compile()

# jackson
maven_repository(
    name = "jackson_annotations",
    transitive_deps = [
        "ba7f0e6f8f1b28d251eeff2a5604bed34c53ff35:com.fasterxml.jackson.core:jackson-annotations:2.9.8",
    ],
    deps = [
        "com.fasterxml.jackson.core:jackson-annotations:2.9.8",
    ],
)

load("@jackson_annotations//:rules.bzl", "jackson_annotations_compile")

jackson_annotations_compile()

#
maven_repository(
    name = "jackson_core",
    transitive_deps = [
        "0f5a654e4675769c716e5b387830d19b501ca191:com.fasterxml.jackson.core:jackson-core:2.9.8",
    ],
    deps = [
        "com.fasterxml.jackson.core:jackson-core:2.9.8",
    ],
)

load("@jackson_core//:rules.bzl", "jackson_core_compile")

jackson_core_compile()

#
maven_repository(
    name = "jackson_databind",
    force = [
        "com.fasterxml.jackson.core:jackson-annotations:2.9.8",
    ],
    transitive_deps = [
        "ba7f0e6f8f1b28d251eeff2a5604bed34c53ff35:com.fasterxml.jackson.core:jackson-annotations:2.9.8",
        "0f5a654e4675769c716e5b387830d19b501ca191:com.fasterxml.jackson.core:jackson-core:2.9.8",
        "11283f21cc480aa86c4df7a0a3243ec508372ed2:com.fasterxml.jackson.core:jackson-databind:2.9.8",
    ],
    deps = [
        "com.fasterxml.jackson.core:jackson-databind:2.9.8",
    ],
)

load("@jackson_databind//:rules.bzl", "jackson_databind_compile")

jackson_databind_compile()

#
maven_repository(
    name = "jackson_guava",
    force = [
        "com.fasterxml.jackson.core:jackson-annotations:2.9.8",
        "com.google.guava:guava:26.0-jre",
        "com.google.errorprone:error_prone_annotations:2.3.1",
    ],
    transitive_deps = [
        "ba7f0e6f8f1b28d251eeff2a5604bed34c53ff35:com.fasterxml.jackson.core:jackson-annotations:2.9.8",
        "0f5a654e4675769c716e5b387830d19b501ca191:com.fasterxml.jackson.core:jackson-core:2.9.8",
        "11283f21cc480aa86c4df7a0a3243ec508372ed2:com.fasterxml.jackson.core:jackson-databind:2.9.8",
        "b7f7819800f8ebe51a03dd951636f6dbc6245ce6:com.fasterxml.jackson.datatype:jackson-datatype-guava:2.9.8",
        "25ea2e8b0c338a877313bd4672d3fe056ea78f0d:com.google.code.findbugs:jsr305:3.0.2",
        "a6a2b2df72fd13ec466216049b303f206bd66c5d:com.google.errorprone:error_prone_annotations:2.3.1",
        "6a806eff209f36f635f943e16d97491f00f6bfab:com.google.guava:guava:26.0-jre",
        "ed28ded51a8b1c6b112568def5f4b455e6809019:com.google.j2objc:j2objc-annotations:1.1",
        "cea74543d5904a30861a61b4643a5f2bb372efc4:org.checkerframework:checker-qual:2.5.2",
        "775b7e22fb10026eed3f86e8dc556dfafe35f2d5:org.codehaus.mojo:animal-sniffer-annotations:1.14",
    ],
    deps = [
        "com.fasterxml.jackson.datatype:jackson-datatype-guava:2.9.8",
    ],
)

load("@jackson_guava//:rules.bzl", "jackson_guava_compile")

jackson_guava_compile()

#
maven_repository(
    name = "jackson_jdk8",
    force = [
        "com.fasterxml.jackson.core:jackson-annotations:2.9.8",
    ],
    transitive_deps = [
        "ba7f0e6f8f1b28d251eeff2a5604bed34c53ff35:com.fasterxml.jackson.core:jackson-annotations:2.9.8",
        "0f5a654e4675769c716e5b387830d19b501ca191:com.fasterxml.jackson.core:jackson-core:2.9.8",
        "11283f21cc480aa86c4df7a0a3243ec508372ed2:com.fasterxml.jackson.core:jackson-databind:2.9.8",
        "bcd02aa9195390e23747ed40bf76be869ad3a2fb:com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.9.8",
    ],
    deps = [
        "com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.9.8",
    ],
)

load("@jackson_jdk8//:rules.bzl", "jackson_jdk8_compile")

jackson_jdk8_compile()

#
maven_repository(
    name = "jackson_jsr310",
    force = [
        "com.fasterxml.jackson.core:jackson-annotations:2.9.8",
    ],
    transitive_deps = [
        "ba7f0e6f8f1b28d251eeff2a5604bed34c53ff35:com.fasterxml.jackson.core:jackson-annotations:2.9.8",
        "0f5a654e4675769c716e5b387830d19b501ca191:com.fasterxml.jackson.core:jackson-core:2.9.8",
        "11283f21cc480aa86c4df7a0a3243ec508372ed2:com.fasterxml.jackson.core:jackson-databind:2.9.8",
        "28ad1bced632ba338e51c825a652f6e11a8e6eac:com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.9.8",
    ],
    deps = [
        "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.9.8",
    ],
)

load("@jackson_jsr310//:rules.bzl", "jackson_jsr310_compile")

jackson_jsr310_compile()

#
maven_repository(
    name = "jackson_jaxrs_base",
    force = [
        "com.fasterxml.jackson.core:jackson-annotations:2.9.8",
    ],
    transitive_deps = [
        "ba7f0e6f8f1b28d251eeff2a5604bed34c53ff35:com.fasterxml.jackson.core:jackson-annotations:2.9.8",
        "0f5a654e4675769c716e5b387830d19b501ca191:com.fasterxml.jackson.core:jackson-core:2.9.8",
        "11283f21cc480aa86c4df7a0a3243ec508372ed2:com.fasterxml.jackson.core:jackson-databind:2.9.8",
        "0113dc616852ea990255d3a820d1006ac2531ae7:com.fasterxml.jackson.jaxrs:jackson-jaxrs-base:2.9.8",
    ],
    deps = [
        "com.fasterxml.jackson.jaxrs:jackson-jaxrs-base:2.9.8",
    ],
)

load("@jackson_jaxrs_base//:rules.bzl", "jackson_jaxrs_base_compile")

jackson_jaxrs_base_compile()

# Jaeger
maven_repository(
    name = "jaeger_core",
    force = [
        "commons-logging:commons-logging:1.2",
        "org.slf4j:slf4j-api:1.7.25",
    ],
    transitive_deps = [
        "c4ba5371a29ac9b2ad6129b1d39ea38750043eff:com.google.code.gson:gson:2.8.0",
        "4d060ca3190df0eda4dc13415532a12e15ca5f11:com.squareup.okhttp3:okhttp:3.8.1",
        "a9283170b7305c8d92d25aff02a6ab7e45d06cbe:com.squareup.okio:okio:1.13.0",
        "3afc7a32a93b3ad1587d9ae3308ff9a93dcc0573:com.uber.jaeger:jaeger-core:0.21.0",
        "d4211908c29a740a08bf5d4c71e22024c7ccda13:com.uber.jaeger:jaeger-thrift:0.21.0",
        "b7f0fc8f61ecadeb3695f0b9464755eee44374d4:commons-codec:commons-codec:1.6",
        "4bfc12adfe4842bf07b657f0369c4cb522955686:commons-logging:commons-logging:1.2",
        "7e54c7a39d508a3268509d9c69dfeea665fc0595:io.opentracing:opentracing-api:0.30.0",
        "0a8e47f081e14acda02dd7446b43a990d5e48a4d:io.opentracing:opentracing-noop:0.30.0",
        "b5b39128cee4ff50b6aee28f9248875fdccd22c6:io.opentracing:opentracing-util:0.30.0",
        "666e26e76f2e87d84e4f16acb546481ae1b8e9a6:org.apache.httpcomponents:httpclient:4.2.5",
        "3b7f38df6de5dd8b500e602ae8c2dd5ee446f883:org.apache.httpcomponents:httpcore:4.2.4",
        "9b067e2e2c5291e9f0d8b3561b1654286e6d81ee:org.apache.thrift:libthrift:0.9.2",
        "da76ca59f6a57ee3102f8f9bd9cee742973efa8a:org.slf4j:slf4j-api:1.7.25",
    ],
    deps = [
        "com.uber.jaeger:jaeger-core:0.21.0",
    ],
)

load("@jaeger_core//:rules.bzl", "jaeger_core_compile")

jaeger_core_compile()

# JAX-RS
maven_repository(
    name = "jaxrs_api",
    transitive_deps = [
        "d3466bc9321fe84f268a1adb3b90373fc14b0eb5:javax.ws.rs:javax.ws.rs-api:2.1.1",
    ],
    deps = [
        "javax.ws.rs:javax.ws.rs-api:2.1.1",
    ],
)

load("@jaxrs_api//:rules.bzl", "jaxrs_api_compile")

jaxrs_api_compile()

# javax-annotation
maven_repository(
    name = "javax_annotation",
    transitive_deps = [
        "934c04d3cfef185a8008e7bf34331b79730a9d43:javax.annotation:javax.annotation-api:1.3.2",
    ],
    deps = [
        "javax.annotation:javax.annotation-api:1.3.2",
    ],
)

load("@javax_annotation//:rules.bzl", "javax_annotation_compile")

javax_annotation_compile()

# Jersey
maven_repository(
    name = "jersey_client",
    force = [
        "javax.annotation:javax.annotation-api:1.3.2",
        "javax.ws.rs:javax.ws.rs-api:2.1.1",
    ],
    transitive_deps = [
        "934c04d3cfef185a8008e7bf34331b79730a9d43:javax.annotation:javax.annotation-api:1.3.2",
        "d3466bc9321fe84f268a1adb3b90373fc14b0eb5:javax.ws.rs:javax.ws.rs-api:2.1.1",
        "98e0b7dcef77dc04809f0603868140a1c60bea71:org.glassfish.hk2.external:javax.inject:2.5.0-b42",
        "4ed2b2d4738aed5786cfa64cba5a332779c4c708:org.glassfish.hk2:osgi-resource-locator:1.0.1",
        "66dd6a62a981c0f186d96048fe11e9ddeca3ae28:org.glassfish.jersey.core:jersey-client:2.27",
        "f20d4304a476a92001930d75747adcc232dbe779:org.glassfish.jersey.core:jersey-common:2.27",
    ],
    deps = [
        "org.glassfish.jersey.core:jersey-client:2.27",
    ],
)

load("@jersey_client//:rules.bzl", "jersey_client_compile")

jersey_client_compile()

#
maven_repository(
    name = "jersey_common",
    force = [
        "javax.annotation:javax.annotation-api:1.3.2",
        "javax.ws.rs:javax.ws.rs-api:2.1.1",
    ],
    transitive_deps = [
        "934c04d3cfef185a8008e7bf34331b79730a9d43:javax.annotation:javax.annotation-api:1.3.2",
        "d3466bc9321fe84f268a1adb3b90373fc14b0eb5:javax.ws.rs:javax.ws.rs-api:2.1.1",
        "98e0b7dcef77dc04809f0603868140a1c60bea71:org.glassfish.hk2.external:javax.inject:2.5.0-b42",
        "4ed2b2d4738aed5786cfa64cba5a332779c4c708:org.glassfish.hk2:osgi-resource-locator:1.0.1",
        "f20d4304a476a92001930d75747adcc232dbe779:org.glassfish.jersey.core:jersey-common:2.27",
    ],
    deps = [
        "org.glassfish.jersey.core:jersey-common:2.27",
    ],
)

load("@jersey_common//:rules.bzl", "jersey_common_compile")

jersey_common_compile()

#
maven_repository(
    name = "jersey_container_grizzly2",
    force = [
        "javax.annotation:javax.annotation-api:1.3.2",
        "javax.ws.rs:javax.ws.rs-api:2.1.1",
        "org.glassfish.grizzly:grizzly-http-server:2.4.3",
    ],
    transitive_deps = [
        "934c04d3cfef185a8008e7bf34331b79730a9d43:javax.annotation:javax.annotation-api:1.3.2",
        "8613ae82954779d518631e05daa73a6a954817d5:javax.validation:validation-api:1.1.0.Final",
        "d3466bc9321fe84f268a1adb3b90373fc14b0eb5:javax.ws.rs:javax.ws.rs-api:2.1.1",
        "99f802e0cb3e953ba3d6e698795c4aeb98d37c48:javax.xml.bind:jaxb-api:2.3.0",
        "3dc308a8ee5f5cf7349148237a1e2c1c6c459ba1:org.glassfish.grizzly:grizzly-framework:2.4.3",
        "abe2f327ace7f46625f49ce2e20d033c28baddfa:org.glassfish.grizzly:grizzly-http:2.4.3",
        "5bb7c86c60f7642c2aed359823e4cca629014e9c:org.glassfish.grizzly:grizzly-http-server:2.4.3",
        "98e0b7dcef77dc04809f0603868140a1c60bea71:org.glassfish.hk2.external:javax.inject:2.5.0-b42",
        "4ed2b2d4738aed5786cfa64cba5a332779c4c708:org.glassfish.hk2:osgi-resource-locator:1.0.1",
        "57ac01a84d7084802398ff7caae38d8e68bb3c50:org.glassfish.jersey.containers:jersey-container-grizzly2-http:2.27",
        "66dd6a62a981c0f186d96048fe11e9ddeca3ae28:org.glassfish.jersey.core:jersey-client:2.27",
        "f20d4304a476a92001930d75747adcc232dbe779:org.glassfish.jersey.core:jersey-common:2.27",
        "92c158af89e6749b20134c3182935046a49a1662:org.glassfish.jersey.core:jersey-server:2.27",
        "106f2237ee03493185d9d915c9a1c011df716a2a:org.glassfish.jersey.media:jersey-media-jaxb:2.27",
    ],
    deps = [
        "javax.xml.bind:jaxb-api:2.3.0",
        "org.glassfish.jersey.containers:jersey-container-grizzly2-http:2.27",
    ],
)

load("@jersey_container_grizzly2//:rules.bzl", "jersey_container_grizzly2_compile")

jersey_container_grizzly2_compile()

#
maven_repository(
    name = "jersey_media_jackson",
    force = [
        "com.fasterxml.jackson.core:jackson-annotations:2.9.8",
        "com.fasterxml.jackson.core:jackson-core:2.9.8",
        "com.fasterxml.jackson.core:jackson-databind:2.9.8",
        "com.fasterxml.jackson.jaxrs:jackson-jaxrs-base:2.9.8",
        "com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider:2.9.8",
        "com.fasterxml.jackson.module:jackson-module-jaxb-annotations:2.9.8",
        "javax.annotation:javax.annotation-api:1.3.2",
        "javax.ws.rs:javax.ws.rs-api:2.1.1",
    ],
    transitive_deps = [
        "ba7f0e6f8f1b28d251eeff2a5604bed34c53ff35:com.fasterxml.jackson.core:jackson-annotations:2.9.8",
        "0f5a654e4675769c716e5b387830d19b501ca191:com.fasterxml.jackson.core:jackson-core:2.9.8",
        "11283f21cc480aa86c4df7a0a3243ec508372ed2:com.fasterxml.jackson.core:jackson-databind:2.9.8",
        "da08815ba1c7f7b435e7df02b7bf98327bad2fd4:com.fasterxml.jackson.module:jackson-module-jaxb-annotations:2.9.8",
        "934c04d3cfef185a8008e7bf34331b79730a9d43:javax.annotation:javax.annotation-api:1.3.2",
        "d3466bc9321fe84f268a1adb3b90373fc14b0eb5:javax.ws.rs:javax.ws.rs-api:2.1.1",
        "98e0b7dcef77dc04809f0603868140a1c60bea71:org.glassfish.hk2.external:javax.inject:2.5.0-b42",
        "4ed2b2d4738aed5786cfa64cba5a332779c4c708:org.glassfish.hk2:osgi-resource-locator:1.0.1",
        "f20d4304a476a92001930d75747adcc232dbe779:org.glassfish.jersey.core:jersey-common:2.27",
        "60280972e427915c123db25dbb7adc3fd4fc28a1:org.glassfish.jersey.ext:jersey-entity-filtering:2.27",
        "c426d54b5a7f4dd470286c7fa44d08d8cbc286be:org.glassfish.jersey.media:jersey-media-json-jackson:2.27",
    ],
    deps = [
        "org.glassfish.jersey.media:jersey-media-json-jackson:2.27",
    ],
)

load("@jersey_media_jackson//:rules.bzl", "jersey_media_jackson_compile")

jersey_media_jackson_compile()

#
maven_repository(
    name = "jersey_media_jettison",
    force = [
        "org.codehaus.jettison:jettison:1.4.0",
        "javax.annotation:javax.annotation-api:1.3.2",
        "javax.ws.rs:javax.ws.rs-api:2.1.1",
    ],
    transitive_deps = [
        "934c04d3cfef185a8008e7bf34331b79730a9d43:javax.annotation:javax.annotation-api:1.3.2",
        "d3466bc9321fe84f268a1adb3b90373fc14b0eb5:javax.ws.rs:javax.ws.rs-api:2.1.1",
        "99f802e0cb3e953ba3d6e698795c4aeb98d37c48:javax.xml.bind:jaxb-api:2.3.0",
        "8f0e7bb69242e9ed5bfdf7384b37c1095b0974fc:org.codehaus.jettison:jettison:1.4.0",
        "98e0b7dcef77dc04809f0603868140a1c60bea71:org.glassfish.hk2.external:javax.inject:2.5.0-b42",
        "4ed2b2d4738aed5786cfa64cba5a332779c4c708:org.glassfish.hk2:osgi-resource-locator:1.0.1",
        "f20d4304a476a92001930d75747adcc232dbe779:org.glassfish.jersey.core:jersey-common:2.27",
        "106f2237ee03493185d9d915c9a1c011df716a2a:org.glassfish.jersey.media:jersey-media-jaxb:2.27",
        "5649c52a4e0e54df7612afabd67f2bae24c593f3:org.glassfish.jersey.media:jersey-media-json-jettison:2.27",
    ],
    deps = [
        "javax.xml.bind:jaxb-api:2.3.0",
        "org.glassfish.jersey.media:jersey-media-json-jettison:2.27",
    ],
)

load("@jersey_media_jettison//:rules.bzl", "jersey_media_jettison_compile")

jersey_media_jettison_compile()

#
maven_repository(
    name = "jersey_media_multipart",
    force = [
        "javax.annotation:javax.annotation-api:1.3.2",
        "javax.ws.rs:javax.ws.rs-api:2.1.1",
    ],
    transitive_deps = [
        "934c04d3cfef185a8008e7bf34331b79730a9d43:javax.annotation:javax.annotation-api:1.3.2",
        "d3466bc9321fe84f268a1adb3b90373fc14b0eb5:javax.ws.rs:javax.ws.rs-api:2.1.1",
        "98e0b7dcef77dc04809f0603868140a1c60bea71:org.glassfish.hk2.external:javax.inject:2.5.0-b42",
        "4ed2b2d4738aed5786cfa64cba5a332779c4c708:org.glassfish.hk2:osgi-resource-locator:1.0.1",
        "f20d4304a476a92001930d75747adcc232dbe779:org.glassfish.jersey.core:jersey-common:2.27",
        "ebd5d04fbd308ba7d7cba7ecc93869062e42327b:org.glassfish.jersey.media:jersey-media-multipart:2.27",
        "41c913d791e16f93bc712a8c8a30bb64daa2e9bd:org.jvnet.mimepull:mimepull:1.9.6",
    ],
    deps = [
        "org.glassfish.jersey.media:jersey-media-multipart:2.27",
    ],
)

load("@jersey_media_multipart//:rules.bzl", "jersey_media_multipart_compile")

jersey_media_multipart_compile()

#
maven_repository(
    name = "jersey_server",
    force = [
        "javax.annotation:javax.annotation-api:1.3.2",
        "javax.ws.rs:javax.ws.rs-api:2.1.1",
    ],
    transitive_deps = [
        "e6cb541461c2834bdea3eb920f1884d1eb508b50:javax.activation:activation:1.1",
        "934c04d3cfef185a8008e7bf34331b79730a9d43:javax.annotation:javax.annotation-api:1.3.2",
        "6975da39a7040257bd51d21a231b76c915872d38:javax.inject:javax.inject:1",
        "8613ae82954779d518631e05daa73a6a954817d5:javax.validation:validation-api:1.1.0.Final",
        "d3466bc9321fe84f268a1adb3b90373fc14b0eb5:javax.ws.rs:javax.ws.rs-api:2.1.1",
        "99f802e0cb3e953ba3d6e698795c4aeb98d37c48:javax.xml.bind:jaxb-api:2.3.0",
        "e74beab6ca12e9e745eb47ca61729d9452b96f0c:org.glassfish.hk2.external:aopalliance-repackaged:2.5.0-b42",
        "98e0b7dcef77dc04809f0603868140a1c60bea71:org.glassfish.hk2.external:javax.inject:2.5.0-b42",
        "7fd4e3d0f2cb37c80ad0dedee3f5ee69503eaf52:org.glassfish.hk2:hk2-api:2.5.0-b42",
        "9d6edc0cb226401a8316e67d81bfc37cb626ef91:org.glassfish.hk2:hk2-locator:2.5.0-b42",
        "102a344e1728825e865a8986d7605602aba3c3b6:org.glassfish.hk2:hk2-utils:2.5.0-b42",
        "4ed2b2d4738aed5786cfa64cba5a332779c4c708:org.glassfish.hk2:osgi-resource-locator:1.0.1",
        "66dd6a62a981c0f186d96048fe11e9ddeca3ae28:org.glassfish.jersey.core:jersey-client:2.27",
        "f20d4304a476a92001930d75747adcc232dbe779:org.glassfish.jersey.core:jersey-common:2.27",
        "92c158af89e6749b20134c3182935046a49a1662:org.glassfish.jersey.core:jersey-server:2.27",
        "9ea08f93ee7045a36926954536ad385ac03b753f:org.glassfish.jersey.inject:jersey-hk2:2.27",
        "106f2237ee03493185d9d915c9a1c011df716a2a:org.glassfish.jersey.media:jersey-media-jaxb:2.27",
        "44eaf0990dea92f4bca4b9931b2239c0e8756ee7:org.javassist:javassist:3.22.0-CR2",
    ],
    deps = [
        "javax.activation:activation:1.1",
        "javax.xml.bind:jaxb-api:2.3.0",
        "org.glassfish.jersey.core:jersey-server:2.27",
        "org.glassfish.jersey.inject:jersey-hk2:2.27",
    ],
)

load("@jersey_server//:rules.bzl", "jersey_server_compile")

jersey_server_compile()

#
maven_repository(
    name = "jersey_test_framework",
    exclude = {
        "org.glassfish.jersey.test-framework:jersey-test-framework-core": [
            "org.hamcrest:hamcrest-core",
        ],
    },
    force = [
        "javax.servlet:javax.servlet-api:4.0.0",
        "javax.annotation:javax.annotation-api:1.3.2",
        "javax.ws.rs:javax.ws.rs-api:2.1.1",
    ],
    transitive_deps = [
        "934c04d3cfef185a8008e7bf34331b79730a9d43:javax.annotation:javax.annotation-api:1.3.2",
        "60200affc2fe0165136ed3690faf00b66aed581a:javax.servlet:javax.servlet-api:4.0.0",
        "8613ae82954779d518631e05daa73a6a954817d5:javax.validation:validation-api:1.1.0.Final",
        "d3466bc9321fe84f268a1adb3b90373fc14b0eb5:javax.ws.rs:javax.ws.rs-api:2.1.1",
        "99f802e0cb3e953ba3d6e698795c4aeb98d37c48:javax.xml.bind:jaxb-api:2.3.0",
        "2973d150c0dc1fefe998f834810d68f278ea58ec:junit:junit:4.12",
        "98e0b7dcef77dc04809f0603868140a1c60bea71:org.glassfish.hk2.external:javax.inject:2.5.0-b42",
        "4ed2b2d4738aed5786cfa64cba5a332779c4c708:org.glassfish.hk2:osgi-resource-locator:1.0.1",
        "81e24b17ff7cee96a1c830fe63aa1215176c009c:org.glassfish.jersey.containers:jersey-container-servlet-core:2.27",
        "66dd6a62a981c0f186d96048fe11e9ddeca3ae28:org.glassfish.jersey.core:jersey-client:2.27",
        "f20d4304a476a92001930d75747adcc232dbe779:org.glassfish.jersey.core:jersey-common:2.27",
        "92c158af89e6749b20134c3182935046a49a1662:org.glassfish.jersey.core:jersey-server:2.27",
        "106f2237ee03493185d9d915c9a1c011df716a2a:org.glassfish.jersey.media:jersey-media-jaxb:2.27",
        "5e5cc32c73cda92af420be5302b963c8bd26f26f:org.glassfish.jersey.test-framework:jersey-test-framework-core:2.27",
        "0f1c8853ade0ecf707f5a261c830e98893983813:org.hamcrest:java-hamcrest:2.0.0.0",
    ],
    deps = [
        "javax.xml.bind:jaxb-api:2.3.0",
        "org.glassfish.jersey.test-framework:jersey-test-framework-core:2.27",
        "org.hamcrest:java-hamcrest:2.0.0.0",
    ],
)

load("@jersey_test_framework//:rules.bzl", "jersey_test_framework_compile")

jersey_test_framework_compile()

#
maven_repository(
    name = "jersey_test_framework_grizzly2",
    exclude = {
        "org.glassfish.jersey.test-framework.providers:jersey-test-framework-provider-grizzly2": [
            "org.hamcrest:hamcrest-core",
        ],
    },
    force = [
        "javax.servlet:javax.servlet-api:4.0.0",
        "javax.annotation:javax.annotation-api:1.3.2",
        "javax.ws.rs:javax.ws.rs-api:2.1.1",
        "org.glassfish.grizzly:grizzly-framework:2.4.3",
        "org.glassfish.grizzly:grizzly-http:2.4.3",
        "org.glassfish.grizzly:grizzly-http-server:2.4.3",
        "org.glassfish.grizzly:grizzly-http-servlet:2.4.3",
    ],
    transitive_deps = [
        "934c04d3cfef185a8008e7bf34331b79730a9d43:javax.annotation:javax.annotation-api:1.3.2",
        "60200affc2fe0165136ed3690faf00b66aed581a:javax.servlet:javax.servlet-api:4.0.0",
        "8613ae82954779d518631e05daa73a6a954817d5:javax.validation:validation-api:1.1.0.Final",
        "d3466bc9321fe84f268a1adb3b90373fc14b0eb5:javax.ws.rs:javax.ws.rs-api:2.1.1",
        "99f802e0cb3e953ba3d6e698795c4aeb98d37c48:javax.xml.bind:jaxb-api:2.3.0",
        "2973d150c0dc1fefe998f834810d68f278ea58ec:junit:junit:4.12",
        "3dc308a8ee5f5cf7349148237a1e2c1c6c459ba1:org.glassfish.grizzly:grizzly-framework:2.4.3",
        "abe2f327ace7f46625f49ce2e20d033c28baddfa:org.glassfish.grizzly:grizzly-http:2.4.3",
        "5bb7c86c60f7642c2aed359823e4cca629014e9c:org.glassfish.grizzly:grizzly-http-server:2.4.3",
        "00a067d0e6bc3220997afe6fda1dd015482044fd:org.glassfish.grizzly:grizzly-http-servlet:2.4.3",
        "98e0b7dcef77dc04809f0603868140a1c60bea71:org.glassfish.hk2.external:javax.inject:2.5.0-b42",
        "4ed2b2d4738aed5786cfa64cba5a332779c4c708:org.glassfish.hk2:osgi-resource-locator:1.0.1",
        "57ac01a84d7084802398ff7caae38d8e68bb3c50:org.glassfish.jersey.containers:jersey-container-grizzly2-http:2.27",
        "41f451e3d73fe701f836f15a1fdf1bd27243407e:org.glassfish.jersey.containers:jersey-container-grizzly2-servlet:2.27",
        "aa97ee9f6528706d1d7214f39a404297903a7598:org.glassfish.jersey.containers:jersey-container-servlet:2.27",
        "81e24b17ff7cee96a1c830fe63aa1215176c009c:org.glassfish.jersey.containers:jersey-container-servlet-core:2.27",
        "66dd6a62a981c0f186d96048fe11e9ddeca3ae28:org.glassfish.jersey.core:jersey-client:2.27",
        "f20d4304a476a92001930d75747adcc232dbe779:org.glassfish.jersey.core:jersey-common:2.27",
        "92c158af89e6749b20134c3182935046a49a1662:org.glassfish.jersey.core:jersey-server:2.27",
        "106f2237ee03493185d9d915c9a1c011df716a2a:org.glassfish.jersey.media:jersey-media-jaxb:2.27",
        "5e5cc32c73cda92af420be5302b963c8bd26f26f:org.glassfish.jersey.test-framework:jersey-test-framework-core:2.27",
        "e0f254a263831248d009a9538d0a43412d1ae09a:org.glassfish.jersey.test-framework.providers:jersey-test-framework-provider-grizzly2:2.27",
        "0f1c8853ade0ecf707f5a261c830e98893983813:org.hamcrest:java-hamcrest:2.0.0.0",
    ],
    deps = [
        "javax.xml.bind:jaxb-api:2.3.0",
        "org.glassfish.jersey.test-framework.providers:jersey-test-framework-provider-grizzly2:2.27",
        "org.hamcrest:java-hamcrest:2.0.0.0",
    ],
)

load("@jersey_test_framework_grizzly2//:rules.bzl", "jersey_test_framework_grizzly2_compile")

jersey_test_framework_grizzly2_compile()

# Jettison
maven_repository(
    name = "jettison",
    transitive_deps = [
        "8f0e7bb69242e9ed5bfdf7384b37c1095b0974fc:org.codehaus.jettison:jettison:1.4.0",
    ],
    deps = [
        "org.codehaus.jettison:jettison:1.4.0",
    ],
)

load("@jettison//:rules.bzl", "jettison_compile")

jettison_compile()

# jgrapht
maven_repository(
    name = "jgrapht",
    transitive_deps = [
        "ccaf1e8dd0e4ab1e8891e8d62b315370aef195f9:org.jgrapht:jgrapht-core:1.2.0",
    ],
    deps = [
        "org.jgrapht:jgrapht-core:1.2.0",
    ],
)

load("@jgrapht//:rules.bzl", "jgrapht_compile")

jgrapht_compile()

# JLine3
maven_repository(
    name = "jline3",
    transitive_deps = [
        "da6eb8ebdd131ec41f7e42e7e77b257868279698:org.jline:jline:3.9.0",
    ],
    deps = [
        "org.jline:jline:3.9.0",
    ],
)

load("@jline3//:rules.bzl", "jline3_compile")

jline3_compile()

# json_smart
maven_repository(
    name = "json_smart",
    force = [
        "org.ow2.asm:asm:7.0",
    ],
    transitive_deps = [
        "c592b500269bfde36096641b01238a8350f8aa31:net.minidev:accessors-smart:1.2",
        "007396407491352ce4fa30de92efb158adb76b5b:net.minidev:json-smart:2.3",
        "d74d4ba0dee443f68fb2dcb7fcdb945a2cd89912:org.ow2.asm:asm:7.0",
    ],
    deps = [
        "net.minidev:json-smart:2.3",
    ],
)

load("@json_smart//:rules.bzl", "json_smart_compile")

json_smart_compile()

# jsonassert
maven_repository(
    name = "jsonassert",
    transitive_deps = [
        "fa26d351fe62a6a17f5cda1287c1c6110dec413f:com.vaadin.external.google:android-json:0.0.20131108.vaadin1",
        "6c9d5fe2f59da598d9aefc1cfc6528ff3cf32df3:org.skyscreamer:jsonassert:1.5.0",
    ],
    deps = [
        "org.skyscreamer:jsonassert:1.5.0",
    ],
)

load("@jsonassert//:rules.bzl", "jsonassert_compile")

jsonassert_compile()

# jsonassert
maven_repository(
    name = "jsonpath",
    force = [
        "org.ow2.asm:asm:7.0",
    ],
    transitive_deps = [
        "765a4401ceb2dc8d40553c2075eb80a8fa35c2ae:com.jayway.jsonpath:json-path:2.4.0",
        "c592b500269bfde36096641b01238a8350f8aa31:net.minidev:accessors-smart:1.2",
        "007396407491352ce4fa30de92efb158adb76b5b:net.minidev:json-smart:2.3",
        "d74d4ba0dee443f68fb2dcb7fcdb945a2cd89912:org.ow2.asm:asm:7.0",
        "da76ca59f6a57ee3102f8f9bd9cee742973efa8a:org.slf4j:slf4j-api:1.7.25",
    ],
    deps = [
        "com.jayway.jsonpath:json-path:2.4.0",
    ],
)

load("@jsonpath//:rules.bzl", "jsonpath_compile")

jsonpath_compile()

# JSR305
maven_repository(
    name = "jsr305",
    transitive_deps = [
        "25ea2e8b0c338a877313bd4672d3fe056ea78f0d:com.google.code.findbugs:jsr305:3.0.2",
    ],
    deps = [
        "com.google.code.findbugs:jsr305:3.0.2",
    ],
)

load("@jsr305//:rules.bzl", "jsr305_compile")

jsr305_compile()

# JUnit
maven_repository(
    name = "junit",
    exclude = {
        "junit:junit": [
            "org.hamcrest:hamcrest-core",
        ],
    },
    transitive_deps = [
        "2973d150c0dc1fefe998f834810d68f278ea58ec:junit:junit:4.12",
        "0f1c8853ade0ecf707f5a261c830e98893983813:org.hamcrest:java-hamcrest:2.0.0.0",
    ],
    deps = [
        "junit:junit:4.12",
        "org.hamcrest:java-hamcrest:2.0.0.0",
    ],
)

load("@junit//:rules.bzl", "junit_compile")

junit_compile()

# LZ4
maven_repository(
    name = "lz4",
    transitive_deps = [
        "d36fb639f06aaa4f17307625f80e2e32f815672a:org.lz4:lz4-java:1.5.0",
    ],
    deps = [
        "org.lz4:lz4-java:1.5.0",
    ],
)

load("@lz4//:rules.bzl", "lz4_compile")

lz4_compile()

# Maven-Artifact
maven_repository(
    name = "maven_artifact",
    force = [
        "org.apache.commons:commons-lang3:3.8.1",
    ],
    transitive_deps = [
        "6505a72a097d9270f7a9e7bf42c4238283247755:org.apache.commons:commons-lang3:3.8.1",
        "9c905d5a9c144e6f0d30c9db748090e807fed60e:org.apache.maven:maven-artifact:3.5.4",
        "60eecb6f15abdb1c653ad80abaac6fe188b3feaa:org.codehaus.plexus:plexus-utils:3.1.0",
    ],
    deps = [
        "org.apache.maven:maven-artifact:3.5.4",
    ],
)

load("@maven_artifact//:rules.bzl", "maven_artifact_compile")

maven_artifact_compile()

# Opentracing
maven_repository(
    name = "opentracing_api",
    transitive_deps = [
        "7e54c7a39d508a3268509d9c69dfeea665fc0595:io.opentracing:opentracing-api:0.30.0",
    ],
    deps = [
        "io.opentracing:opentracing-api:0.30.0",
    ],
)

load("@opentracing_api//:rules.bzl", "opentracing_api_compile")

opentracing_api_compile()

#
maven_repository(
    name = "opentracing_contrib_jaxrs",
    transitive_deps = [
        "875bcb1f0afe82d76a72241a290a9c3a79188743:io.opentracing.contrib:opentracing-concurrent:0.0.1",
        "f15b8572c395699fa3ed305993e3a83c226305f0:io.opentracing.contrib:opentracing-jaxrs2:0.0.9",
        "07a7528739d3d5ce0103639fd0a0fc0a73c200c5:io.opentracing.contrib:opentracing-web-servlet-filter:0.0.9",
        "7e54c7a39d508a3268509d9c69dfeea665fc0595:io.opentracing:opentracing-api:0.30.0",
        "0a8e47f081e14acda02dd7446b43a990d5e48a4d:io.opentracing:opentracing-noop:0.30.0",
        "b5b39128cee4ff50b6aee28f9248875fdccd22c6:io.opentracing:opentracing-util:0.30.0",
    ],
    deps = [
        "io.opentracing.contrib:opentracing-jaxrs2:0.0.9",
    ],
)

load("@opentracing_contrib_jaxrs//:rules.bzl", "opentracing_contrib_jaxrs_compile")

opentracing_contrib_jaxrs_compile()

#
maven_repository(
    name = "opentracing_mock",
    transitive_deps = [
        "7e54c7a39d508a3268509d9c69dfeea665fc0595:io.opentracing:opentracing-api:0.30.0",
        "aecf4f635a237a49a1b351f5dd5b2aaea9650733:io.opentracing:opentracing-mock:0.30.0",
        "0a8e47f081e14acda02dd7446b43a990d5e48a4d:io.opentracing:opentracing-noop:0.30.0",
    ],
    deps = [
        "io.opentracing:opentracing-mock:0.30.0",
    ],
)

load("@opentracing_mock//:rules.bzl", "opentracing_mock_compile")

opentracing_mock_compile()

#
maven_repository(
    name = "opentracing_util",
    transitive_deps = [
        "7e54c7a39d508a3268509d9c69dfeea665fc0595:io.opentracing:opentracing-api:0.30.0",
        "0a8e47f081e14acda02dd7446b43a990d5e48a4d:io.opentracing:opentracing-noop:0.30.0",
        "b5b39128cee4ff50b6aee28f9248875fdccd22c6:io.opentracing:opentracing-util:0.30.0",
    ],
    deps = [
        "io.opentracing:opentracing-util:0.30.0",
    ],
)

load("@opentracing_util//:rules.bzl", "opentracing_util_compile")

opentracing_util_compile()

# parboiled
maven_repository(
    name = "parboiled",
    transitive_deps = [
        "d74d4ba0dee443f68fb2dcb7fcdb945a2cd89912:org.ow2.asm:asm:7.0",
        "4b310d20d6f1c6b7197a75f1b5d69f169bc8ac1f:org.ow2.asm:asm-analysis:7.0",
        "29bc62dcb85573af6e62e5b2d735ef65966c4180:org.ow2.asm:asm-tree:7.0",
        "18d4d07010c24405129a6dbb0e92057f8779fb9d:org.ow2.asm:asm-util:7.0",
        "2de1123c7844679a1af247cb5a58c6c2ff0a571c:org.parboiled:parboiled-core:1.3.0",
        "894462ef5b936cd246caab80ba97a7976e206a00:org.parboiled:parboiled-java:1.3.0",
    ],
    deps = [
        "org.parboiled:parboiled-java:1.3.0",
    ],
)

load("@parboiled//:rules.bzl", "parboiled_compile")

parboiled_compile()

# scala_library
maven_repository(
    name = "scala_library",
    transitive_deps = [
        "270fc1cda47bc255f3cd03152ec8c2ed7d224e2b:org.scala-lang:scala-library:2.12.0",
    ],
    deps = [
        "org.scala-lang:scala-library:2.12.0",
    ],
)

load("@scala_library//:rules.bzl", "scala_library_compile")

scala_library_compile()

# slf4j
maven_repository(
    name = "slf4j_api",
    transitive_deps = [
        "da76ca59f6a57ee3102f8f9bd9cee742973efa8a:org.slf4j:slf4j-api:1.7.25",
    ],
    deps = [
        "org.slf4j:slf4j-api:1.7.25",
    ],
)

load("@slf4j_api//:rules.bzl", "slf4j_api_compile")

slf4j_api_compile()

#
maven_repository(
    name = "slf4j_jdk14",
    transitive_deps = [
        "da76ca59f6a57ee3102f8f9bd9cee742973efa8a:org.slf4j:slf4j-api:1.7.25",
        "bccda40ebc8067491b32a88f49615a747d20082d:org.slf4j:slf4j-jdk14:1.7.25",
    ],
    deps = [
        "org.slf4j:slf4j-jdk14:1.7.25",
    ],
)

load("@slf4j_jdk14//:rules.bzl", "slf4j_jdk14_compile")

slf4j_jdk14_compile()

# SQLite
maven_repository(
    name = "sqlite_jdbc",
    transitive_deps = [
        "bd3c4cde613b661871e861eb56656c4c39e1ee0a:org.xerial:sqlite-jdbc:3.25.2",
    ],
    deps = [
        "org.xerial:sqlite-jdbc:3.25.2",
    ],
)

load("@sqlite_jdbc//:rules.bzl", "sqlite_jdbc_compile")

sqlite_jdbc_compile()

# XStream
maven_repository(
    name = "xstream",
    transitive_deps = [
        "bce3282142b63068260f021fcbe48b72e8d71a1a:com.thoughtworks.xstream:xstream:1.4.7",
        "2b8e230d2ab644e4ecaa94db7cdedbc40c805dfa:xmlpull:xmlpull:1.1.3.1",
        "19d4e90b43059058f6e056f794f0ea4030d60b86:xpp3:xpp3_min:1.1.4c",
    ],
    deps = [
        "com.thoughtworks.xstream:xstream:1.4.7",
    ],
)

load("@xstream//:rules.bzl", "xstream_compile")

xstream_compile()

##########################################################
## Third section: tools

# ANTLR4 tool
http_jar(
    name = "antlr4_tool",
    sha256 = "6852386d7975eff29171dae002cc223251510d35f291ae277948f381a7b380b4",
    url = "https://search.maven.org/remotecontent?filepath=org/antlr/antlr4/4.7.2/antlr4-4.7.2-complete.jar",
)
