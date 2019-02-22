package org.batfish.common.util.serialization;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.google.common.collect.RangeSet;
import javax.annotation.Nullable;
import org.batfish.common.util.serialization.guava.RangeSetDeserializer;

/** {@link Deserializers} for use by {@link BatfishThirdPartySerializationModule} */
public final class BatfishThirdPartyDeserializers extends Deserializers.Base {

  @Override
  public @Nullable JsonDeserializer<?> findBeanDeserializer(
      JavaType type, DeserializationConfig config, BeanDescription beanDesc)
      throws JsonMappingException {
    if (type.hasRawClass(RangeSet.class)) {
      return new RangeSetDeserializer(type);
    }
    return null;
  }
}
