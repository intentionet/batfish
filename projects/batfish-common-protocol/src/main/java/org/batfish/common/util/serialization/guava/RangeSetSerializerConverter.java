package org.batfish.common.util.serialization.guava;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/** Converter from {@link Set} of {@link Range} to {@link RangeSet} */
@ParametersAreNonnullByDefault
@SuppressWarnings("rawtypes")
public final class RangeSetSerializerConverter
    implements Converter<RangeSet<Comparable>, Set<Range<Comparable>>> {

  static @Nonnull JavaType rangeSetDelegateType(TypeFactory typeFactory, JavaType type) {
    return typeFactory.constructParametricType(
        Set.class,
        typeFactory.constructParametricType(
            Range.class, type.getBindings().getTypeParameters().get(0)));
  }

  private final @Nonnull JavaType _type;

  public RangeSetSerializerConverter(JavaType type) {
    _type = type;
  }

  @Override
  public @Nonnull Set<Range<Comparable>> convert(RangeSet<Comparable> value) {
    return value.asRanges();
  }

  @Override
  public @Nonnull JavaType getInputType(TypeFactory typeFactory) {
    return _type;
  }

  @Override
  public @Nonnull JavaType getOutputType(TypeFactory typeFactory) {
    return RangeSetSerializerConverter.rangeSetDelegateType(typeFactory, _type);
  }
}
