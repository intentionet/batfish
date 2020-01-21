package org.batfish.representation.juniper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.batfish.common.Warnings;
import org.batfish.datamodel.ExprAclLine;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.TraceElement;
import org.batfish.datamodel.acl.MatchHeaderSpace;

public final class BaseApplication implements Application, Serializable {

  public static final class Term implements Serializable {

    private HeaderSpace _headerSpace;

    public Term() {
      _headerSpace = HeaderSpace.builder().build();
    }

    public void applyTo(HeaderSpace.Builder destinationHeaderSpace) {
      destinationHeaderSpace.setIpProtocols(
          Iterables.concat(destinationHeaderSpace.getIpProtocols(), _headerSpace.getIpProtocols()));
      destinationHeaderSpace.setDstPorts(
          Iterables.concat(destinationHeaderSpace.getDstPorts(), _headerSpace.getDstPorts()));
      destinationHeaderSpace.setSrcPorts(
          Iterables.concat(destinationHeaderSpace.getSrcPorts(), _headerSpace.getSrcPorts()));
    }

    public HeaderSpace getHeaderSpace() {
      return _headerSpace;
    }

    public void setHeaderSpace(HeaderSpace headerSpace) {
      _headerSpace = headerSpace;
    }
  }

  private boolean _ipv6;

  private Term _mainTerm;

  private final Map<String, Term> _terms;

  private String _name;

  public BaseApplication(String name) {
    _mainTerm = new Term();
    _terms = new LinkedHashMap<>();
    _name = name;
  }

  @Override
  public void applyTo(
      JuniperConfiguration jc,
      HeaderSpace.Builder srcHeaderSpaceBuilder,
      LineAction action,
      List<? super ExprAclLine> lines,
      Warnings w) {
    Collection<Term> terms;
    if (_terms.isEmpty()) {
      terms = ImmutableList.of(_mainTerm);
    } else {
      terms = _terms.values();
    }
    for (Term term : terms) {
      HeaderSpace oldHeaderSpace = srcHeaderSpaceBuilder.build();
      HeaderSpace.Builder newHeaderSpaceBuilder =
          HeaderSpace.builder()
              .setDstIps(oldHeaderSpace.getDstIps())
              .setSrcIps(oldHeaderSpace.getSrcIps());
      term.applyTo(newHeaderSpaceBuilder);
      ExprAclLine newLine =
          ExprAclLine.builder()
              .setAction(action)
              .setMatchCondition(new MatchHeaderSpace(newHeaderSpaceBuilder.build()))
              .setTraceElement(getTraceElement())
              .build();
      lines.add(newLine);
    }
  }

  @Override
  public boolean getIpv6() {
    return _ipv6;
  }

  public Term getMainTerm() {
    return _mainTerm;
  }

  public Map<String, Term> getTerms() {
    return _terms;
  }

  public void setIpv6(boolean ipv6) {
    _ipv6 = true;
  }

  TraceElement getTraceElement() {
    return TraceElement.of(String.format("Matched application %s", _name));
  }
}
