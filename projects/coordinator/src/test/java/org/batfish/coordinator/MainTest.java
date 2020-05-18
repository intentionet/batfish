package org.batfish.coordinator;

import static org.batfish.coordinator.Main.getQuestionTemplates;
import static org.batfish.coordinator.Main.readQuestionTemplate;
import static org.batfish.coordinator.Main.readQuestionTemplates;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.batfish.common.BatfishLogger;
import org.codehaus.jettison.json.JSONObject;
import org.hamcrest.collection.IsMapContaining;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests of {@link Main}. */
@RunWith(JUnit4.class)
public class MainTest {

  private static final String DUPLICATE_TEMPLATE_NAME = "duplicate_template";
  private static final String NEW_TEMPLATE_NAME = "new_template";

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Test
  public void testReadQuestionTemplateDuplicate() throws Exception {
    Map<String, String> questionTemplates = Maps.newHashMap();
    // already existing question template with key duplicate_template
    questionTemplates.put(DUPLICATE_TEMPLATE_NAME, "template_body");

    JSONObject testQuestion = new JSONObject();
    testQuestion.put(
        "instance",
        new JSONObject()
            .put("instanceName", DUPLICATE_TEMPLATE_NAME)
            .put("description", "test question description"));
    Path questionJsonPath = _folder.newFile("testquestion.json").toPath();
    Files.write(questionJsonPath, testQuestion.toString().getBytes(StandardCharsets.UTF_8));

    readQuestionTemplate(questionJsonPath, questionTemplates);

    assertThat(questionTemplates.keySet(), hasSize(1));
    // the value of the question template with key duplicate_template should be replaced now
    assertThat(
        questionTemplates,
        IsMapContaining.hasEntry(
            DUPLICATE_TEMPLATE_NAME,
            "{\"instance\":{\"instanceName\":\"duplicate_template\",\"description\":\"test question description\"}}"));
  }

  @Test
  public void testReadQuestionTemplatesCaps() throws Exception {
    Map<String, String> questionTemplates = Maps.newHashMap();
    questionTemplates.put(DUPLICATE_TEMPLATE_NAME, "template_body");

    JSONObject testQuestion = new JSONObject();
    testQuestion.put(
        "instance",
        new JSONObject()
            .put("instanceName", DUPLICATE_TEMPLATE_NAME.toUpperCase())
            .put("description", "test question description"));
    Path questionJsonPath = _folder.newFile("testquestion.json").toPath();
    Files.write(questionJsonPath, testQuestion.toString().getBytes(StandardCharsets.UTF_8));

    readQuestionTemplate(questionJsonPath, questionTemplates);

    assertThat(questionTemplates.keySet(), hasSize(1));
    // the value will be replaced with the body of the last template read
    // with (instanceName:DUPLICATE_TEMPLATE), key of the map will be same
    assertThat(
        questionTemplates,
        IsMapContaining.hasEntry(
            DUPLICATE_TEMPLATE_NAME,
            "{\"instance\":{\"instanceName\":\"DUPLICATE_TEMPLATE\",\"description\":\"test question description\"}}"));
  }

  @Test
  public void testReadQuestionTemplates() throws Exception {
    Map<String, String> questionTemplates = Maps.newHashMap();
    questionTemplates.put(DUPLICATE_TEMPLATE_NAME, "template_body");

    JSONObject testQuestion = new JSONObject();
    testQuestion.put(
        "instance",
        new JSONObject()
            .put("instanceName", NEW_TEMPLATE_NAME)
            .put("description", "test question description"));
    Path questionJsonPath = _folder.newFile("testquestion.json").toPath();
    Files.write(questionJsonPath, testQuestion.toString().getBytes(StandardCharsets.UTF_8));

    readQuestionTemplate(questionJsonPath, questionTemplates);

    assertThat(questionTemplates.keySet(), hasSize(2));
    // Both templates should be present
    assertThat(
        questionTemplates,
        allOf(
            IsMapContaining.hasEntry(DUPLICATE_TEMPLATE_NAME, "template_body"),
            IsMapContaining.hasEntry(
                NEW_TEMPLATE_NAME,
                "{\"instance\":{\"instanceName\":\"new_template\",\"description\":\"test question description\"}}")));
  }

  @Test
  public void testReadQuestionTemplatesRecursive() throws Exception {
    JSONObject testQuestion1 = new JSONObject();
    testQuestion1.put(
        "instance",
        new JSONObject()
            .put("instanceName", "testQuestion1")
            .put("description", "test question one description"));
    Path question1JsonPath = _folder.newFile("testquestion1.json").toPath();
    Files.write(question1JsonPath, testQuestion1.toString().getBytes(StandardCharsets.UTF_8));

    JSONObject testQuestion2 = new JSONObject();
    testQuestion2.put(
        "instance",
        new JSONObject()
            .put("instanceName", "testQuestion2")
            .put("description", "test question two description"));
    File nestedFolder = _folder.newFolder("nestedFolder");
    Path question2JsonPath = nestedFolder.toPath().resolve("testquestions2.json");
    Files.write(question2JsonPath, testQuestion2.toString().getBytes(StandardCharsets.UTF_8));

    Map<String, String> questionTemplates = Maps.newHashMap();

    readQuestionTemplates(_folder.getRoot().toPath(), questionTemplates);
    assertThat(questionTemplates.keySet(), hasSize(2));
    // Both templates should be present
    assertThat(
        questionTemplates,
        allOf(
            IsMapContaining.hasEntry(
                "testquestion1",
                "{\"instance\":{\"instanceName\":\"testQuestion1\",\"description\":\"test question one description\"}}"),
            IsMapContaining.hasEntry(
                "testquestion2",
                "{\"instance\":{\"instanceName\":\"testQuestion2\",\"description\":\"test question two description\"}}")));
  }

  @Test
  public void testEmptyQuestionTemplateDir() throws Exception {
    JSONObject testQuestion = new JSONObject();
    testQuestion.put(
        "instance",
        new JSONObject()
            .put("instanceName", "testQuestion")
            .put("description", "test question description"));
    Path questionJsonPath = _folder.newFile("testquestion.json").toPath();
    Files.write(questionJsonPath, testQuestion.toString().getBytes(StandardCharsets.UTF_8));

    Main.mainInit(new String[0]);
    Main.setLogger(new BatfishLogger("debug", false));
    Main.getSettings()
        .setQuestionTemplateDirs(ImmutableList.of(_folder.getRoot().toPath(), Paths.get("")));

    Map<String, String> questionTemplates = getQuestionTemplates();

    assertThat(questionTemplates, notNullValue());
    assertThat(questionTemplates.keySet(), hasSize(1));
    // Both templates should be present
    assertThat(
        questionTemplates,
        IsMapContaining.hasEntry(
            "testquestion",
            "{\"instance\":{\"instanceName\":\"testQuestion\",\"description\":\"test question description\"}}"));
  }
}
