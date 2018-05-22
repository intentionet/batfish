workspace(name = 'batfish')

##########################################################
# The Maven artifacts depended-upon by our Java projects #
##########################################################

##########################################################
## First section: enable pubref_rules_maven at all
git_repository(
  name = 'org_pubref_rules_maven',
  remote = 'https://github.com/pubref/rules_maven',
  commit = '9c3b07a6d9b195a1192aea3cd78afd1f66c80710',
)

load('@org_pubref_rules_maven//maven:rules.bzl', 'maven_repositories', 'maven_repository')
maven_repositories()

##########################################################
## Second section: per-library, what we depend on

# ANTLR 4
maven_repository(
    name = 'antlr4',
    deps = [
        'org.antlr:antlr4:4.7',
    ],
    transitive_deps = [
        'db9fd4b4c189cf1518db14c67d14a2cfcfbe59f6:com.ibm.icu:icu4j:58.2',
        '457216e8e6578099ae63667bb1e4439235892028:org.abego.treelayout:org.abego.treelayout.core:1.0.3',
        '0a1c55e974f8a94d78e2348fa6ff63f4fa1fae64:org.antlr:ST4:4.0.8',
        'cd6df46532bccabd8127c18c9ca5ef481962e931:org.antlr:antlr4:4.7',
        '30b13b7efc55b7feea667691509cf59902375001:org.antlr:antlr4-runtime:4.7',
        'cd9cd41361c155f3af0f653009dcecb08d8b4afd:org.antlr:antlr-runtime:3.5.2',
        '3178f73569fd7a1e5ffc464e680f7a8cc784b85a:org.glassfish:javax.json:1.0.4',
    ],

)
load('@antlr4//:rules.bzl', 'antlr4_compile')
antlr4_compile()

# ANTLR 4 Runtime
maven_repository(
    name = 'antlr4_runtime',
    deps = [
        'org.antlr:antlr4-runtime:4.7',
    ],
    transitive_deps = [
        '30b13b7efc55b7feea667691509cf59902375001:org.antlr:antlr4-runtime:4.7',
    ],
)
load('@antlr4_runtime//:rules.bzl', 'antlr4_runtime_compile')
antlr4_runtime_compile()

# AutoService
maven_repository(
    name = 'auto_service',
    deps = [
        'com.google.auto.service:auto-service:1.0-rc3',
    ],
    force = [
        'com.google.guava:guava:22.0',
    ],
    transitive_deps = [
        '4073ab16ab4aceb9a217273da6442166bf51ae16:com.google.auto:auto-common:0.3',
        '35c5d43b0332b8f94d473f9fee5fb1d74b5e0056:com.google.auto.service:auto-service:1.0-rc3',
        '40719ea6961c0cb6afaeb6a921eaa1f6afd4cfdf:com.google.code.findbugs:jsr305:1.3.9',
        '5f65affce1684999e2f4024983835efc3504012e:com.google.errorprone:error_prone_annotations:2.0.18',
        '3564ef3803de51fb0530a8377ec6100b33b0d073:com.google.guava:guava:22.0',
        'ed28ded51a8b1c6b112568def5f4b455e6809019:com.google.j2objc:j2objc-annotations:1.1',
        '775b7e22fb10026eed3f86e8dc556dfafe35f2d5:org.codehaus.mojo:animal-sniffer-annotations:1.14',
    ],
)
load('@auto_service//:rules.bzl', 'auto_service_compile')
auto_service_compile()

# azure-storage
maven_repository(
    name = 'azure_storage',
    deps = [
        'com.microsoft.azure:azure-storage:2.0.0',
    ],
	  force = [
        'com.fasterxml.jackson.core:jackson-core:2.9.0',
        'org.apache.commons:commons-lang3:3.7',
        'org.slf4j:slf4j-api:1.7.25',
    ],
    transitive_deps = [
        '88e7c6220be3b3497b3074d3fc7754213289b987:com.fasterxml.jackson.core:jackson-core:2.9.0',
        'b970c65a38da0569013e0c76de7c404f842496c2:com.microsoft.azure:azure-storage:2.0.0',
        '557edd918fd41f9260963583ebf5a61a43a6b423:org.apache.commons:commons-lang3:3.7',
        'da76ca59f6a57ee3102f8f9bd9cee742973efa8a:org.slf4j:slf4j-api:1.7.25',
    ],
)
load('@azure_storage//:rules.bzl', 'azure_storage_compile')
azure_storage_compile()

# Commons
maven_repository(
    name = 'commons_beanutils',
    deps = [
        'commons-beanutils:commons-beanutils:1.9.3',
    ],
    transitive_deps = [
        'c845703de334ddc6b4b3cd26835458cb1cba1f3d:commons-beanutils:commons-beanutils:1.9.3',
        '8ad72fe39fa8c91eaaf12aadb21e0c3661fe26d5:commons-collections:commons-collections:3.2.2',
        '4bfc12adfe4842bf07b657f0369c4cb522955686:commons-logging:commons-logging:1.2',
    ],
)
load('@commons_beanutils//:rules.bzl', 'commons_beanutils_compile')
commons_beanutils_compile()
#
maven_repository(
    name = 'commons_cli',
    deps = [
        'commons-cli:commons-cli:1.3.1',
    ],
    transitive_deps = [
        '1303efbc4b181e5a58bf2e967dc156a3132b97c0:commons-cli:commons-cli:1.3.1',
    ],
)
load('@commons_cli//:rules.bzl', 'commons_cli_compile')
commons_cli_compile()
#
maven_repository(
    name = 'commons_collections4',
    deps = [
        'org.apache.commons:commons-collections4:4.1',
    ],
    transitive_deps = [
        'a4cf4688fe1c7e3a63aa636cc96d013af537768e:org.apache.commons:commons-collections4:4.1',
    ],
)
load('@commons_collections4//:rules.bzl', 'commons_collections4_compile')
commons_collections4_compile()
#
maven_repository(
    name = 'commons_configuration2',
    deps = [
        'org.apache.commons:commons-configuration2:2.2',
    ],
    force = [
        'org.apache.commons:commons-lang3:3.7',
    ],
    transitive_deps = [
        '4bfc12adfe4842bf07b657f0369c4cb522955686:commons-logging:commons-logging:1.2',
        '5ee9a0c14ac47e8c78d7f573bea1867f0d3b2894:org.apache.commons:commons-configuration2:2.2',
        '557edd918fd41f9260963583ebf5a61a43a6b423:org.apache.commons:commons-lang3:3.7',
    ],
)
load('@commons_configuration2//:rules.bzl', 'commons_configuration2_compile')
commons_configuration2_compile()
#
maven_repository(
    name = 'commons_lang3',
    deps = [
        'org.apache.commons:commons-lang3:3.7',
    ],
    transitive_deps = [
        '557edd918fd41f9260963583ebf5a61a43a6b423:org.apache.commons:commons-lang3:3.7',
    ],
)
load('@commons_lang3//:rules.bzl', 'commons_lang3_compile')
commons_lang3_compile()
#
maven_repository(
    name = 'commons_io',
    deps = [
        'commons-io:commons-io:2.4',
    ],
    transitive_deps = [
        'b1b6ea3b7e4aa4f492509a4952029cd8e48019ad:commons-io:commons-io:2.4',
    ],
)
load('@commons_io//:rules.bzl', 'commons_io_compile')
commons_io_compile()

