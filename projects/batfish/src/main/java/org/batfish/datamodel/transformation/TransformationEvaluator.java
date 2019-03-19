package org.batfish.datamodel.transformation;

import static com.google.common.base.Verify.verifyNotNull;
import static org.batfish.datamodel.FlowDiff.flowDiff;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowDiff;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.acl.Evaluator;
import org.batfish.datamodel.flow.Step;
import org.batfish.datamodel.flow.StepAction;
import org.batfish.datamodel.flow.TransformationStep.TransformationStepDetail;
import org.batfish.datamodel.flow.TransformationStep.TransformationType;

/** Evaluates a {@link Transformation} on an input {@link Flow}. */
public class TransformationEvaluator {
  private final Flow.Builder _flowBuilder;
  private final String _srcInterface;
  private final Map<String, IpAccessList> _namedAcls;
  private final Map<String, IpSpace> _namedIpSpaces;
  private final ImmutableList.Builder<Step<?>> _traceSteps;

  // the ACL evaluator has to be re-initialized every time we transform the flow
  private Evaluator _aclEvaluator;
  private Flow _currentFlow;

  /**
   * StepEvaluator returns true iff the step transforms the packet. It's possible that the step
   * matches but doesn't transform the packet (e.g. Noop). This means transformation trace steps are
   * generated even when the packet isn't transformed.
   */
  @VisibleForTesting
  class StepEvaluator implements TransformationStepVisitor<Boolean> {
    private final Map<TransformationType, ImmutableSortedSet.Builder<FlowDiff>> _flowDiffs =
        new EnumMap<>(TransformationType.class);

    private void set(IpField field, Ip ip) {
      switch (field) {
        case DESTINATION:
          _flowBuilder.setDstIp(ip);
          break;
        case SOURCE:
          _flowBuilder.setSrcIp(ip);
          break;
        default:
          throw new IllegalArgumentException("unknown IpField " + field);
      }
    }

    private void set(PortField field, int port) {
      switch (field) {
        case DESTINATION:
          _flowBuilder.setDstPort(port);
          break;
        case SOURCE:
          _flowBuilder.setSrcPort(port);
          break;
        default:
          throw new IllegalArgumentException("unknown PortField " + field);
      }
    }

    private Ip get(IpField field) {
      switch (field) {
        case DESTINATION:
          return _flowBuilder.getDstIp();
        case SOURCE:
          return _flowBuilder.getSrcIp();
        default:
          throw new IllegalArgumentException("unknown IpField " + field);
      }
    }

    private int get(PortField field) {
      switch (field) {
        case DESTINATION:
          return verifyNotNull(_flowBuilder.getDstPort(), "Missing destination port");
        case SOURCE:
          return verifyNotNull(_flowBuilder.getSrcPort(), "Missing source port");
        default:
          throw new IllegalArgumentException("unknown PortField " + field);
      }
    }

    private ImmutableSortedSet.Builder<FlowDiff> getFlowDiffs(TransformationType type) {
      return _flowDiffs.computeIfAbsent(type, k -> ImmutableSortedSet.naturalOrder());
    }

    // when no values change, simply apply noop
    private void noop(TransformationType type) {
      _flowDiffs.computeIfAbsent(type, k -> ImmutableSortedSet.naturalOrder());
    }

    private List<Step<?>> getTraceSteps() {
      return _flowDiffs.entrySet().stream()
          .map(
              entry -> {
                ImmutableSortedSet<FlowDiff> flowDiffs = entry.getValue().build();
                TransformationStepDetail detail =
                    new TransformationStepDetail(entry.getKey(), flowDiffs);
                StepAction action =
                    flowDiffs.isEmpty() ? StepAction.PERMITTED : StepAction.TRANSFORMED;
                return new org.batfish.datamodel.flow.TransformationStep(detail, action);
              })
          .collect(ImmutableList.toImmutableList());
    }

    private boolean set(TransformationType type, IpField ipField, Ip oldValue, Ip newValue) {
      if (oldValue.equals(newValue)) {
        noop(type);
        return false;
      } else {
        set(ipField, newValue);
        getFlowDiffs(type).add(flowDiff(ipField, oldValue, newValue));
        return true;
      }
    }

    private boolean set(TransformationType type, PortField portField, int oldValue, int newValue) {
      if (oldValue == newValue) {
        noop(type);
        return false;
      } else {
        set(portField, newValue);
        getFlowDiffs(type).add(flowDiff(portField, oldValue, newValue));
        return true;
      }
    }

