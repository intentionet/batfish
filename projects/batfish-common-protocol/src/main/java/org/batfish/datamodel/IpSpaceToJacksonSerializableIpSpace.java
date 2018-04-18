package org.batfish.datamodel;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import org.batfish.datamodel.visitors.GenericIpSpaceVisitor;

public class IpSpaceToJacksonSerializableIpSpace extends JsonSerializer<IpSpace>
    implements GenericIpSpaceVisitor<JacksonSerializableIpSpace> {

  private IpSpaceToJacksonSerializableIpSpace() {}

  private static final IpSpaceToJacksonSerializableIpSpace INSTANCE =
      new IpSpaceToJacksonSerializableIpSpace();

  public static JacksonSerializableIpSpace toJacksonSerializableIpSpace(IpSpace ipSpace) {
    return ipSpace.accept(INSTANCE);
  }

  @Override
  public void serialize(IpSpace value, JsonGenerator gen, SerializerProvider serializers)
      throws IOException {
    gen.writeObject(value.accept(this));
  }

  @Override
  public JacksonSerializableIpSpace castToGenericIpSpaceVisitorReturnType(Object o) {
    return (JacksonSerializableIpSpace) o;
  }

  @Override
  public JacksonSerializableIpSpace visitAclIpSpace(AclIpSpace aclIpSpace) {
    return new JacksonSerializableAclIpSpace(aclIpSpace);
  }

  @Override
  public JacksonSerializableIpSpace visitEmptyIpSpace(EmptyIpSpace emptyIpSpace) {
    return emptyIpSpace;
  }

  @Override
  public JacksonSerializableIpSpace visitIp(Ip ip) {
    return new SimpleJsonIpSpace(ip);
  }

  @Override
  public JacksonSerializableIpSpace visitIpWildcard(IpWildcard ipWildcard) {
    return new SimpleJsonIpSpace(ipWildcard);
  }

  @Override
  public JacksonSerializableIpSpace visitIpWildcardSetIpSpace(
      IpWildcardSetIpSpace ipWildcardSetIpSpace) {
    return new SimpleJsonIpSpace(ipWildcardSetIpSpace);
  }

  @Override
  public JacksonSerializableIpSpace visitPrefix(Prefix prefix) {
    return new SimpleJsonIpSpace(prefix);
  }

  @Override
  public JacksonSerializableIpSpace visitUniverseIpSpace(UniverseIpSpace universeIpSpace) {
    return universeIpSpace;
  }
}
