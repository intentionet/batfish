package org.batfish.main;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import javax.annotation.Nonnull;

/** A {@link Snapshot} represents a combination of a testrig and an environment. */
public class Snapshot {
  /** Returns a new {@link Snapshot} corresponding to the given {@code testrig} and {@code env}. */
  public Snapshot(@Nonnull String testrig, @Nonnull String env) {
    _testrig = testrig;
    _env = env;
  }

  public String getTestrig() {
    return _testrig;
  }

  public String getEnvironment() {
    return _env;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof Snapshot)) {
      return false;
    }
    Snapshot other = (Snapshot) o;
    return _testrig.equals(other._testrig) && _env.equals(other._env);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_testrig, _env);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(Snapshot.class)
        .add("testrig", _testrig)
        .add("env", _env)
        .toString();
  }

  private final String _testrig;
  private final String _env;
}
