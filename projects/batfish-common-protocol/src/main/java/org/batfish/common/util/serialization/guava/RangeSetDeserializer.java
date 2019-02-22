package org.batfish.common.util.serialization.guava;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDelegatingDeserializer;
import com.fasterxml.jackson.databind.util.Converter;
import com.google.common.collect.RangeSet;
import javax.annotation.Nonnull;

/** Custom deserializer for {@link RangeSet} */
@SuppressWarnings("rawtypes")
public final class RangeSetDeserializer extends StdDelegatingDeserializer<RangeSet<Comparable>>
    implements ContextualDeserializer {

  private static final long serialVersionUID = 1L;

  public RangeSetDeserializer(@Nonnull JavaType type) {
    super(new RangeSetDeserializerConverter(type));
  }

  @Override
  protected @Nonnull StdDelegatingDeserializer<RangeSet<Comparable>> withDelegate(
      Converter<Object, RangeSet<Comparable>> converter,
      JavaType delegateType,
      JsonDeserializer<?> delegateDeserializer) {
    return new StdDelegatingDeserializer<>(converter, delegateType, delegateDeserializer);
  }
}
