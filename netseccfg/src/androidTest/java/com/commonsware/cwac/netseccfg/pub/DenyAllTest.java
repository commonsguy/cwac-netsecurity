package com.commonsware.cwac.netseccfg.pub;

import com.commonsware.cwac.netseccfg.TrustManagerBuilder;

public class DenyAllTest extends SimpleHTTPSTest {
  @Override
  protected TrustManagerBuilder getBuilder() throws Exception {
    return(new TrustManagerBuilder().denyAll());
  }

  @Override
  protected boolean isPositiveTest() {
    return(false);
  }
}
