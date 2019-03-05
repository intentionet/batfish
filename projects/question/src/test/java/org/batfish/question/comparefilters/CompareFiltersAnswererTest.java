package org.batfish.question.comparefilters;

import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.datamodel.LineAction.DENY;
import static org.batfish.datamodel.LineAction.PERMIT;
import static org.batfish.question.comparefilters.CompareFiltersAnswerer.compareFilters;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.sf.javabdd.BDD;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.datamodel.LineAction;
import org.junit.Before;
import org.junit.Test;

public final class CompareFiltersAnswererTest {
  private static final String HOSTNAME = "hostname";
  private static final String FILTER = "filter";

  private BDDPacket _pkt;

  @Before
  public void setup() {
    _pkt = new BDDPacket();
  }

  private static List<FilterDifference> compare(
      List<LineAction> currentActions,
      List<BDD> currentBdds,
      List<LineAction> referenceActions,
      List<BDD> referenceBdds) {
    return compareFilters(
            HOSTNAME, FILTER, currentActions, currentBdds, referenceActions, referenceBdds)
        .collect(Collectors.toList());
  }

  private static FilterDifference difference(
      @Nullable Integer currentIndex, @Nullable Integer referenceIndex) {
    return new FilterDifference(HOSTNAME, FILTER, currentIndex, referenceIndex);
  }

  private List<BDD> aclBdds(BDD... lineBdds) {
    checkArgument(lineBdds.length > 0);

    ImmutableList.Builder<BDD> aclBdds = ImmutableList.builder();
    BDD reach = _pkt.getFactory().one();
    for (BDD lineBdd : lineBdds) {
      aclBdds.add(reach.and(lineBdd));
      reach = reach.and(lineBdd.not());
    }
    // add the BDD for flows that match no lines in the acl.
    aclBdds.add(reach);
    return aclBdds.build();
  }

  @Test
  public void testCompareFilters() {
    BDD dstIp0 = _pkt.getDstIp().value(0);
    BDD dstIp1 = _pkt.getDstIp().value(1);
    BDD srcIp0 = _pkt.getSrcIp().value(0);
    BDD dstPort0 = _pkt.getDstPort().value(0);
    BDD srcPort0 = _pkt.getSrcPort().value(0);

    List<BDD> zeroBdds = aclBdds(dstIp0, srcIp0, dstPort0, srcPort0);

    // Representation of an empty Acl.
    List<BDD> emptyAclBdds = ImmutableList.of(_pkt.getFactory().one());
    List<LineAction> emptyAclActions = ImmutableList.of();

    List<LineAction> pppp = ImmutableList.of(PERMIT, PERMIT, PERMIT, PERMIT);
    List<LineAction> dpdp = ImmutableList.of(DENY, PERMIT, DENY, PERMIT);

    // If nothing has changed, return no differences
    assertThat(compare(pppp, zeroBdds, pppp, zeroBdds), empty());

    // If all lines deleted, return a difference for each permit line
    assertThat(
        compare(emptyAclActions, emptyAclBdds, pppp, zeroBdds),
        contains(
            difference(null, 0), difference(null, 1), difference(null, 2), difference(null, 3)));
    assertThat(
        compare(emptyAclActions, emptyAclBdds, dpdp, zeroBdds),
        contains(difference(null, 1), difference(null, 3)));

    // If all lines added, return a difference for each permit line
    assertThat(
        compare(pppp, zeroBdds, emptyAclActions, emptyAclBdds),
        contains(
            difference(0, null), difference(1, null), difference(2, null), difference(3, null)));
    assertThat(
        compare(dpdp, zeroBdds, emptyAclActions, emptyAclBdds),
        contains(difference(1, null), difference(3, null)));

    {
      List<BDD> currentAcl = zeroBdds;
      List<BDD> referenceAcl = aclBdds(dstIp1, srcIp0, dstPort0, srcPort0);

      // if we change a permit line, we get differences for that line in each direction
      assertThat(
          compare(pppp, currentAcl, pppp, referenceAcl),
          contains(difference(0, null), difference(null, 0)));

      // if we change a deny line, we affect later permit lines
      List<FilterDifference> compare = compare(dpdp, currentAcl, dpdp, referenceAcl);
      assertThat(
          compare,
          contains(difference(0, 1), difference(0, 3), difference(1, 0), difference(3, 0)));
    }
  }
}
