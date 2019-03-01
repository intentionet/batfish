package org.batfish.datamodel;

import static org.batfish.datamodel.matchers.IpSpaceMatchers.containsIp;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import org.batfish.common.util.BatfishObjectMapper;
import org.junit.Test;

public class IpSpaceTest {

  @Test
  public void testIpSpace() {
    IpWildcardSetIpSpace any = IpWildcardSetIpSpace.builder().including(IpWildcard.ANY).build();
    IpWildcardSetIpSpace justMax =
        IpWildcardSetIpSpace.builder().including(new IpWildcard(Ip.MAX)).build();
    IpWildcardSetIpSpace anyExceptMax =
        IpWildcardSetIpSpace.builder()
            .including(IpWildcard.ANY)
            .excluding(new IpWildcard(Ip.MAX))
            .build();
    IpWildcardSetIpSpace none1 = IpWildcardSetIpSpace.builder().build();
    IpWildcardSetIpSpace none2 =
        IpWildcardSetIpSpace.builder().including(IpWildcard.ANY).excluding(IpWildcard.ANY).build();
    IpWildcardSetIpSpace someButNotMax =
        IpWildcardSetIpSpace.builder().including(new IpWildcard("1.2.3.4")).build();

    /*
     * Contains every IP, so should contain Ip.MAX
     */
    assertThat(any, containsIp(Ip.MAX));

    /*
     * Contains just IP.MAX, so should contain Ip.MAX
     */
    assertThat(justMax, containsIp(Ip.MAX));

    /*
     * Should not contain Ip.MAX because of explicit blacklist
     */
    assertThat(anyExceptMax, not(containsIp(Ip.MAX)));

    /*
     * Should not contain Ip.MAX because contains nothing
     */
    assertThat(none1, not(containsIp(Ip.MAX)));

    /*
     * Should not contain Ip.MAX because of general blacklist
     */
    assertThat(none2, not(containsIp(Ip.MAX)));

    /*
     * Should not contain Ip.MAX because not in whitelist
     */
    assertThat(someButNotMax, not(containsIp(Ip.MAX)));
  }

  @Test
  public void testIpSpaceJacksonSerialization() throws IOException {
    Ip ip = Ip.parse("1.0.0.0");
    IpIpSpace ipIpSpace = ip.toIpSpace();
    Prefix p = Prefix.create(ip, 24);
    IpSpace prefixIpSpace = p.toIpSpace();
    IpWildcard ipWildcard1 = new IpWildcard(p);
    IpWildcardIpSpace ipWildcard1IpSpace = ipWildcard1.toIpSpace();
    IpWildcard ipWildcard2 = new IpWildcard(ip);
    IpWildcardIpSpace ipWildcard2IpSpace = ipWildcard2.toIpSpace();
    IpWildcard ipWildcard3 = new IpWildcard(ip, Ip.parse("0.255.0.255"));
    IpWildcardIpSpace ipWildcard3IpSpace = ipWildcard3.toIpSpace();
    IpSpace ipWildcardSetIpSpace =
        IpWildcardSetIpSpace.builder().including(ipWildcard1, ipWildcard2, ipWildcard3).build();
    IpSpace aclIpSpace1 =
        AclIpSpace.permitting(ipWildcardSetIpSpace)
            .thenPermitting(ipIpSpace)
            .thenPermitting(prefixIpSpace)
            .thenRejecting(ipWildcard1IpSpace)
            .thenPermitting(EmptyIpSpace.INSTANCE)
            .thenRejecting(UniverseIpSpace.INSTANCE)
            .build();
    for (IpSpace ipSpace :
        ImmutableList.<IpSpace>of(
            ipIpSpace,
            prefixIpSpace,
            ipWildcard1IpSpace,
            ipWildcard2IpSpace,
            ipWildcard3IpSpace,
            ipWildcardSetIpSpace,
            aclIpSpace1)) {
      String jsonString = BatfishObjectMapper.writePrettyString(ipSpace);
      IpSpace deserializedIpSpace =
          BatfishObjectMapper.mapper().readValue(jsonString, IpSpace.class);

      /* IpSpace should be equal to deserialized version */
      assertThat(ipSpace, equalTo(deserializedIpSpace));
    }
  }
}
