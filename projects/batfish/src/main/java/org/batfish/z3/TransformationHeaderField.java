package org.batfish.z3;

public enum TransformationHeaderField implements HeaderField {
  NEW_SRC_IP(32, BasicHeaderField.ORIG_SRC_IP, BasicHeaderField.SRC_IP);

  private final BasicHeaderField _current;

  private final BasicHeaderField _original;

  private final int _size;

  private TransformationHeaderField(int size, BasicHeaderField original, BasicHeaderField current) {
    _size = size;
    _original = original;
    _current = current;
  }

  public BasicHeaderField getCurrent() {
    return _current;
  }

  @Override
  public String getName() {
    return name();
  }

  public BasicHeaderField getOriginal() {
    return _original;
  }

  @Override
  public int getSize() {
    return _size;
  }
}
