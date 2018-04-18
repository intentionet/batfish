package org.batfish.datamodel;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import org.batfish.common.util.BatfishObjectMapper;
import org.hamcrest.FeatureMatcher;
import org.junit.Test;

public class IpSpaceTest {

  private static class IpSpaceWrapper {

    private static final String PROP_IP_SPACE = "ipSpace";

    private IpSpace _ipSpace;

    @JsonSerialize(
      using = IpSpaceToJacksonSerializableIpSpace.class,
      as = JacksonSerializableIpSpace.class
    )
    @JsonProperty(PROP_IP_SPACE)
    private IpSpace getIpspace() {
      return _ipSpace;
    }

    @JsonDeserialize(
      using = JacksonSerializableIpSpaceToIpSpace.class,
      as = JacksonSerializableIpSpace.class
    )
    @JsonProperty(PROP_IP_SPACE)
    private void setIpSpace(IpSpace ipSpace) {
      _ipSpace = ipSpace;
    }
  }

  private static class Contains extends FeatureMatcher<IpWildcardSetIpSpace, Boolean> {

    private final Ip _ip;

    public Contains(Ip ip) {
      super(equalTo(true), "contains " + ip, "contains " + ip);
      _ip = ip;
    }

    @Override
    protected Boolean featureValueOf(IpWildcardSetIpSpace actual) {
      return actual.containsIp(_ip);
    }
  }

  public static Contains contains(Ip ip) {
    return new Contains(ip);
  }

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
    assertThat(any, contains(Ip.MAX));

    /*
     * Contains just IP.MAX, so should contain Ip.MAX
     */
    assertThat(justMax, contains(Ip.MAX));

    /*
     * Should not contain Ip.MAX because of explicit blacklist
     */
    assertThat(anyExceptMax, not(contains(Ip.MAX)));

    /*
     * Should not contain Ip.MAX because contains nothing
     */
    assertThat(none1, not(contains(Ip.MAX)));

    /*
     * Should not contain Ip.MAX because of general blacklist
     */
    assertThat(none2, not(contains(Ip.MAX)));

    /*
     * Should not contain Ip.MAX because not in whitelist
     */
    assertThat(someButNotMax, not(contains(Ip.MAX)));
  }

  @Test
  public void testIpSpaceJacksonSerialization() throws IOException {
    Ip ip1 = new Ip("1.0.0.0");
    Prefix p1 = new Prefix(ip1, 24);
    IpWildcard ipWildcard1 = new IpWildcard(p1);
    IpWildcard ipWildcard2 = new IpWildcard(ip1);
    IpWildcard ipWildcard3 = new IpWildcard(ip1, new Ip("0.255.0.255"));
    IpSpace ipWildcardSetIpSpace =
        IpWildcardSetIpSpace.builder().including(ipWildcard1, ipWildcard2, ipWildcard3).build();
    IpSpace aclIpSpace1 =
        AclIpSpace.permitting(ipWildcardSetIpSpace)
            .thenPermitting(ip1)
            .thenPermitting(p1)
            .thenRejecting(ipWildcard1)
            .thenPermitting(EmptyIpSpace.INSTANCE)
            .thenRejecting(UniverseIpSpace.INSTANCE)
            .build();
    for (IpSpace ipSpace :
        ImmutableList.<IpSpace>of(
            ip1, p1, ipWildcard1, ipWildcard2, ipWildcard3, ipWildcardSetIpSpace, aclIpSpace1)) {
      IpSpaceWrapper wrapper = new IpSpaceWrapper();
      wrapper.setIpSpace(ipSpace);
      String jsonString = BatfishObjectMapper.writePrettyString(wrapper);
      IpSpaceWrapper deserializedIpSpaceWrapper =
          BatfishObjectMapper.mapper().readValue(jsonString, IpSpaceWrapper.class);

      /* IpSpace should be equal to deserialized version */
      assertThat(ipSpace, equalTo(deserializedIpSpaceWrapper.getIpspace()));
    }
  }
}
