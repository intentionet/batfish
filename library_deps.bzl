# See: https://github.com/bazelbuild/rules_jvm_external#exporting-and-consuming-artifacts-from-external-repositories
BATFISH_MAVEN_ARTIFACTS = [
    "com.fasterxml.jackson.core:jackson-annotations:2.10.5",
    "com.fasterxml.jackson.core:jackson-core:2.10.5",
    "com.fasterxml.jackson.core:jackson-databind:2.10.5.1",
    "com.fasterxml.jackson.datatype:jackson-datatype-guava:2.10.5",
    "com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.10.5",
    "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.10.5",
    "com.fasterxml.jackson.jaxrs:jackson-jaxrs-base:2.10.5",
    "com.fasterxml.jackson.module:jackson-module-jaxb-annotations:2.10.5",
    "com.google.auto.service:auto-service:1.0-rc6",
    "com.google.auto.service:auto-service-annotations:1.0-rc6",
    "com.google.code.findbugs:jsr305:3.0.2",
    "com.google.errorprone:error_prone_annotations:2.3.1",
    "com.google.guava:guava:28.1-jre",
    "com.google.guava:guava-testlib:28.1-jre",
    "com.google.re2j:re2j:1.4",
    "com.ibm.icu:icu4j:63.1",
    "com.squareup.okhttp3:okhttp:3.14.8",
    "commons-beanutils:commons-beanutils:1.9.4",
    "commons-cli:commons-cli:1.4",
    "commons-io:commons-io:2.6",
    "dk.brics:automaton:1.12-1",
    "io.jaegertracing:jaeger-core:1.2.0",
    "io.jaegertracing:jaeger-thrift:1.2.0",
    "io.opentracing:opentracing-api:0.33.0",
    "io.opentracing:opentracing-mock:0.33.0",
    "io.opentracing:opentracing-noop:0.33.0",
    "io.opentracing:opentracing-util:0.33.0",
    "io.opentracing.contrib:opentracing-jaxrs2:1.0.0",
    "javax.activation:activation:1.1",
    "javax.annotation:javax.annotation-api:1.3.2",
    "javax.ws.rs:javax.ws.rs-api:2.1.1",
    "javax.xml.bind:jaxb-api:2.3.0",
    "junit:junit:4.12",
    "net.sourceforge.pmd:pmd-java:6.23.0",
    "org.antlr:antlr4-runtime:4.7.2",
    "org.apache.commons:commons-collections4:4.4",
    "org.apache.commons:commons-configuration2:2.7",
    "org.apache.commons:commons-lang3:3.9",
    "org.apache.commons:commons-text:1.8",
    "org.apache.httpcomponents:httpclient:4.3.6",
    "org.apache.logging.log4j:log4j-api:2.13.3",
    "org.apache.logging.log4j:log4j-core:2.13.3",
    "org.apache.logging.log4j:log4j-slf4j-impl:2.13.3",
    "org.apache.thrift:libthrift:0.14.0",
    "org.codehaus.jettison:jettison:1.4.0",
    "io.github.java-diff-utils:java-diff-utils:4.0",
    "org.glassfish.grizzly:grizzly-http-server:2.4.3",
    "org.glassfish.grizzly:grizzly-framework:2.4.3",
    "org.glassfish.jersey.containers:jersey-container-grizzly2-http:2.27",
    "org.glassfish.jersey.core:jersey-client:2.27",
    "org.glassfish.jersey.core:jersey-common:2.27",
    "org.glassfish.jersey.core:jersey-server:2.27",
    "org.glassfish.jersey.inject:jersey-hk2:2.27",
    "org.glassfish.jersey.media:jersey-media-json-jackson:2.27",
    "org.glassfish.jersey.media:jersey-media-json-jettison:2.27",
    "org.glassfish.jersey.media:jersey-media-multipart:2.27",
    "org.glassfish.jersey.test-framework:jersey-test-framework-core:2.27",
    "org.glassfish.jersey.test-framework.providers:jersey-test-framework-provider-grizzly2:2.27",
    "org.hamcrest:hamcrest:2.2",
    "org.lz4:lz4-java:1.7.1",
    "org.mockito:mockito-core:3.3.3",
    "org.mockito:mockito-inline:3.3.3",
    "org.jgrapht:jgrapht-core:1.3.1",
    "org.jline:jline:3.13.1",
    "org.parboiled:parboiled-core:1.3.1",
    "org.parboiled:parboiled-java:1.3.1",
    "org.skyscreamer:jsonassert:1.5.0",
    "org.xerial:sqlite-jdbc:3.25.2",
]
