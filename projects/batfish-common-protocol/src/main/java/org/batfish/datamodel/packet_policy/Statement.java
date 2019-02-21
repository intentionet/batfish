package org.batfish.datamodel.packet_policy;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;

/** A general {@link PacketPolicy} statement */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class")
public interface Statement extends Serializable {
  <T> T accept(PacketPolicyVisitor<T> visitor);
}
