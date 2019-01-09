package org.batfish.datamodel.matchers;

import javax.annotation.Nullable;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Mlag;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

final class MlagMatchersImpl {

  static final class HasId extends FeatureMatcher<Mlag, String> {
    HasId(String name, Matcher<? super String> subMatcher) {
      super(subMatcher, "An MLAG Configuration with id " + name + ":", "id " + name);
    }

    @Override
    protected String featureValueOf(Mlag mlag) {
      return mlag.getId();
    }
  }

  static final class HasPeerAddress extends FeatureMatcher<Mlag, Ip> {
    HasPeerAddress(Ip ip, Matcher<? super Ip> subMatcher) {
      super(subMatcher, "An MLAG Configuration with peer address " + ip + ":", "peerAddress " + ip);
    }

    @Override
    @Nullable
    protected Ip featureValueOf(Mlag mlag) {
      return mlag.getPeerAddress();
    }
  }

  static final class HasPeerInterface extends FeatureMatcher<Mlag, String> {
    HasPeerInterface(String name, Matcher<? super String> subMatcher) {
      super(
          subMatcher,
          "An MLAG Configuration with peer interface " + name + ":",
          "peerInterface" + name);
    }

    @Nullable
    @Override
    protected String featureValueOf(Mlag mlag) {
      return mlag.getPeerInterface();
    }
  }

  static final class HasLocalInterface extends FeatureMatcher<Mlag, String> {
    HasLocalInterface(String name, Matcher<? super String> subMatcher) {
      super(
          subMatcher,
          "An MLAG Configuration with local interface " + name + ":",
          "localInterface" + name);
    }

    @Nullable
    @Override
    protected String featureValueOf(Mlag mlag) {
      return mlag.getLocalInterface();
    }
  }

  // Prevent initialization
  private MlagMatchersImpl() {}
}