# errorprone-annotations
maven_repository(
    name = 'errorprone_annotations',
    deps = [
        'com.google.errorprone:error_prone_annotations:2.0.18',
    ],
    transitive_deps = [
        '5f65affce1684999e2f4024983835efc3504012e:com.google.errorprone:error_prone_annotations:2.0.18',
    ],
)
load('@errorprone_annotations//:rules.bzl', 'errorprone_annotations_compile')
errorprone_annotations_compile()

# Grizzly
maven_repository(
    name = 'grizzly_framework',
    deps = [
        'org.glassfish.grizzly:grizzly-framework:2.3.28',
    ],
    transitive_deps = [
        '23a90f6316b3776699b173ccf9394c69d15b7e9c:org.glassfish.grizzly:grizzly-framework:2.3.28',
    ],
)
load('@grizzly_framework//:rules.bzl', 'grizzly_framework_compile')
grizzly_framework_compile()
#
maven_repository(
    name = 'grizzly_server',
    deps = [
        'org.glassfish.grizzly:grizzly-http-server:2.3.28',
    ],
    transitive_deps = [
        '23a90f6316b3776699b173ccf9394c69d15b7e9c:org.glassfish.grizzly:grizzly-framework:2.3.28',
        'bb34b4e7fbb66b53ac6d428dcc99f5925c9ff7bd:org.glassfish.grizzly:grizzly-http:2.3.28',
        '13bc9a63dae3a0a623b52fe71753d5413d134540:org.glassfish.grizzly:grizzly-http-server:2.3.28',
    ],
)
load('@grizzly_server//:rules.bzl', 'grizzly_server_compile')
grizzly_server_compile()

# Guava
maven_repository(
    name = 'guava',
    deps = [
        'com.google.guava:guava:22.0',
    ],
    transitive_deps = [
        '40719ea6961c0cb6afaeb6a921eaa1f6afd4cfdf:com.google.code.findbugs:jsr305:1.3.9',
        '5f65affce1684999e2f4024983835efc3504012e:com.google.errorprone:error_prone_annotations:2.0.18',
        '3564ef3803de51fb0530a8377ec6100b33b0d073:com.google.guava:guava:22.0',
        'ed28ded51a8b1c6b112568def5f4b455e6809019:com.google.j2objc:j2objc-annotations:1.1',
        '775b7e22fb10026eed3f86e8dc556dfafe35f2d5:org.codehaus.mojo:animal-sniffer-annotations:1.14',
    ],
)
load('@guava//:rules.bzl', 'guava_compile')
guava_compile()

# Guava-testlib
maven_repository(
    name = 'guava_testlib',
    deps = [
        'com.google.guava:guava-testlib:22.0',
        'org.hamcrest:java-hamcrest:2.0.0.0',
    ],
    exclude = {
        'com.google.guava:guava-testlib': [
            'org.hamcrest:hamcrest-core',
        ],
    },
    force = [
        'junit:junit:4.12',
    ],
    transitive_deps = [
        '40719ea6961c0cb6afaeb6a921eaa1f6afd4cfdf:com.google.code.findbugs:jsr305:1.3.9',
        '5f65affce1684999e2f4024983835efc3504012e:com.google.errorprone:error_prone_annotations:2.0.18',
        '3564ef3803de51fb0530a8377ec6100b33b0d073:com.google.guava:guava:22.0',
        '3be1b88f1cfc6592acbcbfe1f3a420f79eb2b146:com.google.guava:guava-testlib:22.0',
        'ed28ded51a8b1c6b112568def5f4b455e6809019:com.google.j2objc:j2objc-annotations:1.1',
        '2973d150c0dc1fefe998f834810d68f278ea58ec:junit:junit:4.12',
        '775b7e22fb10026eed3f86e8dc556dfafe35f2d5:org.codehaus.mojo:animal-sniffer-annotations:1.14',
        '0f1c8853ade0ecf707f5a261c830e98893983813:org.hamcrest:java-hamcrest:2.0.0.0',
    ],
)
load('@guava_testlib//:rules.bzl', 'guava_testlib_compile')
guava_testlib_compile()

# Hamcrest
maven_repository(
    name = 'hamcrest',
    deps = [
        'org.hamcrest:java-hamcrest:2.0.0.0',
    ],
    transitive_deps = [
        '0f1c8853ade0ecf707f5a261c830e98893983813:org.hamcrest:java-hamcrest:2.0.0.0',
    ],
)
load('@hamcrest//:rules.bzl', 'hamcrest_compile')
hamcrest_compile()

