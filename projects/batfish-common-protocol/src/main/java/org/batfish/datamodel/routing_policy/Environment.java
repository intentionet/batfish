package org.batfish.datamodel.routing_policy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AbstractRoute6;
import org.batfish.datamodel.AbstractRouteBuilder;
import org.batfish.datamodel.AbstractRouteDecorator;
import org.batfish.datamodel.AnnotatedRoute;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Vrf;

public class Environment {

  public static Builder builder(@Nonnull Configuration c) {
    return new Builder(c);
  }

  public enum Direction {
    IN,
    OUT
  }

  private boolean _buffered;

  private boolean _callExprContext;

  private boolean _callStatementContext;

  @Nonnull private final Configuration _configuration;

  private boolean _defaultAction;

  private String _defaultPolicy;

  private final Direction _direction;

  private boolean _error;

  private BgpRoute.Builder _intermediateBgpAttributes;

  private boolean _localDefaultAction;

  private final AbstractRoute _originalRoute;

  @Nullable private final AbstractRoute6 _originalRoute6;

  private final AbstractRouteBuilder<?, ?> _outputRoute;

  @Nullable private final Ip _peerAddress;

  @Nullable private final Prefix _peerPrefix;

  private boolean _readFromIntermediateBgpAttributes;

  @Nullable private final String _routeSourceVrf;

  private final boolean _useOutputAttributes;

  private final Vrf _vrf;

  private boolean _writeToIntermediateBgpAttributes;

  private Boolean _suppressed;

  private Environment(
      boolean buffered,
      boolean callExprContext,
      boolean callStatementContext,
      @Nonnull Configuration configuration,
      boolean defaultAction,
      String defaultPolicy,
      Direction direction,
      boolean error,
      BgpRoute.Builder intermediateBgpAttributes,
      boolean localDefaultAction,
      AbstractRouteDecorator originalRoute,
      @Nullable AbstractRoute6 originalRoute6,
      AbstractRouteBuilder<?, ?> outputRoute,
      @Nullable Ip peerAddress,
      @Nullable Prefix peerPrefix,
      boolean readFromIntermediateBgpAttributes,
      Vrf vrf,
      boolean writeToIntermediateBgpAttributes) {
    _buffered = buffered;
    _callExprContext = callExprContext;
    _callStatementContext = callStatementContext;
    _configuration = configuration;
    _defaultAction = defaultAction;
    _defaultPolicy = defaultPolicy;
    _direction = direction;
    _error = error;
    _intermediateBgpAttributes = intermediateBgpAttributes;
    _localDefaultAction = localDefaultAction;
    _originalRoute = originalRoute == null ? null : originalRoute.getAbstractRoute();
    _originalRoute6 = originalRoute6;
    _outputRoute = outputRoute;
    _peerAddress = peerAddress;
    _peerPrefix = peerPrefix;
    _readFromIntermediateBgpAttributes = readFromIntermediateBgpAttributes;
    _routeSourceVrf =
        originalRoute instanceof AnnotatedRoute
            ? ((AnnotatedRoute<?>) originalRoute).getSourceVrf()
            : null;
    ConfigurationFormat format = configuration.getConfigurationFormat();
    _useOutputAttributes =
        format == ConfigurationFormat.JUNIPER
            || format == ConfigurationFormat.JUNIPER_SWITCH
            || format == ConfigurationFormat.FLAT_JUNIPER;
    _vrf = vrf;
    _writeToIntermediateBgpAttributes = writeToIntermediateBgpAttributes;
  }

  public boolean getBuffered() {
    return _buffered;
  }

  public boolean getCallExprContext() {
    return _callExprContext;
  }

  public boolean getCallStatementContext() {
    return _callStatementContext;
  }

  @Nonnull
  public Configuration getConfiguration() {
    return _configuration;
  }

  public boolean getDefaultAction() {
    return _defaultAction;
  }

  public String getDefaultPolicy() {
    return _defaultPolicy;
  }

  public Direction getDirection() {
    return _direction;
  }

  public boolean getError() {
    return _error;
  }

  public BgpRoute.Builder getIntermediateBgpAttributes() {
    return _intermediateBgpAttributes;
  }

  public boolean getLocalDefaultAction() {
    return _localDefaultAction;
  }

  public AbstractRoute getOriginalRoute() {
    return _originalRoute;
  }

  @Nullable
  public AbstractRoute6 getOriginalRoute6() {
    return _originalRoute6;
  }

  public AbstractRouteBuilder<?, ?> getOutputRoute() {
    return _outputRoute;
  }

  @Nullable
  public Ip getPeerAddress() {
    return _peerAddress;
  }

  @Nullable
  public Prefix getPeerPrefix() {
    return _peerPrefix;
  }

  public boolean getReadFromIntermediateBgpAttributes() {
    return _readFromIntermediateBgpAttributes;
  }

  public String getRouteSourceVrf() {
    return _routeSourceVrf;
  }

  public boolean getUseOutputAttributes() {
    return _useOutputAttributes;
  }

  public Vrf getVrf() {
    return _vrf;
  }

  public boolean getWriteToIntermediateBgpAttributes() {
    return _writeToIntermediateBgpAttributes;
  }

  public void setBuffered(boolean buffered) {
    _buffered = buffered;
  }

