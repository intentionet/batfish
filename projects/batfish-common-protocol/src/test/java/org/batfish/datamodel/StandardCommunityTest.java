package org.batfish.datamodel;

import static org.batfish.datamodel.StandardCommunity.literalCommunityValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public final class StandardCommunityTest {

  @Test
  public void testLiteralCommunityValueInvalid() {
    String invalidLeft1 = "-1:1";
    String invalidLeft2 = "65536:1";
    String invalidRight1 = "1:-1";
    String invalidRight2 = "1:65536";
    String invalidSegments = "1:2:3";
    String invalidNonDigit = "1:A";
    
    assertThat(literalCommunityValue(invalidLeft1), nullValue());
    assertThat(literalCommunityValue(invalidLeft2), nullValue());
    assertThat(literalCommunityValue(invalidRight1), nullValue());
    assertThat(literalCommunityValue(invalidRight2), nullValue());
    assertThat(literalCommunityValue(invalidSegments), nullValue());
    assertThat(literalCommunityValue(invalidNonDigit), nullValue());
  }

  @Test
  public void testLiteralCommunityValid() {
    assertThat(literalCommunityValue("0:1"), equalTo(1L));
    assertThat(literalCommunityValue("1:0"), equalTo(0x10000L));
    assertThat(literalCommunityValue("65535:65535"), equalTo(0xFFFFFFFFL));
  }
  
}
