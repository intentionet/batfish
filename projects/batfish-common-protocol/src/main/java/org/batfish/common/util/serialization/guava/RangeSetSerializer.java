package org.batfish.common.util.serialization.guava;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer;
import com.fasterxml.jackson.databind.util.Converter;
import com.google.common.collect.RangeSet;
import javax.annotation.Nonnull;

/** Custom serializer for {@link RangeSet} */
public final class RangeSetSerializer extends StdDelegatingSerializer {

  private static final long serialVersionUID = 1L;

  public RangeSetSerializer(@Nonnull JavaType type) {
    super(new RangeSetSerializerConverter(type));
  }

  @Override
  protected @Nonnull StdDelegatingSerializer withDelegate(
      Converter<Object, ?> converter, JavaType delegateType, JsonSerializer<?> delegateSerializer) {
    return this;
  }
}
