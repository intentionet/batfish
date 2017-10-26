package org.batfish.datamodel.routing_policy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AbstractRouteBuilder;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.statement.Statement;

@JsonSchemaDescription(
    "A procedural routing policy used to transform and accept/reject IPV4/IPV6 routes")
public class RoutingPolicy extends ComparableStructure<String> {

  /** */
  private static final long serialVersionUID = 1L;

  private static final String PROP_STATEMENTS = "statements";

  private Configuration _owner;

  private List<Statement> _statements;

  @JsonCreator
  private RoutingPolicy(@JsonProperty(PROP_NAME) String name) {
    super(name);
    _statements = new ArrayList<>();
  }

  public RoutingPolicy(String name, Configuration owner) {
    this(name);
    _owner = owner;
  }

  public Result call(Environment environment) {
    for (Statement statement : _statements) {
      Result result = statement.execute(environment);
      if (result.getExit()) {
        return result;
      }
      if (result.getReturn()) {
        result.setReturn(false);
        return result;
      }
    }
    Result result = new Result();
    result.setFallThrough(true);
    result.setBooleanValue(environment.getDefaultAction());
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof RoutingPolicy)) {
      return false;
    }
    RoutingPolicy other = (RoutingPolicy) o;
    return _statements.equals(other._statements);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_key, _statements);
  }

  @JsonIgnore
  public Configuration getOwner() {
    return _owner;
  }

  @JsonProperty(PROP_STATEMENTS)
  @JsonPropertyDescription("The list of routing-policy statements to execute")
  public List<Statement> getStatements() {
    return _statements;
  }

  public boolean process(
      AbstractRoute inputRoute,
      AbstractRouteBuilder<?, ?> outputRoute,
      Ip peerAddress,
      String vrf,
      Direction direction) {
    Environment environment =
        new Environment(_owner, vrf, inputRoute, null, outputRoute, peerAddress, direction);
    Result result = call(environment);
    return result.getBooleanValue();
  }

  @JsonProperty(PROP_STATEMENTS)
  public void setStatements(List<Statement> statements) {
    _statements = statements;
  }

  public RoutingPolicy simplify() {
    List<Statement> simpleStatements = new ArrayList<>();
    for (Statement statement : _statements) {
      simpleStatements.addAll(statement.simplify());
    }
    RoutingPolicy simple = new RoutingPolicy(_key, _owner);
    simple.setStatements(simpleStatements);
    return simple;
  }
}
