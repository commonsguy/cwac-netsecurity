package com.commonsware.cwac.netseccfg.okhttp3.test.pub;

import com.commonsware.cwac.netseccfg.TrustManagerBuilder;

public class DenyAllTest extends
  com.commonsware.cwac.netseccfg.test.pub.SimpleHTTPSTest {
  @Override
  protected TrustManagerBuilder getBuilder() throws Exception {
    return(new TrustManagerBuilder().denyAll());
  }

  @Override
  protected boolean isPositiveTest() {
    return(false);
  }
}
