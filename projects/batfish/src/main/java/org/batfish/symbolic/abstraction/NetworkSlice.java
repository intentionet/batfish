package org.batfish.symbolic.abstraction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Prefix;
import org.batfish.symbolic.Graph;
import org.batfish.symbolic.bdd.BDDNetwork;
import org.batfish.symbolic.utils.Tuple;

public class NetworkSlice {

  private HeaderSpace _headerSpace;

  private Abstraction _abstraction;

  private boolean _isDefaultCase;

  public NetworkSlice(
      HeaderSpace headerSpace, @Nullable Abstraction abstraction, boolean isDefaultCase) {
    this._headerSpace = headerSpace;
    this._abstraction = abstraction;
    this._isDefaultCase = isDefaultCase;
  }

  public static ArrayList<Supplier<NetworkSlice>> allSlices(DestinationClasses dcs, int fails) {
    BDDNetwork network = BDDNetwork.create(dcs.getGraph());
    ArrayList<Supplier<NetworkSlice>> classes = new ArrayList<>();
    for (Entry<Set<String>, Tuple<HeaderSpace, Tuple<List<Prefix>, Boolean>>> entry :
        dcs.getHeaderspaceMap().entrySet()) {
      Set<String> devices = entry.getKey();
      HeaderSpace headerspace = entry.getValue().getFirst();
      List<Prefix> prefixes = entry.getValue().getSecond().getFirst();
      Boolean isDefaultCase = entry.getValue().getSecond().getSecond();
      Supplier<NetworkSlice> sup =
          () ->
              AbstractionBuilder.createGraph(
                  dcs, network, devices, headerspace, prefixes, fails, isDefaultCase);
      classes.add(sup);
    }
    return classes;
  }

  public HeaderSpace getHeaderSpace() {
    return _headerSpace;
  }

  public Abstraction getAbstraction() {
    return _abstraction;
  }

  public Graph getGraph() {
    return _abstraction.getGraph();
  }

  public boolean getIsDefaultCase() {
    return _isDefaultCase;
  }
}
