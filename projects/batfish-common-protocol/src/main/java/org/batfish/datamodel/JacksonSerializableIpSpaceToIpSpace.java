package org.batfish.datamodel;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

public class JacksonSerializableIpSpaceToIpSpace extends JsonDeserializer<IpSpace> {

  @Override
  public IpSpace deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException, JsonProcessingException {
    return p.readValueAs(JacksonSerializableIpSpace.class).unwrap();
  }
}
