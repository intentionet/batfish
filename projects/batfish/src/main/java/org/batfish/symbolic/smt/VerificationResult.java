package org.batfish.symbolic.smt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.z3.BoolExpr;
import java.util.SortedMap;
import java.util.SortedSet;
import javax.annotation.Nullable;

/**
 * The result of trying to verify properties of the network model built with an encoder. Either the
 * model is verified, or there is a counter example and the counter example is provided as a model
 * for each encoding variable.
 *
 * @author Ryan Beckett
 */
public class VerificationResult {

  private static final String VERIFIED_VAR = "verified";

  private static final String MODEL_VAR = "model";

  private static final String PACKET_MODEL_VAR = "packetModel";

  private static final String ENV_MODEL_VAR = "environmentModel";

  private static final String FWD_MODEL_VAR = "forwardingModel";

  private static final String FAILURE_MODEL_VAR = "failuresModel";

  private boolean _verified;

  private SortedMap<String, String> _model;

  private SortedMap<String, String> _packetModel;

  private SortedMap<String, SortedMap<String, String>> _envModel;

  private SortedSet<String> _fwdModel;

  private SortedSet<String> _failures;

  @JsonCreator
  public VerificationResult(
      @JsonProperty(VERIFIED_VAR) boolean verified,
      @Nullable @JsonProperty(MODEL_VAR) SortedMap<String, String> model,
      @Nullable @JsonProperty(PACKET_MODEL_VAR) SortedMap<String, String> packetModel,
      @Nullable @JsonProperty(ENV_MODEL_VAR) SortedMap<String, SortedMap<String, String>> envModel,
      @Nullable @JsonProperty(FWD_MODEL_VAR) SortedSet<String> fwdModel,
      @Nullable @JsonProperty(FAILURE_MODEL_VAR) SortedSet<String> failures) {
    _verified = verified;
    _model = model;
    _packetModel = packetModel;
    _envModel = envModel;
    _fwdModel = fwdModel;
    _failures = failures;
  }

  @JsonProperty(VERIFIED_VAR)
  public boolean isVerified() {
    return _verified;
  }

  @JsonProperty(MODEL_VAR)
  public SortedMap<String, String> getModel() {
    return _model;
  }

  @JsonProperty(PACKET_MODEL_VAR)
  public SortedMap<String, String> getPacketModel() {
    return _packetModel;
  }

  @JsonProperty(ENV_MODEL_VAR)
  public SortedMap<String, SortedMap<String, String>> getEnvModel() {
    return _envModel;
  }

  @JsonProperty(FWD_MODEL_VAR)
  public SortedSet<String> getFwdModel() {
    return _fwdModel;
  }

  @JsonProperty(FAILURE_MODEL_VAR)
  public SortedSet<String> getFailures() {
    return _failures;
  }

  private String prettyPrintEnv() {
    StringBuilder sb = new StringBuilder();
    if (_envModel != null) {
      sb.append("\n");
      sb.append("Environment Messages:\n");
      sb.append("----------------------");
      _envModel.forEach(
          (edge, map) -> {
            sb.append("\n").append(edge).append(":\n");
            map.forEach(
                (key, val) -> sb.append("  ").append(key).append(": ").append(val).append("\n"));
          });
    }
    return sb.toString();
  }

  private String prettyPrintFailures() {
    StringBuilder sb = new StringBuilder();
    if (_failures != null) {
      sb.append("\n");
      sb.append("Link Failures:\n");
      sb.append("----------------------\n");
      for (String s : _failures) {
        sb.append(s).append("\n");
      }
    }
    return sb.toString();
  }

  private String prettyPrintForwarding() {
    StringBuilder sb = new StringBuilder();
    if (_fwdModel != null) {
      sb.append("\n");
      sb.append("Final Forwarding:\n");
      sb.append("----------------------\n");
      for (String s : _fwdModel) {
        sb.append(s).append("\n");
      }
    }
    return sb.toString();
  }

  private String prettyPrintPacket() {
    StringBuilder sb = new StringBuilder();
    sb.append("Packet:\n");
    sb.append("----------------------\n");
    _packetModel.forEach((key, val) -> sb.append(key).append(": ").append(val).append("\n"));
    return sb.toString();
  }

  public String prettyPrint(@Nullable String iface) {
    StringBuilder sb = new StringBuilder();
    if (_verified) {
      sb.append("\nVerified");
    } else {
      if (iface != null) {
        sb.append("\nCounterexample Found (").append(iface).append(")").append(":\n");
      } else {
        sb.append("\nCounterexample Found:\n");
      }
      sb.append("==========================================\n");
      sb.append(prettyPrintPacket());
      sb.append(prettyPrintEnv());
      sb.append(prettyPrintForwarding());
      sb.append(prettyPrintFailures());
      sb.append("==========================================\n");
    }
    return sb.toString();
  }

  public void debug(EncoderSlice enc, boolean showConstraints, String filter) {
    if (showConstraints) {
      System.out.println("================= Constraints ==================");
      for (BoolExpr be : enc.getSolver().getAssertions()) {
        String x = be.simplify().toString();
        if (filter == null || x.contains(filter)) {
          System.out.println(x);
        }
      }
    }
    if (_verified) {
      System.out.println("verified");
    } else {
      System.out.println("================= Model ================");
      enc.getSymbolicDecisions()
          .getDataForwarding()
          .forEach(
              (router, map) ->
                  map.forEach(
                      (edge, e) -> {
                        String expr = e.toString();
                        if (expr.contains("DATA-")) {
                          String result = _model.get(expr);
                          if ("true".equals(result)) {
                            System.out.println(edge);
                          }
                        }
                      }));
      System.out.println("");
      _model.forEach(
          (var, val) -> {
            if (filter == null || var.contains(filter)) {
              System.out.println(var + "=" + val);
            }
          });
    }

    if (enc.getUnsatCore().getDoTrack()) {
      System.out.println("================= Unsat Core ================");
      for (BoolExpr be : enc.getSolver().getUnsatCore()) {
        BoolExpr constraint = enc.getUnsatCore().getTrackingVars().get(be.toString());
        System.out.println("Var: " + be);
        System.out.println(constraint);
        System.out.println("");
      }
    }
  }
}
