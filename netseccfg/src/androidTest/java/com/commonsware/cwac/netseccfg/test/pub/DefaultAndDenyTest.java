package com.commonsware.cwac.netseccfg.test.pub;

import com.commonsware.cwac.netseccfg.TrustManagerBuilder;

public class DefaultAndDenyTest extends SimpleHTTPSTest {
  @Override
  protected TrustManagerBuilder getBuilder() throws Exception {
    return(new TrustManagerBuilder().useDefault().and().denyAll());
  }

  @Override
  protected boolean isPositiveTest() {
    return(false);
  }
}
