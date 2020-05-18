package org.batfish.coordinator;

import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BfConsts;
import org.batfish.common.CoordConsts;
import org.batfish.common.CoordConstsV2;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.questions.TestQuestion;
import org.batfish.version.BatfishVersion;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jettison.JettisonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class PoolMgrServiceTest extends JerseyTest {

  @Rule public TemporaryFolder _networksFolder = new TemporaryFolder();
  @Rule public TemporaryFolder _questionsTemplatesFolder = new TemporaryFolder();

  @Before
  public void setup() {
    BatfishLogger logger = new BatfishLogger("debug", false);
    Main.mainInit(
        new String[] {
          "-containerslocation",
          _networksFolder.getRoot().toString(),
          "-templatedirs",
          _questionsTemplatesFolder.getRoot().toString()
        });
    Main.setLogger(logger);
    Main.initAuthorizer();
  }

  @Override
  public Application configure() {
    forceSet(TestProperties.CONTAINER_PORT, "0");
    return new ResourceConfig(PoolMgrService.class)
        .register(new JettisonFeature())
        .register(MultiPartFeature.class)
        .register(CrossDomainFilter.class);
  }

  @Override
  protected void configureClient(ClientConfig config) {
    config.register(ServiceObjectMapper.class);
  }

  @Test
  public void testGetQuestionTemplatesVerbose() throws Exception {
    String publicQuestionName = "questionname";
    String hiddenQuestionName = "__questionname";

    writeTemplateFile(publicQuestionName);
    writeTemplateFile(hiddenQuestionName);

    try (Response response = getQuestionTemplatesResponse(true)) {
      assertThat(response.getStatus(), equalTo(OK.getStatusCode()));

      Map<String, String> templates = getQuestionTemplates(response);
      assertThat(
          templates.keySet(), equalTo(ImmutableSet.of(publicQuestionName, hiddenQuestionName)));
    }
  }

  @Test
  public void testGetQuestionTemplatesNotVerbose() throws Exception {
    String publicQuestionName = "questionname";
    String hiddenQuestionName = "__questionname";

    writeTemplateFile(publicQuestionName);
    writeTemplateFile(hiddenQuestionName);

    try (Response response = getQuestionTemplatesResponse(false)) {
      assertThat(response.getStatus(), equalTo(OK.getStatusCode()));

      Map<String, String> templates = getQuestionTemplates(response);
      assertThat(templates.keySet(), equalTo(ImmutableSet.of(publicQuestionName)));
    }
  }

  private Map<String, String> getQuestionTemplates(Response response) throws IOException {
    ArrayNode node = response.readEntity(ArrayNode.class);

    return BatfishObjectMapper.mapper()
        .readValue(
            node.get(1).get(CoordConsts.SVC_KEY_QUESTION_LIST).toString(),
            new TypeReference<Map<String, String>>() {});
  }

  private Response getQuestionTemplatesResponse(boolean verbose) {
    return target(CoordConsts.SVC_CFG_POOL_MGR)
        .path(CoordConsts.SVC_RSC_POOL_GET_QUESTION_TEMPLATES)
        .queryParam(CoordConstsV2.QP_VERBOSE, verbose)
        .request()
        .header(CoordConstsV2.HTTP_HEADER_BATFISH_APIKEY, CoordConsts.DEFAULT_API_KEY)
        .header(CoordConsts.SVC_KEY_VERSION, BatfishVersion.getVersionStatic())
        .get();
  }

  private @Nonnull String writeTemplateFile(String templateName) throws IOException {
    Path questionTemplateDir = _questionsTemplatesFolder.getRoot().toPath().resolve("templates");
    String templateText =
        String.format(
            "{\"class\":\"%s\",\"%s\":{\"%s\":\"%s\"}}",
            TestQuestion.class, BfConsts.PROP_INSTANCE, BfConsts.PROP_INSTANCE_NAME, templateName);
    Path questionTemplateFile = questionTemplateDir.resolve(templateName + ".json");
    questionTemplateDir.toFile().mkdirs();
    Files.write(questionTemplateFile, templateText.getBytes(StandardCharsets.UTF_8));
    Main.getSettings().setQuestionTemplateDirs(ImmutableList.of(questionTemplateDir));
    return templateText;
  }
}
