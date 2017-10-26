package org.batfish.datamodel.questions;

import java.util.SortedSet;
import org.batfish.datamodel.ForwardingAction;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.Protocol;

public interface IReachabilityQuestion extends IQuestion {

  void setActions(SortedSet<ForwardingAction> actionSet);

  void setDstIps(SortedSet<IpWildcard> singleton);

  void setDstProtocols(SortedSet<Protocol> protocols);

  void setIngressNodeRegex(String ingressNodeRegex);

  void setNotDstProtocols(SortedSet<Protocol> protocols);
}