# jackson
maven_repository(
    name = 'jackson_annotations',
    deps = [
        'com.fasterxml.jackson.core:jackson-annotations:2.9.0'
    ],
    transitive_deps = [
        '07c10d545325e3a6e72e06381afe469fd40eb701:com.fasterxml.jackson.core:jackson-annotations:2.9.0',
    ],
)
load('@jackson_annotations//:rules.bzl', 'jackson_annotations_compile')
jackson_annotations_compile()
#
maven_repository(
    name = 'jackson_core',
    deps = [
        'com.fasterxml.jackson.core:jackson-core:2.9.0'
    ],
    transitive_deps = [
        '88e7c6220be3b3497b3074d3fc7754213289b987:com.fasterxml.jackson.core:jackson-core:2.9.0',
    ],
)
load('@jackson_core//:rules.bzl', 'jackson_core_compile')
jackson_core_compile()
#
maven_repository(
    name = 'jackson_databind',
    deps = [
        'com.fasterxml.jackson.core:jackson-databind:2.9.0'
    ],
    transitive_deps = [
        '07c10d545325e3a6e72e06381afe469fd40eb701:com.fasterxml.jackson.core:jackson-annotations:2.9.0',
        '88e7c6220be3b3497b3074d3fc7754213289b987:com.fasterxml.jackson.core:jackson-core:2.9.0',
        '14fb5f088cc0b0dc90a73ba745bcade4961a3ee3:com.fasterxml.jackson.core:jackson-databind:2.9.0',
    ],
)
load('@jackson_databind//:rules.bzl', 'jackson_databind_compile')
jackson_databind_compile()
#
maven_repository(
    name = 'jackson_guava',
    deps = [
        'com.fasterxml.jackson.datatype:jackson-datatype-guava:2.9.0'
    ],
    force = [
        'com.google.guava:guava:22.0',
    ],
    transitive_deps = [
        '07c10d545325e3a6e72e06381afe469fd40eb701:com.fasterxml.jackson.core:jackson-annotations:2.9.0',
        '88e7c6220be3b3497b3074d3fc7754213289b987:com.fasterxml.jackson.core:jackson-core:2.9.0',
        '14fb5f088cc0b0dc90a73ba745bcade4961a3ee3:com.fasterxml.jackson.core:jackson-databind:2.9.0',
        '5b242c510a315d3c116a7c2dd8c5a225aedfc1db:com.fasterxml.jackson.datatype:jackson-datatype-guava:2.9.0',
        '40719ea6961c0cb6afaeb6a921eaa1f6afd4cfdf:com.google.code.findbugs:jsr305:1.3.9',
        '5f65affce1684999e2f4024983835efc3504012e:com.google.errorprone:error_prone_annotations:2.0.18',
        '3564ef3803de51fb0530a8377ec6100b33b0d073:com.google.guava:guava:22.0',
        'ed28ded51a8b1c6b112568def5f4b455e6809019:com.google.j2objc:j2objc-annotations:1.1',
        '775b7e22fb10026eed3f86e8dc556dfafe35f2d5:org.codehaus.mojo:animal-sniffer-annotations:1.14',
    ],
)
load('@jackson_guava//:rules.bzl', 'jackson_guava_compile')
jackson_guava_compile()
#
maven_repository(
    name = 'jackson_jdk8',
    deps = [
        'com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.9.0'
    ],
    transitive_deps = [
        '07c10d545325e3a6e72e06381afe469fd40eb701:com.fasterxml.jackson.core:jackson-annotations:2.9.0',
        '88e7c6220be3b3497b3074d3fc7754213289b987:com.fasterxml.jackson.core:jackson-core:2.9.0',
        '14fb5f088cc0b0dc90a73ba745bcade4961a3ee3:com.fasterxml.jackson.core:jackson-databind:2.9.0',
        'c63bfee268b6330b80ee9b151e0aba6d105bda80:com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.9.0',
    ],
)
load('@jackson_jdk8//:rules.bzl', 'jackson_jdk8_compile')
jackson_jdk8_compile()
#
maven_repository(
    name = 'jackson_jsr310',
    deps = [
        'com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.9.0'
    ],
    transitive_deps = [
        '07c10d545325e3a6e72e06381afe469fd40eb701:com.fasterxml.jackson.core:jackson-annotations:2.9.0',
        '88e7c6220be3b3497b3074d3fc7754213289b987:com.fasterxml.jackson.core:jackson-core:2.9.0',
        '14fb5f088cc0b0dc90a73ba745bcade4961a3ee3:com.fasterxml.jackson.core:jackson-databind:2.9.0',
        '65fd41b086a7451903c9ee216c7699030c449d9c:com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.9.0',
    ],
)
load('@jackson_jsr310//:rules.bzl', 'jackson_jsr310_compile')
jackson_jsr310_compile()
#
maven_repository(
    name = 'jackson_jaxrs_base',
    deps = [
        'com.fasterxml.jackson.jaxrs:jackson-jaxrs-base:2.9.0'
    ],
    transitive_deps = [
        '07c10d545325e3a6e72e06381afe469fd40eb701:com.fasterxml.jackson.core:jackson-annotations:2.9.0',
        '88e7c6220be3b3497b3074d3fc7754213289b987:com.fasterxml.jackson.core:jackson-core:2.9.0',
        '14fb5f088cc0b0dc90a73ba745bcade4961a3ee3:com.fasterxml.jackson.core:jackson-databind:2.9.0',
        'e16e621cae8edecc101470b54f5ccf1ebc7f468f:com.fasterxml.jackson.jaxrs:jackson-jaxrs-base:2.9.0',
    ],
)
load('@jackson_jaxrs_base//:rules.bzl', 'jackson_jaxrs_base_compile')
jackson_jaxrs_base_compile()

# Jaeger
maven_repository(
    name = 'jaeger_core',
    deps = [
        'com.uber.jaeger:jaeger-core:0.21.0',
    ],
    force = [
        'commons-logging:commons-logging:1.2',
        'org.slf4j:slf4j-api:1.7.25',
    ],
    transitive_deps = [
        'c4ba5371a29ac9b2ad6129b1d39ea38750043eff:com.google.code.gson:gson:2.8.0',
        '4d060ca3190df0eda4dc13415532a12e15ca5f11:com.squareup.okhttp3:okhttp:3.8.1',
        'a9283170b7305c8d92d25aff02a6ab7e45d06cbe:com.squareup.okio:okio:1.13.0',
        '3afc7a32a93b3ad1587d9ae3308ff9a93dcc0573:com.uber.jaeger:jaeger-core:0.21.0',
        'd4211908c29a740a08bf5d4c71e22024c7ccda13:com.uber.jaeger:jaeger-thrift:0.21.0',
        'b7f0fc8f61ecadeb3695f0b9464755eee44374d4:commons-codec:commons-codec:1.6',
        '4bfc12adfe4842bf07b657f0369c4cb522955686:commons-logging:commons-logging:1.2',
        '7e54c7a39d508a3268509d9c69dfeea665fc0595:io.opentracing:opentracing-api:0.30.0',
        '0a8e47f081e14acda02dd7446b43a990d5e48a4d:io.opentracing:opentracing-noop:0.30.0',
        'b5b39128cee4ff50b6aee28f9248875fdccd22c6:io.opentracing:opentracing-util:0.30.0',
        '666e26e76f2e87d84e4f16acb546481ae1b8e9a6:org.apache.httpcomponents:httpclient:4.2.5',
        '3b7f38df6de5dd8b500e602ae8c2dd5ee446f883:org.apache.httpcomponents:httpcore:4.2.4',
        '9b067e2e2c5291e9f0d8b3561b1654286e6d81ee:org.apache.thrift:libthrift:0.9.2',
        'da76ca59f6a57ee3102f8f9bd9cee742973efa8a:org.slf4j:slf4j-api:1.7.25',
    ],
)
load('@jaeger_core//:rules.bzl', 'jaeger_core_compile')
jaeger_core_compile()

# JAX-RS
maven_repository(
    name = 'jaxrs_api',
    deps = [
        'javax.ws.rs:javax.ws.rs-api:2.0.1',
    ],
    transitive_deps = [
        '104e9c2b5583cfcfeac0402316221648d6d8ea6b:javax.ws.rs:javax.ws.rs-api:2.0.1',
    ],
)
load('@jaxrs_api//:rules.bzl', 'jaxrs_api_compile')
jaxrs_api_compile()

# javax-annotation
maven_repository(
    name = 'javax_annotation',
    deps = [
        'javax.annotation:javax.annotation-api:1.2',
    ],
    transitive_deps = [
        '479c1e06db31c432330183f5cae684163f186146:javax.annotation:javax.annotation-api:1.2',
    ],
)
load('@javax_annotation//:rules.bzl', 'javax_annotation_compile')
javax_annotation_compile()

