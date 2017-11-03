package org.batfish.common;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.Sets;
import com.google.common.testing.EqualsTester;
import java.util.Collections;
import java.util.TreeSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link Container}. */
@RunWith(JUnit4.class)
public class ContainerTest {
  @Test
  public void testToString() {
    Container c = Container.of("foo", new TreeSet<>());
    assertThat(c.toString(), equalTo("Container{name=foo, testrigs=[]}"));
  }

  @Test
  public void testEquals() {
    Container c = Container.of("foo", new TreeSet<>());
    Container cCopy = Container.of("foo", new TreeSet<>());
    Container cWithTestrig =
        Container.of("foo", Sets.newTreeSet(Collections.singletonList("testrig")));
    Container cOtherName = Container.of("bar", new TreeSet<>());

    new EqualsTester()
        .addEqualityGroup(c, cCopy)
        .addEqualityGroup(cWithTestrig)
        .addEqualityGroup(cOtherName)
        .testEquals();
  }
}
