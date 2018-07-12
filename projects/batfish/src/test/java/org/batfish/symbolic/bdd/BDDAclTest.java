package org.batfish.symbolic.bdd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Map;
import net.sf.javabdd.BDD;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.PermittedByAcl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class BDDAclTest {
  @Rule public ExpectedException exception = ExpectedException.none();

  private NetworkFactory _nf;

  private BDDPacket _pkt;

  @Before
  public void setup() {
    _nf = new NetworkFactory();
    _pkt = new BDDPacket();
  }

  private IpAccessList aclWithLines(IpAccessListLine... lines) {
    return _nf.aclBuilder().setLines(Arrays.asList(lines)).build();
  }

  private static IpAccessListLine accepting(AclLineMatchExpr matchExpr) {
    return IpAccessListLine.accepting().setMatchCondition(matchExpr).build();
  }

  private static IpAccessListLine accepting(HeaderSpace headerSpace) {
    return accepting(new MatchHeaderSpace(headerSpace));
  }

  @Test
  public void testPermittedByAcl() {
    Ip fooIp = new Ip("1.1.1.1");
    BDD fooIpBDD = _pkt.getDstIp().value(fooIp.asLong());
    IpAccessList fooAcl =
        aclWithLines(accepting(HeaderSpace.builder().setDstIps(fooIp.toIpSpace()).build()));
    Map<String, IpAccessList> namedAcls = ImmutableMap.of("foo", fooAcl);
    IpAccessList acl = aclWithLines(accepting(new PermittedByAcl("foo")));
    BDD bdd = BDDAcl.create(acl, namedAcls, ImmutableMap.of()).getBdd();
    assertThat(bdd, equalTo(fooIpBDD));
  }

  @Test
  public void testPermittedByAcl_undefined() {
    IpAccessList acl = aclWithLines(accepting(new PermittedByAcl("foo")));
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Undefined PermittedByAcl reference: foo");
    BDDAcl.create(acl);
  }

  @Test
  public void testPermittedByAcl_circular() {
    PermittedByAcl permittedByAcl = new PermittedByAcl("foo");
    IpAccessList fooAcl = aclWithLines(accepting(permittedByAcl));
    Map<String, IpAccessList> namedAcls = ImmutableMap.of("foo", fooAcl);
    exception.expect(BatfishException.class);
    exception.expectMessage("Circular PermittedByAcl reference: foo");
    BDDAcl.create(fooAcl, namedAcls, ImmutableMap.of());
  }
}