# Jersey
maven_repository(
    name = 'jersey_client',
    deps = [
        'org.glassfish.jersey.core:jersey-client:2.25.1',
    ],
    transitive_deps = [
        '479c1e06db31c432330183f5cae684163f186146:javax.annotation:javax.annotation-api:1.2',
        '6975da39a7040257bd51d21a231b76c915872d38:javax.inject:javax.inject:1',
        '104e9c2b5583cfcfeac0402316221648d6d8ea6b:javax.ws.rs:javax.ws.rs-api:2.0.1',
        '6af37c3f8ec6f9e9653ec837eb508da28ce443cd:org.glassfish.hk2.external:aopalliance-repackaged:2.5.0-b32',
        'b2fa50c8186a38728c35fe6a9da57ce4cc806923:org.glassfish.hk2.external:javax.inject:2.5.0-b32',
        '6a576c9653832ce610b80a2f389374ef19d96171:org.glassfish.hk2:hk2-api:2.5.0-b32',
        '195474f8ad0a8d130e9ea949a771bcf1215fc33b:org.glassfish.hk2:hk2-locator:2.5.0-b32',
        '5108a926988c4ceda7f1e681dddfe3101454a002:org.glassfish.hk2:hk2-utils:2.5.0-b32',
        '4ed2b2d4738aed5786cfa64cba5a332779c4c708:org.glassfish.hk2:osgi-resource-locator:1.0.1',
        'a2bb4f8208e134cf2cf71dfb8824e42942f7bd06:org.glassfish.jersey.bundles.repackaged:jersey-guava:2.25.1',
        '4d563b1f93352ee9fad597e9e1daf2c6159993c6:org.glassfish.jersey.core:jersey-client:2.25.1',
        '2438ce68d4907046095ab54aa83a6092951b4bbb:org.glassfish.jersey.core:jersey-common:2.25.1',
        'a9cbcdfb7e9f86fbc74d3afae65f2248bfbf82a0:org.javassist:javassist:3.20.0-GA',
    ],
)
load('@jersey_client//:rules.bzl', 'jersey_client_compile')
jersey_client_compile()
#
maven_repository(
    name = 'jersey_common',
    deps = [
        'org.glassfish.jersey.core:jersey-common:2.25.1',
    ],
    transitive_deps = [
        '479c1e06db31c432330183f5cae684163f186146:javax.annotation:javax.annotation-api:1.2',
        '6975da39a7040257bd51d21a231b76c915872d38:javax.inject:javax.inject:1',
        '104e9c2b5583cfcfeac0402316221648d6d8ea6b:javax.ws.rs:javax.ws.rs-api:2.0.1',
        '6af37c3f8ec6f9e9653ec837eb508da28ce443cd:org.glassfish.hk2.external:aopalliance-repackaged:2.5.0-b32',
        'b2fa50c8186a38728c35fe6a9da57ce4cc806923:org.glassfish.hk2.external:javax.inject:2.5.0-b32',
        '6a576c9653832ce610b80a2f389374ef19d96171:org.glassfish.hk2:hk2-api:2.5.0-b32',
        '195474f8ad0a8d130e9ea949a771bcf1215fc33b:org.glassfish.hk2:hk2-locator:2.5.0-b32',
        '5108a926988c4ceda7f1e681dddfe3101454a002:org.glassfish.hk2:hk2-utils:2.5.0-b32',
        '4ed2b2d4738aed5786cfa64cba5a332779c4c708:org.glassfish.hk2:osgi-resource-locator:1.0.1',
        'a2bb4f8208e134cf2cf71dfb8824e42942f7bd06:org.glassfish.jersey.bundles.repackaged:jersey-guava:2.25.1',
        '2438ce68d4907046095ab54aa83a6092951b4bbb:org.glassfish.jersey.core:jersey-common:2.25.1',
        'a9cbcdfb7e9f86fbc74d3afae65f2248bfbf82a0:org.javassist:javassist:3.20.0-GA',
    ],
)
load('@jersey_common//:rules.bzl', 'jersey_common_compile')
jersey_common_compile()
#
maven_repository(
    name = 'jersey_container_grizzly2',
    deps = [
        'org.glassfish.jersey.containers:jersey-container-grizzly2-http:2.25.1',
    ],
    transitive_deps = [
        '479c1e06db31c432330183f5cae684163f186146:javax.annotation:javax.annotation-api:1.2',
        '6975da39a7040257bd51d21a231b76c915872d38:javax.inject:javax.inject:1',
        '8613ae82954779d518631e05daa73a6a954817d5:javax.validation:validation-api:1.1.0.Final',
        '104e9c2b5583cfcfeac0402316221648d6d8ea6b:javax.ws.rs:javax.ws.rs-api:2.0.1',
        '23a90f6316b3776699b173ccf9394c69d15b7e9c:org.glassfish.grizzly:grizzly-framework:2.3.28',
        'bb34b4e7fbb66b53ac6d428dcc99f5925c9ff7bd:org.glassfish.grizzly:grizzly-http:2.3.28',
        '13bc9a63dae3a0a623b52fe71753d5413d134540:org.glassfish.grizzly:grizzly-http-server:2.3.28',
        '6af37c3f8ec6f9e9653ec837eb508da28ce443cd:org.glassfish.hk2.external:aopalliance-repackaged:2.5.0-b32',
        'b2fa50c8186a38728c35fe6a9da57ce4cc806923:org.glassfish.hk2.external:javax.inject:2.5.0-b32',
        '6a576c9653832ce610b80a2f389374ef19d96171:org.glassfish.hk2:hk2-api:2.5.0-b32',
        '195474f8ad0a8d130e9ea949a771bcf1215fc33b:org.glassfish.hk2:hk2-locator:2.5.0-b32',
        '5108a926988c4ceda7f1e681dddfe3101454a002:org.glassfish.hk2:hk2-utils:2.5.0-b32',
        '4ed2b2d4738aed5786cfa64cba5a332779c4c708:org.glassfish.hk2:osgi-resource-locator:1.0.1',
        'a2bb4f8208e134cf2cf71dfb8824e42942f7bd06:org.glassfish.jersey.bundles.repackaged:jersey-guava:2.25.1',
        '6cfd264755979bafbc65dba660bbc898b25998d0:org.glassfish.jersey.containers:jersey-container-grizzly2-http:2.25.1',
        '4d563b1f93352ee9fad597e9e1daf2c6159993c6:org.glassfish.jersey.core:jersey-client:2.25.1',
        '2438ce68d4907046095ab54aa83a6092951b4bbb:org.glassfish.jersey.core:jersey-common:2.25.1',
        '276e2ee0fd1cdabf99357fce560c5baab675b1a2:org.glassfish.jersey.core:jersey-server:2.25.1',
        '0d7da0beeed5614a3bfd882662faec602699e24b:org.glassfish.jersey.media:jersey-media-jaxb:2.25.1',
        'a9cbcdfb7e9f86fbc74d3afae65f2248bfbf82a0:org.javassist:javassist:3.20.0-GA',
    ],
)
load('@jersey_container_grizzly2//:rules.bzl', 'jersey_container_grizzly2_compile')
jersey_container_grizzly2_compile()
#
maven_repository(
    name = 'jersey_media_jackson',
    deps = [
        'org.glassfish.jersey.media:jersey-media-json-jackson:2.25.1',
    ],
    force = [
        'com.fasterxml.jackson.core:jackson-annotations:2.9.0',
        'com.fasterxml.jackson.core:jackson-core:2.9.0',
        'com.fasterxml.jackson.jaxrs:jackson-jaxrs-base:2.9.0',
        'com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider:2.9.0',
        'com.fasterxml.jackson.module:jackson-module-jaxb-annotations:2.9.0',
    ],
    transitive_deps = [
        '07c10d545325e3a6e72e06381afe469fd40eb701:com.fasterxml.jackson.core:jackson-annotations:2.9.0',
        '88e7c6220be3b3497b3074d3fc7754213289b987:com.fasterxml.jackson.core:jackson-core:2.9.0',
        '14fb5f088cc0b0dc90a73ba745bcade4961a3ee3:com.fasterxml.jackson.core:jackson-databind:2.9.0',
        'e16e621cae8edecc101470b54f5ccf1ebc7f468f:com.fasterxml.jackson.jaxrs:jackson-jaxrs-base:2.9.0',
        '0eddf41a27c709f1b0c05a02bfaa80497d6d0e1c:com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider:2.9.0',
        '2d1fb33f57102810f8f74fe28bebb59d2b23312a:com.fasterxml.jackson.module:jackson-module-jaxb-annotations:2.9.0',
        '479c1e06db31c432330183f5cae684163f186146:javax.annotation:javax.annotation-api:1.2',
        '6975da39a7040257bd51d21a231b76c915872d38:javax.inject:javax.inject:1',
        '104e9c2b5583cfcfeac0402316221648d6d8ea6b:javax.ws.rs:javax.ws.rs-api:2.0.1',
        '6af37c3f8ec6f9e9653ec837eb508da28ce443cd:org.glassfish.hk2.external:aopalliance-repackaged:2.5.0-b32',
        'b2fa50c8186a38728c35fe6a9da57ce4cc806923:org.glassfish.hk2.external:javax.inject:2.5.0-b32',
        '6a576c9653832ce610b80a2f389374ef19d96171:org.glassfish.hk2:hk2-api:2.5.0-b32',
        '195474f8ad0a8d130e9ea949a771bcf1215fc33b:org.glassfish.hk2:hk2-locator:2.5.0-b32',
        '5108a926988c4ceda7f1e681dddfe3101454a002:org.glassfish.hk2:hk2-utils:2.5.0-b32',
        '4ed2b2d4738aed5786cfa64cba5a332779c4c708:org.glassfish.hk2:osgi-resource-locator:1.0.1',
        'a2bb4f8208e134cf2cf71dfb8824e42942f7bd06:org.glassfish.jersey.bundles.repackaged:jersey-guava:2.25.1',
        '2438ce68d4907046095ab54aa83a6092951b4bbb:org.glassfish.jersey.core:jersey-common:2.25.1',
        '4a5805060f796ec2c9bb1ba0ce91c1db6d889524:org.glassfish.jersey.ext:jersey-entity-filtering:2.25.1',
        '19d1e4276eb7b6386640c344d9e5c01eba7eae5d:org.glassfish.jersey.media:jersey-media-json-jackson:2.25.1',
        'a9cbcdfb7e9f86fbc74d3afae65f2248bfbf82a0:org.javassist:javassist:3.20.0-GA',
    ],
)
load('@jersey_media_jackson//:rules.bzl', 'jersey_media_jackson_compile')
jersey_media_jackson_compile()
#
maven_repository(
    name = 'jersey_media_jettison',
    deps = [
        'org.glassfish.jersey.media:jersey-media-json-jettison:2.25.1',
    ],
    force = [
        'org.codehaus.jettison:jettison:1.3.8',
    ],
    transitive_deps = [
        '479c1e06db31c432330183f5cae684163f186146:javax.annotation:javax.annotation-api:1.2',
        '6975da39a7040257bd51d21a231b76c915872d38:javax.inject:javax.inject:1',
        '104e9c2b5583cfcfeac0402316221648d6d8ea6b:javax.ws.rs:javax.ws.rs-api:2.0.1',
        '6ad07db4f344d965455c237907f65420ced10ae1:org.codehaus.jettison:jettison:1.3.8',
        '6af37c3f8ec6f9e9653ec837eb508da28ce443cd:org.glassfish.hk2.external:aopalliance-repackaged:2.5.0-b32',
        'b2fa50c8186a38728c35fe6a9da57ce4cc806923:org.glassfish.hk2.external:javax.inject:2.5.0-b32',
        '6a576c9653832ce610b80a2f389374ef19d96171:org.glassfish.hk2:hk2-api:2.5.0-b32',
        '195474f8ad0a8d130e9ea949a771bcf1215fc33b:org.glassfish.hk2:hk2-locator:2.5.0-b32',
        '5108a926988c4ceda7f1e681dddfe3101454a002:org.glassfish.hk2:hk2-utils:2.5.0-b32',
        '4ed2b2d4738aed5786cfa64cba5a332779c4c708:org.glassfish.hk2:osgi-resource-locator:1.0.1',
        'a2bb4f8208e134cf2cf71dfb8824e42942f7bd06:org.glassfish.jersey.bundles.repackaged:jersey-guava:2.25.1',
        '2438ce68d4907046095ab54aa83a6092951b4bbb:org.glassfish.jersey.core:jersey-common:2.25.1',
        '0d7da0beeed5614a3bfd882662faec602699e24b:org.glassfish.jersey.media:jersey-media-jaxb:2.25.1',
        'c4e22208dff383be75a5d763f9f41b169168cc11:org.glassfish.jersey.media:jersey-media-json-jettison:2.25.1',
        'a9cbcdfb7e9f86fbc74d3afae65f2248bfbf82a0:org.javassist:javassist:3.20.0-GA',
    ],
)
load('@jersey_media_jettison//:rules.bzl', 'jersey_media_jettison_compile')
jersey_media_jettison_compile()
#
maven_repository(
    name = 'jersey_media_multipart',
    deps = [
        'org.glassfish.jersey.media:jersey-media-multipart:2.25.1',
    ],
    transitive_deps = [
        '479c1e06db31c432330183f5cae684163f186146:javax.annotation:javax.annotation-api:1.2',
        '6975da39a7040257bd51d21a231b76c915872d38:javax.inject:javax.inject:1',
        '104e9c2b5583cfcfeac0402316221648d6d8ea6b:javax.ws.rs:javax.ws.rs-api:2.0.1',
        '6af37c3f8ec6f9e9653ec837eb508da28ce443cd:org.glassfish.hk2.external:aopalliance-repackaged:2.5.0-b32',
        'b2fa50c8186a38728c35fe6a9da57ce4cc806923:org.glassfish.hk2.external:javax.inject:2.5.0-b32',
        '6a576c9653832ce610b80a2f389374ef19d96171:org.glassfish.hk2:hk2-api:2.5.0-b32',
        '195474f8ad0a8d130e9ea949a771bcf1215fc33b:org.glassfish.hk2:hk2-locator:2.5.0-b32',
        '5108a926988c4ceda7f1e681dddfe3101454a002:org.glassfish.hk2:hk2-utils:2.5.0-b32',
        '4ed2b2d4738aed5786cfa64cba5a332779c4c708:org.glassfish.hk2:osgi-resource-locator:1.0.1',
        'a2bb4f8208e134cf2cf71dfb8824e42942f7bd06:org.glassfish.jersey.bundles.repackaged:jersey-guava:2.25.1',
        '2438ce68d4907046095ab54aa83a6092951b4bbb:org.glassfish.jersey.core:jersey-common:2.25.1',
        '1d2db0078ee1b740c4e7ec7413d328a8a7e1c480:org.glassfish.jersey.media:jersey-media-multipart:2.25.1',
        'a9cbcdfb7e9f86fbc74d3afae65f2248bfbf82a0:org.javassist:javassist:3.20.0-GA',
        '41c913d791e16f93bc712a8c8a30bb64daa2e9bd:org.jvnet.mimepull:mimepull:1.9.6',
    ],
)
load('@jersey_media_multipart//:rules.bzl', 'jersey_media_multipart_compile')
jersey_media_multipart_compile()
#
maven_repository(
    name = 'jersey_server',
    deps = [
        'org.glassfish.jersey.core:jersey-server:2.25.1',
    ],
    transitive_deps = [
        '479c1e06db31c432330183f5cae684163f186146:javax.annotation:javax.annotation-api:1.2',
        '6975da39a7040257bd51d21a231b76c915872d38:javax.inject:javax.inject:1',
        '8613ae82954779d518631e05daa73a6a954817d5:javax.validation:validation-api:1.1.0.Final',
        '104e9c2b5583cfcfeac0402316221648d6d8ea6b:javax.ws.rs:javax.ws.rs-api:2.0.1',
        '6af37c3f8ec6f9e9653ec837eb508da28ce443cd:org.glassfish.hk2.external:aopalliance-repackaged:2.5.0-b32',
        'b2fa50c8186a38728c35fe6a9da57ce4cc806923:org.glassfish.hk2.external:javax.inject:2.5.0-b32',
        '6a576c9653832ce610b80a2f389374ef19d96171:org.glassfish.hk2:hk2-api:2.5.0-b32',
        '195474f8ad0a8d130e9ea949a771bcf1215fc33b:org.glassfish.hk2:hk2-locator:2.5.0-b32',
        '5108a926988c4ceda7f1e681dddfe3101454a002:org.glassfish.hk2:hk2-utils:2.5.0-b32',
        '4ed2b2d4738aed5786cfa64cba5a332779c4c708:org.glassfish.hk2:osgi-resource-locator:1.0.1',
        'a2bb4f8208e134cf2cf71dfb8824e42942f7bd06:org.glassfish.jersey.bundles.repackaged:jersey-guava:2.25.1',
        '4d563b1f93352ee9fad597e9e1daf2c6159993c6:org.glassfish.jersey.core:jersey-client:2.25.1',
        '2438ce68d4907046095ab54aa83a6092951b4bbb:org.glassfish.jersey.core:jersey-common:2.25.1',
        '276e2ee0fd1cdabf99357fce560c5baab675b1a2:org.glassfish.jersey.core:jersey-server:2.25.1',
        '0d7da0beeed5614a3bfd882662faec602699e24b:org.glassfish.jersey.media:jersey-media-jaxb:2.25.1',
        'a9cbcdfb7e9f86fbc74d3afae65f2248bfbf82a0:org.javassist:javassist:3.20.0-GA',
    ],
)
load('@jersey_server//:rules.bzl', 'jersey_server_compile')
jersey_server_compile()
#
maven_repository(
    name = 'jersey_test_framework',
    deps = [
        'org.glassfish.jersey.test-framework:jersey-test-framework-core:2.25.1',
        'org.hamcrest:java-hamcrest:2.0.0.0',
    ],
    exclude = {
        'org.glassfish.jersey.test-framework:jersey-test-framework-core': [
            'org.hamcrest:hamcrest-core',
        ],
    },
    transitive_deps = [
        '479c1e06db31c432330183f5cae684163f186146:javax.annotation:javax.annotation-api:1.2',
        '6975da39a7040257bd51d21a231b76c915872d38:javax.inject:javax.inject:1',
        '6bf0ebb7efd993e222fc1112377b5e92a13b38dd:javax.servlet:javax.servlet-api:3.0.1',
        '8613ae82954779d518631e05daa73a6a954817d5:javax.validation:validation-api:1.1.0.Final',
        '104e9c2b5583cfcfeac0402316221648d6d8ea6b:javax.ws.rs:javax.ws.rs-api:2.0.1',
        '2973d150c0dc1fefe998f834810d68f278ea58ec:junit:junit:4.12',
        '6af37c3f8ec6f9e9653ec837eb508da28ce443cd:org.glassfish.hk2.external:aopalliance-repackaged:2.5.0-b32',
        'b2fa50c8186a38728c35fe6a9da57ce4cc806923:org.glassfish.hk2.external:javax.inject:2.5.0-b32',
        '6a576c9653832ce610b80a2f389374ef19d96171:org.glassfish.hk2:hk2-api:2.5.0-b32',
        '195474f8ad0a8d130e9ea949a771bcf1215fc33b:org.glassfish.hk2:hk2-locator:2.5.0-b32',
        '5108a926988c4ceda7f1e681dddfe3101454a002:org.glassfish.hk2:hk2-utils:2.5.0-b32',
        '4ed2b2d4738aed5786cfa64cba5a332779c4c708:org.glassfish.hk2:osgi-resource-locator:1.0.1',
        'a2bb4f8208e134cf2cf71dfb8824e42942f7bd06:org.glassfish.jersey.bundles.repackaged:jersey-guava:2.25.1',
        '400e30bb035a0cdf3c554530224141ce659a0d1e:org.glassfish.jersey.containers:jersey-container-servlet-core:2.25.1',
        '4d563b1f93352ee9fad597e9e1daf2c6159993c6:org.glassfish.jersey.core:jersey-client:2.25.1',
        '2438ce68d4907046095ab54aa83a6092951b4bbb:org.glassfish.jersey.core:jersey-common:2.25.1',
        '276e2ee0fd1cdabf99357fce560c5baab675b1a2:org.glassfish.jersey.core:jersey-server:2.25.1',
        '0d7da0beeed5614a3bfd882662faec602699e24b:org.glassfish.jersey.media:jersey-media-jaxb:2.25.1',
        '9368dc18933a8b9f2526c86ab310b02781969aa3:org.glassfish.jersey.test-framework:jersey-test-framework-core:2.25.1',
        '0f1c8853ade0ecf707f5a261c830e98893983813:org.hamcrest:java-hamcrest:2.0.0.0',
        'a9cbcdfb7e9f86fbc74d3afae65f2248bfbf82a0:org.javassist:javassist:3.20.0-GA',
    ],
)
load('@jersey_test_framework//:rules.bzl', 'jersey_test_framework_compile')
jersey_test_framework_compile()
#
maven_repository(
    name = 'jersey_test_framework_grizzly2',
    deps = [
        'org.glassfish.jersey.test-framework.providers:jersey-test-framework-provider-grizzly2:2.25.1',
        'org.hamcrest:java-hamcrest:2.0.0.0',
    ],
    exclude = {
        'org.glassfish.jersey.test-framework.providers:jersey-test-framework-provider-grizzly2': [
            'org.hamcrest:hamcrest-core',
        ],
    },
    transitive_deps = [
        '479c1e06db31c432330183f5cae684163f186146:javax.annotation:javax.annotation-api:1.2',
        '6975da39a7040257bd51d21a231b76c915872d38:javax.inject:javax.inject:1',
        '6bf0ebb7efd993e222fc1112377b5e92a13b38dd:javax.servlet:javax.servlet-api:3.0.1',
        '8613ae82954779d518631e05daa73a6a954817d5:javax.validation:validation-api:1.1.0.Final',
        '104e9c2b5583cfcfeac0402316221648d6d8ea6b:javax.ws.rs:javax.ws.rs-api:2.0.1',
        '2973d150c0dc1fefe998f834810d68f278ea58ec:junit:junit:4.12',
        '23a90f6316b3776699b173ccf9394c69d15b7e9c:org.glassfish.grizzly:grizzly-framework:2.3.28',
        'bb34b4e7fbb66b53ac6d428dcc99f5925c9ff7bd:org.glassfish.grizzly:grizzly-http:2.3.28',
        '13bc9a63dae3a0a623b52fe71753d5413d134540:org.glassfish.grizzly:grizzly-http-server:2.3.28',
        '61d7eeadb8b49616d207771b866756c0d70a415b:org.glassfish.grizzly:grizzly-http-servlet:2.3.28',
        '6af37c3f8ec6f9e9653ec837eb508da28ce443cd:org.glassfish.hk2.external:aopalliance-repackaged:2.5.0-b32',
        'b2fa50c8186a38728c35fe6a9da57ce4cc806923:org.glassfish.hk2.external:javax.inject:2.5.0-b32',
        '6a576c9653832ce610b80a2f389374ef19d96171:org.glassfish.hk2:hk2-api:2.5.0-b32',
        '195474f8ad0a8d130e9ea949a771bcf1215fc33b:org.glassfish.hk2:hk2-locator:2.5.0-b32',
        '5108a926988c4ceda7f1e681dddfe3101454a002:org.glassfish.hk2:hk2-utils:2.5.0-b32',
        '4ed2b2d4738aed5786cfa64cba5a332779c4c708:org.glassfish.hk2:osgi-resource-locator:1.0.1',
        'a2bb4f8208e134cf2cf71dfb8824e42942f7bd06:org.glassfish.jersey.bundles.repackaged:jersey-guava:2.25.1',
        '6cfd264755979bafbc65dba660bbc898b25998d0:org.glassfish.jersey.containers:jersey-container-grizzly2-http:2.25.1',
        'e5aa51a3fea1c0fd77fe921e9be52788ccc46e7a:org.glassfish.jersey.containers:jersey-container-grizzly2-servlet:2.25.1',
        'cf5f7a76fcea38158b890ab7a0142d4db709a882:org.glassfish.jersey.containers:jersey-container-servlet:2.25.1',
        '400e30bb035a0cdf3c554530224141ce659a0d1e:org.glassfish.jersey.containers:jersey-container-servlet-core:2.25.1',
        '4d563b1f93352ee9fad597e9e1daf2c6159993c6:org.glassfish.jersey.core:jersey-client:2.25.1',
        '2438ce68d4907046095ab54aa83a6092951b4bbb:org.glassfish.jersey.core:jersey-common:2.25.1',
        '276e2ee0fd1cdabf99357fce560c5baab675b1a2:org.glassfish.jersey.core:jersey-server:2.25.1',
        '0d7da0beeed5614a3bfd882662faec602699e24b:org.glassfish.jersey.media:jersey-media-jaxb:2.25.1',
        '9368dc18933a8b9f2526c86ab310b02781969aa3:org.glassfish.jersey.test-framework:jersey-test-framework-core:2.25.1',
        '9d934d36578f61322d35ee7879d72e5179f42c6b:org.glassfish.jersey.test-framework.providers:jersey-test-framework-provider-grizzly2:2.25.1',
        '0f1c8853ade0ecf707f5a261c830e98893983813:org.hamcrest:java-hamcrest:2.0.0.0',
        'a9cbcdfb7e9f86fbc74d3afae65f2248bfbf82a0:org.javassist:javassist:3.20.0-GA',
    ],
)
load('@jersey_test_framework_grizzly2//:rules.bzl', 'jersey_test_framework_grizzly2_compile')
jersey_test_framework_grizzly2_compile()