  public void setCallExprContext(boolean callExprContext) {
    _callExprContext = callExprContext;
  }

  public void setCallStatementContext(boolean callStatementContext) {
    _callStatementContext = callStatementContext;
  }

  public void setDefaultAction(boolean defaultAction) {
    _defaultAction = defaultAction;
  }

  public void setDefaultPolicy(String defaultPolicy) {
    _defaultPolicy = defaultPolicy;
  }

  public void setError(boolean error) {
    _error = error;
  }

  public void setIntermediateBgpAttributes(BgpRoute.Builder intermediateBgpAttributes) {
    _intermediateBgpAttributes = intermediateBgpAttributes;
  }

  public void setLocalDefaultAction(boolean localDefaultAction) {
    _localDefaultAction = localDefaultAction;
  }

  public void setReadFromIntermediateBgpAttributes(boolean readFromIntermediateBgpAttributes) {
    _readFromIntermediateBgpAttributes = readFromIntermediateBgpAttributes;
  }

  public void setWriteToIntermediateBgpAttributes(boolean writeToIntermediateBgpAttributes) {
    _writeToIntermediateBgpAttributes = writeToIntermediateBgpAttributes;
  }

  public static final class Builder {
    private boolean _buffered;
    private boolean _callExprContext;
    private boolean _callStatementContext;
    private Configuration _configuration;
    private boolean _defaultAction;
    private String _defaultPolicy;
    private Direction _direction;
    private boolean _error;
    private BgpRoute.Builder _intermediateBgpAttributes;
    private boolean _localDefaultAction;
    private AbstractRouteDecorator _originalRoute;
    private AbstractRoute6 _originalRoute6;
    private AbstractRouteBuilder<?, ?> _outputRoute;
    @Nullable private Ip _peerAddress;
    @Nullable private Prefix _peerPrefix;
    private boolean _readFromIntermediateBgpAttributes;
    private String _vrf;
    private boolean _writeToIntermediateBgpAttributes;

    private Builder(Configuration c) {
      _configuration = c;
    }

    public Builder setBuffered(boolean buffered) {
      _buffered = buffered;
      return this;
    }

    public Builder setCallExprContext(boolean callExprContext) {
      _callExprContext = callExprContext;
      return this;
    }

    public Builder setCallStatementContext(boolean callStatementContext) {
      _callStatementContext = callStatementContext;
      return this;
    }

    public Builder setConfiguration(Configuration configuration) {
      _configuration = configuration;
      return this;
    }

    public Builder setDefaultAction(boolean defaultAction) {
      _defaultAction = defaultAction;
      return this;
    }

    public Builder setDefaultPolicy(String defaultPolicy) {
      _defaultPolicy = defaultPolicy;
      return this;
    }

    public Builder setDirection(Direction direction) {
      _direction = direction;
      return this;
    }

    public Builder setError(boolean error) {
      _error = error;
      return this;
    }

    public Builder setIntermediateBgpAttributes(BgpRoute.Builder intermediateBgpAttributes) {
      _intermediateBgpAttributes = intermediateBgpAttributes;
      return this;
    }

    public Builder setLocalDefaultAction(boolean localDefaultAction) {
      _localDefaultAction = localDefaultAction;
      return this;
    }

    public Builder setOriginalRoute(AbstractRouteDecorator originalRoute) {
      _originalRoute = originalRoute;
      return this;
    }

    public Builder setOriginalRoute6(AbstractRoute6 originalRoute6) {
      _originalRoute6 = originalRoute6;
      return this;
    }

    public Builder setOutputRoute(AbstractRouteBuilder<?, ?> outputRoute) {
      _outputRoute = outputRoute;
      return this;
    }

    public Builder setPeerAddress(@Nullable Ip peerAddress) {
      _peerAddress = peerAddress;
      return this;
    }

    public Builder setPeerPrefix(@Nullable Prefix peerPrefix) {
      _peerPrefix = peerPrefix;
      return this;
    }

    public Builder setReadFromIntermediateBgpAttributes(boolean readFromIntermediateBgpAttributes) {
      _readFromIntermediateBgpAttributes = readFromIntermediateBgpAttributes;
      return this;
    }

    public Builder setVrf(String vrf) {
      _vrf = vrf;
      return this;
    }

    public Builder setWriteToIntermediateBgpAttributes(boolean writeToIntermediateBgpAttributes) {
      _writeToIntermediateBgpAttributes = writeToIntermediateBgpAttributes;
      return this;
    }

    public Environment build() {
      Vrf vrf = _configuration.getVrfs().get(_vrf);
      return new Environment(
          _buffered,
          _callExprContext,
          _callStatementContext,
          _configuration,
          _defaultAction,
          _defaultPolicy,
          _direction,
          _error,
          _intermediateBgpAttributes,
          _localDefaultAction,
          _originalRoute,
          _originalRoute6,
          _outputRoute,
          _peerAddress,
          _peerPrefix,
          _readFromIntermediateBgpAttributes,
          vrf,
          _writeToIntermediateBgpAttributes);
    }
  }

  public @Nullable Boolean getSuppressed() {
    return _suppressed;
  }

  public void setSuppressed(@Nullable Boolean suppressed) {
    _suppressed = suppressed;
  }
}
