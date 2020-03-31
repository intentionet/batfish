package org.batfish.specifier.parboiled;

import org.batfish.common.util.isp.IspModelingUtils;
import org.batfish.specifier.InterfaceLinkLocation;

public class InternetLocationAstNode implements LocationAstNode {
  public static final InternetLocationAstNode INSTANCE = new InternetLocationAstNode();

  public static final InterfaceLinkLocation INTERNET_LOCATION =
      new InterfaceLinkLocation(
          IspModelingUtils.INTERNET_HOST_NAME, IspModelingUtils.INTERNET_OUT_INTERFACE);

  @Override
  public <T> T accept(LocationAstNodeVisitor<T> visitor) {
    return visitor.visitInternetLocationAstNode();
  }

  @Override
  public <T> T accept(AstNodeVisitor<T> visitor) {
    return visitor.visitInternetLocationAstNode();
  }
}
