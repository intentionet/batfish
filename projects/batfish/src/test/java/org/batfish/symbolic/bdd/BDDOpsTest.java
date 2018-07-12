package org.batfish.symbolic.bdd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.JFactory;
import org.junit.Before;
import org.junit.Test;

public class BDDOpsTest {
  private BDDFactory _factory;
  private BDDOps _bddOps;

  @Before
  public void init() {
    _factory = JFactory.init(10000, 1000);
    _factory.disableReorder();
    _factory.setCacheRatio(64);
    _bddOps = new BDDOps(_factory);
  }

  @Test
  public void testAnd_null() {
    assertThat(_bddOps.and(), equalTo(_factory.one()));
    assertThat(_bddOps.and(null, null), equalTo(_factory.one()));
  }

  @Test
  public void testAnd_one() {
    _factory.setVarNum(1);
    BDD var = _factory.ithVar(0);
    assertThat(_bddOps.and(var, _factory.one()), equalTo(var));
  }

  @Test
  public void testAnd_var_varNot() {
    _factory.setVarNum(1);
    BDD var = _factory.ithVar(0);
    assertThat(_bddOps.and(var, var.not()), equalTo(_factory.zero()));
  }

  @Test
  public void testAnd_zero() {
    _factory.setVarNum(1);
    BDD var = _factory.ithVar(0);
    assertThat(_bddOps.and(var, _factory.zero()), equalTo(_factory.zero()));
  }

  @Test
  public void testAndNull_null() {
    assertThat(BDDOps.andNull(), nullValue());
    assertThat(BDDOps.andNull(null, null), nullValue());
  }

  @Test
  public void testAndNull_one() {
    _factory.setVarNum(1);
    BDD var = _factory.ithVar(0);
    assertThat(BDDOps.andNull(var, null), equalTo(var));
  }

  @Test
  public void testAndNull_two() {
    _factory.setVarNum(2);
    BDD var1 = _factory.ithVar(0);
    BDD var2 = _factory.ithVar(1);
    assertThat(BDDOps.andNull(var1, var2, null), equalTo(var1.and(var2)));
  }

  @Test
  public void testOr_one() {
    _factory.setVarNum(1);
    BDD var = _factory.ithVar(0);
    assertThat(_bddOps.or(var, _factory.one()), equalTo(_factory.one()));
  }

  @Test
  public void testOr_null() {
    assertThat(_bddOps.or(), equalTo(_factory.zero()));
    assertThat(_bddOps.or(null, null), equalTo(_factory.zero()));
  }

  @Test
  public void testOr_var_varNot() {
    _factory.setVarNum(1);
    BDD var = _factory.ithVar(0);
    assertThat(_bddOps.or(var, var.not()), equalTo(_factory.one()));
  }

  @Test
  public void testOr_zero() {
    _factory.setVarNum(1);
    BDD var = _factory.ithVar(0);
    assertThat(_bddOps.or(var, _factory.zero()), equalTo(var));
  }

  @Test
  public void testOrNull_null() {
    assertThat(BDDOps.orNull(), nullValue());
    assertThat(BDDOps.orNull(null, null), nullValue());
  }

  @Test
  public void testOrNull_one() {
    _factory.setVarNum(1);
    BDD var = _factory.ithVar(0);
    assertThat(BDDOps.orNull(var, null), equalTo(var));
  }

  @Test
  public void testOrNull_two() {
    _factory.setVarNum(2);
    BDD var1 = _factory.ithVar(0);
    BDD var2 = _factory.ithVar(1);
    assertThat(BDDOps.orNull(null, var1, var2), equalTo(var1.or(var2)));
  }
}
