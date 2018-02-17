package org.batfish.z3;

import org.batfish.common.BatfishException;

public interface HeaderField {

  static HeaderField parse(String name) {
    HeaderField val = BasicHeaderField.valueOf(name);
    if (val != null) {
      return val;
    }
    val = TransformationHeaderField.valueOf(name);
    if (val != null) {
      return val;
    }
    throw new BatfishException(
        String.format("No %s with name: %s", HeaderField.class.getSimpleName(), name));
  }

  String getName();

  int getSize();
}
