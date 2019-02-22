package org.batfish.datamodel.transformation;

import static org.batfish.datamodel.flow.TransformationStep.TransformationType.SOURCE_NAT;

import com.google.common.collect.Streams;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Ip;

/** Utility methods related to {@link Transformation}. */
@ParametersAreNonnullByDefault
public final class TransformationUtil {
  private TransformationUtil() {}

  private static final TransformationStepVisitor<Boolean> IS_SOURCE_NAT =
      new TransformationStepVisitor<Boolean>() {
        @Override
        public Boolean visitAssignIpAddressFromPool(
            AssignIpAddressFromPool assignIpAddressFromPool) {
          return assignIpAddressFromPool.getType() == SOURCE_NAT;
        }

        @Override
        public Boolean visitNoop(Noop noop) {
          return false;
        }

        @Override
        public Boolean visitShiftIpAddressIntoSubnet(
            ShiftIpAddressIntoSubnet shiftIpAddressIntoSubnet) {
          return shiftIpAddressIntoSubnet.getType() == SOURCE_NAT;
        }

        @Override
        public Boolean visitAssignPortFromPool(AssignPortFromPool assignPortFromPool) {
          return assignPortFromPool.getType() == SOURCE_NAT;
        }
      };

  private static final TransformationStepVisitor<Stream<Ip>> SOURCE_NAT_POOL_IPS =
      new TransformationStepVisitor<Stream<Ip>>() {
        @Override
        public Stream<Ip> visitAssignIpAddressFromPool(
            AssignIpAddressFromPool assignIpAddressFromPool) {
          return assignIpAddressFromPool.getType() != SOURCE_NAT
              ? Stream.of()
              : assignIpAddressFromPool.getIpRanges().asRanges().stream()
                  .flatMapToLong(
                      ipRange ->
                          LongStream.range(
                              ipRange.lowerEndpoint().asLong(),
                              ipRange.upperEndpoint().asLong() + 1))
                  .mapToObj(Ip::create);
        }

        @Override
        public Stream<Ip> visitNoop(Noop noop) {
          return Stream.of();
        }

        @Override
        public Stream<Ip> visitShiftIpAddressIntoSubnet(
            ShiftIpAddressIntoSubnet shiftIpAddressIntoSubnet) {
          // not a pool
          return Stream.of();
        }

        @Override
        public Stream<Ip> visitAssignPortFromPool(AssignPortFromPool assignPortFromPool) {
          return Stream.of();
        }
      };

  public static boolean hasSourceNat(@Nullable Transformation transformation) {
    if (transformation == null) {
      return false;
    }

    return transformation.getTransformationSteps().stream().anyMatch(IS_SOURCE_NAT::visit)
        || hasSourceNat(transformation.getAndThen())
        || hasSourceNat(transformation.getOrElse());
  }

  public static Stream<Ip> sourceNatPoolIps(@Nullable Transformation transformation) {
    return transformation == null
        ? Stream.of()
        : Streams.concat(
            transformation.getTransformationSteps().stream().flatMap(SOURCE_NAT_POOL_IPS::visit),
            sourceNatPoolIps(transformation.getAndThen()),
            sourceNatPoolIps(transformation.getOrElse()));
  }

  /**
   * Visit all transformation steps in a (nested) {@link Transformation}. First visit the steps of
   * the root transformation, then recursively visit the andThen transformation, then visit the
   * orElse transformation.
   */
  public static void visitTransformationSteps(
      @Nullable Transformation transformation, TransformationStepVisitor<Void> stepVisitor) {
    if (transformation == null) {
      return;
    }

    transformation.getTransformationSteps().forEach(stepVisitor::visit);
    visitTransformationSteps(transformation.getAndThen(), stepVisitor);
    visitTransformationSteps(transformation.getOrElse(), stepVisitor);
  }
}
