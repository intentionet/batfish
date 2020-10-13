package org.batfish.representation.juniper;

import static org.batfish.datamodel.matchers.HeaderSpaceMatchers.hasNotDstIps;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.SubRange;
import org.junit.Before;
import org.junit.Test;

/** Test for {@link FwFromDestinationPrefixListExcept} */
public class FwFromDestinationPrefixListExceptTest {
  private JuniperConfiguration _jc;
  private Warnings _w;
  private Configuration _c;

  private static final String BASE_PREFIX_LIST_NAME = "prefixList";
  private static final String BASE_IP_PREFIX = "1.2.3.4/32";

  @Before
  public void setup() {
    _jc = new JuniperConfiguration();
    _jc.getMasterLogicalSystem()
        .getPrefixLists()
        .put(BASE_PREFIX_LIST_NAME, new PrefixList(BASE_PREFIX_LIST_NAME));
    _w = new Warnings();
    _c = new Configuration("test", ConfigurationFormat.FLAT_JUNIPER);
    RouteFilterList rflist = new RouteFilterList(BASE_PREFIX_LIST_NAME);
    RouteFilterLine rfline =
        new RouteFilterLine(LineAction.PERMIT, Prefix.parse(BASE_IP_PREFIX), SubRange.singleton(0));
    rflist.addLine(rfline);
    _c.getRouteFilterLists().put(BASE_PREFIX_LIST_NAME, rflist);
  }

  @Test
  public void testToHeaderSpace() {
    IpSpace baseIpSpace = IpWildcard.parse(BASE_IP_PREFIX).toIpSpace();

    FwFromDestinationPrefixListExcept fwFrom =
        new FwFromDestinationPrefixListExcept(BASE_PREFIX_LIST_NAME);

    assertThat(fwFrom.toHeaderspace(_jc, _c, _w), hasNotDstIps(equalTo(baseIpSpace)));
  }
}
