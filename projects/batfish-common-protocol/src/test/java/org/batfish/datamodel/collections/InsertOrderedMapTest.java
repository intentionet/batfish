package org.batfish.datamodel.collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.testing.EqualsTester;
import java.util.AbstractMap.SimpleEntry;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.Before;
import org.junit.Test;

/** Tests of {@link InsertOrderedMap} */
public class InsertOrderedMapTest {

  private InsertOrderedMap<String, String> _testMap;

  @Before
  public void setUp() {
    _testMap = new InsertOrderedMap<>();
  }

  @Test
  public void testIsEmptyOnCreation() {
    assertTrue(_testMap.isEmpty());
  }

  @Test
  public void testPut() {
    _testMap.put("key", "value");
    assertThat(_testMap.get("key"), equalTo("value"));
  }

  @Test
  public void testPutAsReplace() {
    _testMap.put("key", "value");
    _testMap.put("key2", "value2");
    _testMap.put("key", "newValue");
    // Expect new value at key; key moves to the end of the map.
    assertThat(_testMap.get("key"), equalTo("newValue"));
    assertThat(_testMap.keySet(), contains("key2", "key"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPutRejectsNullKey() {
    _testMap.put(null, "value");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPutRejectsNullValue() {
    _testMap.put("key", null);
  }

  @Test
  public void testPutAll() {
    _testMap.putAll(ImmutableSortedMap.of("a", "1", "b", "2", "c", "3"));
    assertThat(_testMap.keySet(), contains("a", "b", "c"));
  }

  @Test
  public void testRemove() {
    String key = "key";
    _testMap.put(key, "value");
    _testMap.remove(key);
    assertTrue(_testMap.isEmpty());
    // removing again should simply return null.
    assertThat(_testMap.remove(key), nullValue());
  }

  @Test
  public void testClear() {
    _testMap.put("key", "value");
    _testMap.clear();
    assertTrue(_testMap.isEmpty());
  }

  @Test
  public void tetKeySetIterationOrder() {
    _testMap.put("first", "1");
    _testMap.put("second", "2");
    _testMap.put("last", "3");
    assertThat(_testMap.keySet(), contains("first", "second", "last"));
  }

  @Test
  public void testValuesIterationOrder() {
    _testMap.put("first", "1");
    _testMap.put("second", "2");
    _testMap.put("last", "3");
    assertThat(_testMap.values(), contains("1", "2", "3"));
  }

  @Test
  public void tetEntrySetIterationOrder() {
    _testMap.put("first", "1");
    _testMap.put("second", "2");
    _testMap.put("last", "3");
    assertThat(
        _testMap.entrySet(),
        contains(
            new SimpleEntry<>("first", "1"),
            new SimpleEntry<>("second", "2"),
            new SimpleEntry<>("last", "3")));
  }

  @Test
  public void testPutFirst() {
    _testMap.put("key", "value");
    _testMap.putFirst("first", "1");
    assertThat(_testMap.keySet(), contains("first", "key"));
  }

  @Test
  public void testMoveBefore() {
    _testMap.put("key1", "value");
    _testMap.put("key2", "value2");
    _testMap.moveBefore("key2", "key1");
    assertThat(_testMap.keySet(), contains("key2", "key1"));
  }

  @Test
  public void testMoveAfter() {
    _testMap.put("key1", "value");
    _testMap.put("key2", "value");
    _testMap.moveAfter("key1", "key2");
    assertThat(_testMap.keySet(), contains("key2", "key1"));
  }

  @Test
  public void testEquals() {
    _testMap.put("first", "val1");
    _testMap.put("second", "val2");
    InsertOrderedMap<String, String> shouldEqual = new InsertOrderedMap<>();
    shouldEqual.put("first", "val1");
    shouldEqual.put("second", "val2");
    InsertOrderedMap<String, String> m1 = new InsertOrderedMap<>();
    m1.put("first", "val1");
    InsertOrderedMap<String, String> m2 = new InsertOrderedMap<>();
    m2.put("second", "val2");
    m2.put("first", "val1");
    new EqualsTester()
        .addEqualityGroup(_testMap, _testMap, shouldEqual)
        .addEqualityGroup(m1)
        .addEqualityGroup(m2)
        .addEqualityGroup(new Object())
        .testEquals();
  }

  @Test
  public void testJavaSerialization() {
    _testMap.put("key1", "value");
    assertThat(SerializationUtils.clone(_testMap), equalTo(_testMap));
  }
}
