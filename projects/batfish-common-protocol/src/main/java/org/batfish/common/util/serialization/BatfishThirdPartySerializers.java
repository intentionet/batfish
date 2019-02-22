package org.batfish.common.util.serialization;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.google.common.collect.RangeSet;
import javax.annotation.Nullable;
import org.batfish.common.util.serialization.guava.RangeSetSerializer;

/** {@link Serializers} for use by {@link BatfishThirdPartySerializationModule} */
public class BatfishThirdPartySerializers extends Serializers.Base {

  @Override
  public @Nullable JsonSerializer<?> findSerializer(
      SerializationConfig config, JavaType type, BeanDescription beanDesc) {
    if (RangeSet.class.isAssignableFrom(type.getRawClass())) {
      return new RangeSetSerializer(type.findSuperType(RangeSet.class));
    }
    return null;
  }
}