# Jettison
maven_repository(
    name = 'jettison',
    deps = [
        'org.codehaus.jettison:jettison:1.3.8',
    ],
    transitive_deps = [
        '6ad07db4f344d965455c237907f65420ced10ae1:org.codehaus.jettison:jettison:1.3.8',
        '49c100caf72d658aca8e58bd74a4ba90fa2b0d70:stax:stax-api:1.0.1',
    ],
)
load('@jettison//:rules.bzl', 'jettison_compile')
jettison_compile()

# jgrapht
maven_repository(
    name = 'jgrapht',
    deps = [
        'org.jgrapht:jgrapht-core:1.1.0',
    ],
    transitive_deps = [
        'b0b4357e6eea77945ca1c1920f865e0cfbbd67f2:org.jgrapht:jgrapht-core:1.1.0',
    ],
)
load('@jgrapht//:rules.bzl', 'jgrapht_compile')
jgrapht_compile()

# JLine3
maven_repository(
    name = 'jline3',
    deps = [
        'org.jline:jline:3.5.2',
    ],
    transitive_deps = [
        '7d079f3a216103454bad29072b08fbcd1a47ad81:org.jline:jline:3.5.2',
    ],
)
load('@jline3//:rules.bzl', 'jline3_compile')
jline3_compile()

