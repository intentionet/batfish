package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
public interface JacksonSerializableIpSpace extends IpSpace {
  IpSpace unwrap();
}
