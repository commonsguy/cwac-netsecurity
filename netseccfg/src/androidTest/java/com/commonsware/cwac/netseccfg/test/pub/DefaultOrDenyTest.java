package com.commonsware.cwac.netseccfg.test.pub;

import com.commonsware.cwac.netseccfg.TrustManagerBuilder;

public class DefaultOrDenyTest extends SimpleHTTPSTest {
  @Override
  protected TrustManagerBuilder getBuilder() throws Exception {
    return(new TrustManagerBuilder().useDefault().or().denyAll());
  }
}