# json_smart
maven_repository(
    name = 'json_smart',
    deps = [
        'net.minidev:json-smart:2.3',
    ],
    transitive_deps = [
        'c592b500269bfde36096641b01238a8350f8aa31:net.minidev:accessors-smart:1.2',
        '007396407491352ce4fa30de92efb158adb76b5b:net.minidev:json-smart:2.3',
        '0da08b8cce7bbf903602a25a3a163ae252435795:org.ow2.asm:asm:5.0.4',
    ],
)
load('@json_smart//:rules.bzl', 'json_smart_compile')
json_smart_compile()

# jsonassert
maven_repository(
    name = 'jsonassert',
    deps = [
        'org.skyscreamer:jsonassert:1.2.3',
    ],
    transitive_deps = [
        'c183aa3a2a6250293808bba12262c8920ce5a51c:org.json:json:20090211',
        '2cb01fdff70caf688561d877cea55451ab164527:org.skyscreamer:jsonassert:1.2.3',
    ],
)
load('@jsonassert//:rules.bzl', 'jsonassert_compile')
jsonassert_compile()

# jsonassert
maven_repository(
    name = 'jsonpath',
    deps = [
        'com.jayway.jsonpath:json-path:2.4.0',
    ],
    transitive_deps = [
        '765a4401ceb2dc8d40553c2075eb80a8fa35c2ae:com.jayway.jsonpath:json-path:2.4.0',
        'c592b500269bfde36096641b01238a8350f8aa31:net.minidev:accessors-smart:1.2',
        '007396407491352ce4fa30de92efb158adb76b5b:net.minidev:json-smart:2.3',
        '0da08b8cce7bbf903602a25a3a163ae252435795:org.ow2.asm:asm:5.0.4',
        'da76ca59f6a57ee3102f8f9bd9cee742973efa8a:org.slf4j:slf4j-api:1.7.25',
    ],
)
load('@jsonpath//:rules.bzl', 'jsonpath_compile')
jsonpath_compile()

