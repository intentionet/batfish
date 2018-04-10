package org.batfish.representation.juniper;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.batfish.common.Warnings;
import org.batfish.common.util.ComparableStructure;
import org.batfish.common.util.DefinedStructure;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.acl.MatchHeaderSpace;

public class BaseApplication extends DefinedStructure<String> implements Application {

  public static class Term extends ComparableStructure<String> {

    /** */
    private static final long serialVersionUID = 1L;

    private final HeaderSpace.Builder _headerSpaceBuilder;

    private final Supplier<HeaderSpace> _headerSpace;

    public Term(String name) {
      super(name);
      _headerSpaceBuilder = HeaderSpace.builder();
      _headerSpace = Suppliers.memoize(_headerSpaceBuilder::build);
    }

    public void applyTo(HeaderSpace.Builder destinationHeaderSpace) {
      destinationHeaderSpace.setIpProtocols(
          Iterables.concat(
              destinationHeaderSpace.getIpProtocols(), _headerSpace.get().getIpProtocols()));
      destinationHeaderSpace.setDstPorts(
          Iterables.concat(destinationHeaderSpace.getDstPorts(), _headerSpace.get().getDstPorts()));
      destinationHeaderSpace.setSrcPorts(
          Iterables.concat(destinationHeaderSpace.getSrcPorts(), _headerSpace.get().getSrcPorts()));
    }

    public HeaderSpace.Builder getHeaderSpaceBuilder() {
      return _headerSpaceBuilder;
    }
  }

  /** */
  private static final long serialVersionUID = 1L;

  private boolean _ipv6;

  private Term _mainTerm;

  private final Map<String, Term> _terms;

  public BaseApplication(String name, int definitionLine) {
    super(name, definitionLine);
    _mainTerm = new Term(getMainTermName());
    _terms = new LinkedHashMap<>();
  }

  @Override
  public void applyTo(
      HeaderSpace.Builder srcHeaderSpaceBuilder,
      LineAction action,
      List<IpAccessListLine> lines,
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
      IpAccessListLine newLine =
          IpAccessListLine.builder()
              .setAction(action)
              .setMatchCondition(new MatchHeaderSpace(newHeaderSpaceBuilder.build()))
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

  private String getMainTermName() {
    return "~MAIN_TERM~" + _key;
  }

  public Map<String, Term> getTerms() {
    return _terms;
  }

  public void setIpv6(boolean ipv6) {
    _ipv6 = true;
  }
}
