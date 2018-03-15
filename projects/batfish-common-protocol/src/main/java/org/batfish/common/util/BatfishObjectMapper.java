package org.batfish.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;

public final class BatfishObjectMapper {
  private static final ObjectMapper MAPPER = baseMapper();
  private static final ObjectWriter WRITER = MAPPER.writer();
  private static final PrettyPrinter PRETTY_PRINTER = new PrettyPrinter();

  private static final ObjectWriter PRETTY_WRITER =
      baseMapper().enable(SerializationFeature.INDENT_OUTPUT).writer(PRETTY_PRINTER);
  private static final ObjectWriter VERBOSE_WRITER =
      baseMapper()
          .enable(SerializationFeature.INDENT_OUTPUT)
          .setSerializationInclusion(Include.ALWAYS)
          .writer(PRETTY_PRINTER);

  /**
   * Returns a {@link ObjectMapper} configured to Batfish JSON standards. The JSON produced is not
   * pretty-printed; see {@link #prettyWriter} for that.
   */
  public static ObjectMapper mapper() {
    return MAPPER;
  }

  /** Uses Jackson to clone the given object using the given type. */
  public static <T> T clone(Object o, Class<T> clazz) throws IOException {
    return MAPPER.readValue(WRITER.writeValueAsBytes(o), clazz);
  }

  /**
   * Returns a {@link ObjectWriter} configured to Batfish JSON standards. The JSON produced is not
   * pretty-printed; see {@link #prettyWriter} for that.
   */
  @SuppressWarnings("unused") // Seems like it should be used some day.
  public static ObjectWriter writer() {
    return WRITER;
  }

  /**
   * Returns a {@link ObjectWriter} configured to Batfish JSON standards. The JSON produced is not
   * pretty-printed; for a more concise encoding use {@link #writer}.
   */
  public static ObjectWriter prettyWriter() {
    return PRETTY_WRITER;
  }

  /**
   * Returns a {@link ObjectWriter} configured to Batfish JSON standards. The JSON produced is
   * verbosely pretty-printed; all fields are included.
   *
   * @see BatfishObjectMapper#writer()
   * @see BatfishObjectMapper#prettyWriter()
   */
  public static ObjectWriter verboseWriter() {
    return VERBOSE_WRITER;
  }

  /** Returns a concise JSON string representation of the given object. */
  public static String writeString(Object o) throws JsonProcessingException {
    return WRITER.writeValueAsString(o);
  }

  /** Returns a pretty JSON string representation of the given object. */
  public static String writePrettyString(Object o) throws JsonProcessingException {
    return PRETTY_WRITER.writeValueAsString(o);
  }

  /**
   * A custom Jackson {@link DefaultPrettyPrinter} that also prints newlines between array elements,
   * which is better suited towards complex, highly-nested objects.
   */
  private static class PrettyPrinter extends DefaultPrettyPrinter {
    /** */
    private static final long serialVersionUID = 1L;

    public PrettyPrinter() {
      _arrayIndenter = DefaultIndenter.SYSTEM_LINEFEED_INSTANCE;
    }
  }

  /** Configures all the default options for a Batfish {@link ObjectMapper}. */
  private static ObjectMapper baseMapper() {
    ObjectMapper mapper = new ObjectMapper();

    mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
    mapper.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
    // Next two lines make Instant class serialize as an RFC-3339 timestamp
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    // This line makes Java 8's Optional type serialize
    mapper.registerModule(new Jdk8Module());
    // See https://groups.google.com/forum/#!topic/jackson-user/WfZzlt5C2Ww
    //  This fixes issues in which non-empty maps with keys with empty values would get omitted
    //  entirely. See also https://github.com/batfish/batfish/issues/256
    mapper.setDefaultPropertyInclusion(
        JsonInclude.Value.construct(Include.NON_EMPTY, Include.ALWAYS));
    // This line makes Guava collections work with jackson
    mapper.registerModule(new GuavaModule());

    return mapper;
  }
}