# JSR305
maven_repository(
    name = 'jsr305',
    deps = [
        'com.google.code.findbugs:jsr305:1.3.9',
    ],
    transitive_deps = [
        '40719ea6961c0cb6afaeb6a921eaa1f6afd4cfdf:com.google.code.findbugs:jsr305:1.3.9',
    ],
)
load('@jsr305//:rules.bzl', 'jsr305_compile')
jsr305_compile()

# JUnit
maven_repository(
    name = 'junit',
    deps = [
        'junit:junit:4.12',
        'org.hamcrest:java-hamcrest:2.0.0.0',
    ],
    exclude = {
        'junit:junit': [
            'org.hamcrest:hamcrest-core',
        ],
    },
    transitive_deps = [
        '2973d150c0dc1fefe998f834810d68f278ea58ec:junit:junit:4.12',
        '0f1c8853ade0ecf707f5a261c830e98893983813:org.hamcrest:java-hamcrest:2.0.0.0',
    ],
)
load('@junit//:rules.bzl', 'junit_compile')
junit_compile()

# LZ4
maven_repository(
    name = 'lz4',
    deps = [
        'org.lz4:lz4-java:1.4.1',
    ],
    transitive_deps = [
        'ad89b11ac280a2992d65e078af06f6709f1fe2fc:org.lz4:lz4-java:1.4.1',
    ],
)
load('@lz4//:rules.bzl', 'lz4_compile')
lz4_compile()

