package org.batfish.common.util.serialization.guava;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/** Converter from {@link Set} of {@link Range} to {@link RangeSet} */
@ParametersAreNonnullByDefault
@SuppressWarnings("rawtypes")
public final class RangeSetDeserializerConverter
    implements Converter<Set<Range<Comparable>>, RangeSet<Comparable>> {

  private final @Nonnull JavaType _type;

  public RangeSetDeserializerConverter(JavaType type) {
    _type = type;
  }

  @Override
  public @Nonnull RangeSet<Comparable> convert(Set<Range<Comparable>> value) {
    return TreeRangeSet.create(value);
  }

  @Override
  public @Nonnull JavaType getInputType(TypeFactory typeFactory) {
    return RangeSetSerializerConverter.rangeSetDelegateType(typeFactory, _type);
  }

  @Override
  public @Nonnull JavaType getOutputType(TypeFactory typeFactory) {
    return _type;
  }
}
