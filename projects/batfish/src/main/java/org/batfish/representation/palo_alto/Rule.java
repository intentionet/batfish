package org.batfish.representation.palo_alto;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.LineAction;

/** PAN datamodel component containing security rule configuration */
@ParametersAreNonnullByDefault
public final class Rule implements Serializable {
  private static final long serialVersionUID = 1L;

  // Name of the rule
  @Nonnull private final String _name;
  // Action of the rule
  @Nonnull private LineAction _action;
  // Owning Vsys of this rule
  @Nonnull private final Vsys _vsys;

  // Description of the rule
  @Nullable private String _description;

  private boolean _disabled;

  // Zones
  @Nonnull private final SortedSet<String> _from;
  @Nonnull private final SortedSet<String> _to;

  // IPs
  @Nonnull private final List<RuleEndpoint> _source;
  @Nonnull private final List<RuleEndpoint> _destination;
  private boolean _negateSource;
  private boolean _negateDestination;

  // Service
  @Nonnull private final SortedSet<ServiceOrServiceGroupReference> _service;

  @Nonnull private final SortedSet<String> _applications;

  public Rule(String name, Vsys vsys) {
    _action = LineAction.DENY;
    _applications = new TreeSet<>();
    _destination = new LinkedList<>();
    _negateDestination = false;
    _disabled = false;
    _from = new TreeSet<>();
    _service = new TreeSet<>();
    _source = new LinkedList<>();
    _negateSource = false;
    _to = new TreeSet<>();
    _name = name;
    _vsys = vsys;
  }

  @Nonnull
  public String getName() {
    return _name;
  }

  @Nonnull
  public LineAction getAction() {
    return _action;
  }

  @Nonnull
  public SortedSet<String> getApplications() {
    return _applications;
  }

  @Nullable
  public String getDescription() {
    return _description;
  }

  @Nonnull
  public List<RuleEndpoint> getDestination() {
    return _destination;
  }

  public boolean getDisabled() {
    return _disabled;
  }

  @Nonnull
  public SortedSet<String> getFrom() {
    return _from;
  }

  public boolean getNegateDestination() {
    return _negateDestination;
  }

  public void setNegateDestination(boolean negateDestination) {
    _negateDestination = negateDestination;
  }

  public boolean getNegateSource() {
    return _negateSource;
  }

  public void setNegateSource(boolean negateSource) {
    _negateSource = negateSource;
  }

  @Nonnull
  public SortedSet<ServiceOrServiceGroupReference> getService() {
    return _service;
  }

  @Nonnull
  public List<RuleEndpoint> getSource() {
    return _source;
  }

  @Nonnull
  public SortedSet<String> getTo() {
    return _to;
  }

  @Nonnull
  public Vsys getVsys() {
    return _vsys;
  }

  public void setAction(LineAction action) {
    _action = action;
  }

  public void setDescription(String description) {
    _description = description;
  }

  public void setDisabled(boolean disabled) {
    _disabled = disabled;
  }
}