# Maven-Artifact
maven_repository(
    name = 'maven_artifact',
    deps = [
        'org.apache.maven:maven-artifact:3.5.0',
    ],
    force = [
        'org.apache.commons:commons-lang3:3.7',
    ],
    transitive_deps = [
        '557edd918fd41f9260963583ebf5a61a43a6b423:org.apache.commons:commons-lang3:3.7',
        '452acdffbb7fcb272db66685dd54983ce2e07f93:org.apache.maven:maven-artifact:3.5.0',
        'b4ac9780b37cb1b736eae9fbcef27609b7c911ef:org.codehaus.plexus:plexus-utils:3.0.24',
    ],
)
load('@maven_artifact//:rules.bzl', 'maven_artifact_compile')
maven_artifact_compile()

# Opentracing
maven_repository(
    name = 'opentracing_api',
    deps = [
        'io.opentracing:opentracing-api:0.30.0',
    ],
    transitive_deps = [
        '7e54c7a39d508a3268509d9c69dfeea665fc0595:io.opentracing:opentracing-api:0.30.0',
    ],
)
load('@opentracing_api//:rules.bzl', 'opentracing_api_compile')
opentracing_api_compile()
#
maven_repository(
    name = 'opentracing_contrib_jaxrs',
    deps = [
        'io.opentracing.contrib:opentracing-jaxrs2:0.0.9',
    ],
    transitive_deps = [
        '875bcb1f0afe82d76a72241a290a9c3a79188743:io.opentracing.contrib:opentracing-concurrent:0.0.1',
        'f15b8572c395699fa3ed305993e3a83c226305f0:io.opentracing.contrib:opentracing-jaxrs2:0.0.9',
        '07a7528739d3d5ce0103639fd0a0fc0a73c200c5:io.opentracing.contrib:opentracing-web-servlet-filter:0.0.9',
        '7e54c7a39d508a3268509d9c69dfeea665fc0595:io.opentracing:opentracing-api:0.30.0',
        '0a8e47f081e14acda02dd7446b43a990d5e48a4d:io.opentracing:opentracing-noop:0.30.0',
        'b5b39128cee4ff50b6aee28f9248875fdccd22c6:io.opentracing:opentracing-util:0.30.0',
    ],
)
load('@opentracing_contrib_jaxrs//:rules.bzl', 'opentracing_contrib_jaxrs_compile')
opentracing_contrib_jaxrs_compile()
#
maven_repository(
    name = 'opentracing_mock',
    deps = [
        'io.opentracing:opentracing-mock:0.30.0',
    ],
    transitive_deps = [
        '7e54c7a39d508a3268509d9c69dfeea665fc0595:io.opentracing:opentracing-api:0.30.0',
        'aecf4f635a237a49a1b351f5dd5b2aaea9650733:io.opentracing:opentracing-mock:0.30.0',
        '0a8e47f081e14acda02dd7446b43a990d5e48a4d:io.opentracing:opentracing-noop:0.30.0',
    ],
)
load('@opentracing_mock//:rules.bzl', 'opentracing_mock_compile')
opentracing_mock_compile()
#
maven_repository(
    name = 'opentracing_util',
    deps = [
        'io.opentracing:opentracing-util:0.30.0',
    ],
    transitive_deps = [
        '7e54c7a39d508a3268509d9c69dfeea665fc0595:io.opentracing:opentracing-api:0.30.0',
        '0a8e47f081e14acda02dd7446b43a990d5e48a4d:io.opentracing:opentracing-noop:0.30.0',
        'b5b39128cee4ff50b6aee28f9248875fdccd22c6:io.opentracing:opentracing-util:0.30.0',
    ],
)
load('@opentracing_util//:rules.bzl', 'opentracing_util_compile')
opentracing_util_compile()

# scala_library
maven_repository(
    name = 'scala_library',
    deps = [
        'org.scala-lang:scala-library:2.12.0',
    ],
    transitive_deps = [
        '270fc1cda47bc255f3cd03152ec8c2ed7d224e2b:org.scala-lang:scala-library:2.12.0',
    ],
)
load('@scala_library//:rules.bzl', 'scala_library_compile')
scala_library_compile()

# slf4j
maven_repository(
    name = 'slf4j_api',
    deps = [
        'org.slf4j:slf4j-api:1.7.25',
    ],
    transitive_deps = [
        'da76ca59f6a57ee3102f8f9bd9cee742973efa8a:org.slf4j:slf4j-api:1.7.25',
    ],
)
load('@slf4j_api//:rules.bzl', 'slf4j_api_compile')
slf4j_api_compile()
#
maven_repository(
    name = 'slf4j_jdk14',
    deps = [
        'org.slf4j:slf4j-jdk14:1.7.25',
    ],
    transitive_deps = [
        'da76ca59f6a57ee3102f8f9bd9cee742973efa8a:org.slf4j:slf4j-api:1.7.25',
        'bccda40ebc8067491b32a88f49615a747d20082d:org.slf4j:slf4j-jdk14:1.7.25',
    ],
)
load('@slf4j_jdk14//:rules.bzl', 'slf4j_jdk14_compile')
slf4j_jdk14_compile()

# SQLite
maven_repository(
    name = 'sqlite_jdbc',
    deps = [
        'org.xerial:sqlite-jdbc:3.21.0',
    ],
    transitive_deps = [
        '347e4d1d3e1dff66d389354af8f0021e62344584:org.xerial:sqlite-jdbc:3.21.0',
    ],
)
load('@sqlite_jdbc//:rules.bzl', 'sqlite_jdbc_compile')
sqlite_jdbc_compile()

# XStream
maven_repository(
    name = 'xstream',
    deps = [
        'com.thoughtworks.xstream:xstream:1.4.7',
    ],
    transitive_deps = [
        'bce3282142b63068260f021fcbe48b72e8d71a1a:com.thoughtworks.xstream:xstream:1.4.7',
        '2b8e230d2ab644e4ecaa94db7cdedbc40c805dfa:xmlpull:xmlpull:1.1.3.1',
        '19d4e90b43059058f6e056f794f0ea4030d60b86:xpp3:xpp3_min:1.1.4c',
    ],
)
load('@xstream//:rules.bzl', 'xstream_compile')
xstream_compile()