    @Override
    public Boolean visitAssignIpAddressFromPool(AssignIpAddressFromPool step) {
      Ip ip = step.getIpRanges().asRanges().iterator().next().lowerEndpoint();
      return set(step.getType(), step.getIpField(), get(step.getIpField()), ip);
    }

    @Override
    public Boolean visitNoop(Noop noop) {
      /* getFlowDiffs makes sure the type is in the key set, which signals that we went through
       * the transformation
       */
      this.noop(noop.getType());
      return false;
    }

    @Override
    public Boolean visitShiftIpAddressIntoSubnet(ShiftIpAddressIntoSubnet step) {
      IpField field = step.getIpField();
      Ip oldValue = get(field);
      Prefix targetSubnet = step.getSubnet();
      Prefix currentSubnetPrefix = Prefix.create(oldValue, targetSubnet.getPrefixLength());
      long offset = oldValue.asLong() - currentSubnetPrefix.getStartIp().asLong();
      Ip newValue = Ip.create(targetSubnet.getStartIp().asLong() + offset);
      return set(step.getType(), field, oldValue, newValue);
    }

    @Override
    public Boolean visitAssignPortFromPool(AssignPortFromPool step) {
      return set(
          step.getType(), step.getPortField(), get(step.getPortField()), step.getPoolStart());
    }

    @Override
    public Boolean visitApplyAll(ApplyAll applyAll) {
      // NOTE that reduce is used instead of anyMatch to ensure all steps accept the visitor.
      return applyAll.getSteps().stream()
          .map(step -> step.accept(this))
          .reduce(false, (a, b) -> a || b);
    }

    @Override
    public Boolean visitApplyAny(ApplyAny applyAny) {
      // NOTE that short-circuiting with anyMatch is intended so no more than one non-identity
      // transformation is performed.
      return applyAny.getSteps().stream().anyMatch(step -> step.accept(this));
    }
  }

  private TransformationEvaluator(
      Flow flow,
      String srcInterface,
      Map<String, IpAccessList> namedAcls,
      Map<String, IpSpace> namedIpSpaces) {
    _currentFlow = flow;
    _flowBuilder = flow.toBuilder();
    _srcInterface = srcInterface;
    _namedAcls = ImmutableMap.copyOf(namedAcls);
    _namedIpSpaces = ImmutableMap.copyOf(namedIpSpaces);
    _traceSteps = ImmutableList.builder();
    initializeAclEvaluator();
  }

  /** The result of evaluating a {@link Transformation}. */
  public static final class TransformationResult {
    private final Flow _outputFlow;
    private final List<Step<?>> _traceSteps;

    TransformationResult(Flow outputFlow, List<Step<?>> traceSteps) {
      _outputFlow = outputFlow;
      _traceSteps = ImmutableList.copyOf(traceSteps);
    }

    public Flow getOutputFlow() {
      return _outputFlow;
    }

    public List<Step<?>> getTraceSteps() {
      return _traceSteps;
    }
  }

  public static TransformationResult eval(
      Transformation transformation,
      Flow flow,
      String srcInterface,
      Map<String, IpAccessList> namedAcls,
      Map<String, IpSpace> namedIpSpaces) {
    TransformationEvaluator evaluator =
        new TransformationEvaluator(flow, srcInterface, namedAcls, namedIpSpaces);
    evaluator.eval(transformation);
    return new TransformationResult(evaluator._currentFlow, evaluator._traceSteps.build());
  }

  private void initializeAclEvaluator() {
    _aclEvaluator = new Evaluator(_currentFlow, _srcInterface, _namedAcls, _namedIpSpaces);
  }

  private void eval(Transformation transformation) {
    Transformation node = transformation;
    while (node != null) {
      if (_aclEvaluator.visit(node.getGuard())) {
        StepEvaluator stepEvaluator = new StepEvaluator();
        boolean transformed =
            node.getTransformationSteps().stream()
                .map(stepEvaluator::visit)
                .reduce(Boolean::logicalOr)
                .orElse(false);
        // noop transformation steps can generate tracesteps without transforming the flow
        _traceSteps.addAll(stepEvaluator.getTraceSteps());

        if (transformed) {
          _currentFlow = _flowBuilder.build();
          initializeAclEvaluator();
        }

        node = node.getAndThen();
      } else {
        node = node.getOrElse();
      }
    }
  }
}
