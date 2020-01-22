package org.batfish.representation.juniper;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import org.batfish.datamodel.ExprAclLine;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.TraceElement;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.representation.juniper.BaseApplication.Term;
import org.junit.Test;

public class FwFromApplicationOrApplicationSetTest {

  @Test
  public void testApplyTo_application() {
    JuniperConfiguration jc = new JuniperConfiguration();

    BaseApplication app = new BaseApplication("app");
    app.getTerms()
        .putAll(
            ImmutableMap.of(
                "t1", new Term("t1"),
                "t2", new Term("t2")));

    jc.getMasterLogicalSystem().getApplications().put("app", app);

    FwFromApplicationOrApplicationSet from = new FwFromApplicationOrApplicationSet("app");

    List<ExprAclLine> lines = new ArrayList<>();
    HeaderSpace.Builder hsBuilder = HeaderSpace.builder();

    from.applyTo(jc, hsBuilder, LineAction.PERMIT, lines, null);

    assertThat(
        lines,
        equalTo(
            ImmutableList.of(
                new ExprAclLine(
                    LineAction.PERMIT,
                    new MatchHeaderSpace(HeaderSpace.builder().build()),
                    null,
                    TraceElement.of("Matched application app term t1")),
                new ExprAclLine(
                    LineAction.PERMIT,
                    new MatchHeaderSpace(HeaderSpace.builder().build()),
                    null,
                    TraceElement.of("Matched application app term t2")))));
  }

  @Test
  public void testApplyTo_applicationSet() {
    JuniperConfiguration jc = new JuniperConfiguration();

    ApplicationSet appSet = new ApplicationSet();
    appSet.setMembers(
        ImmutableList.of(new ApplicationReference("app1"), new ApplicationReference("app2")));
    jc.getMasterLogicalSystem().getApplicationSets().put("appSet", appSet);

    jc.getMasterLogicalSystem()
        .getApplications()
        .putAll(
            ImmutableMap.of(
                "app1", new BaseApplication("app1"), "app2", new BaseApplication("app2")));

    FwFromApplicationOrApplicationSet from = new FwFromApplicationOrApplicationSet("appSet");

    List<ExprAclLine> lines = new ArrayList<>();
    HeaderSpace.Builder hsBuilder = HeaderSpace.builder();

    from.applyTo(jc, hsBuilder, LineAction.PERMIT, lines, null);

    assertThat(
        lines,
        equalTo(
            ImmutableList.of(
                new ExprAclLine(
                    LineAction.PERMIT,
                    new MatchHeaderSpace(HeaderSpace.builder().build()),
                    null,
                    TraceElement.of("Matched application app1")),
                new ExprAclLine(
                    LineAction.PERMIT,
                    new MatchHeaderSpace(HeaderSpace.builder().build()),
                    null,
                    TraceElement.of("Matched application app2")))));
  }
}
