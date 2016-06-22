package com.commonsware.cwac.netseccfg.pub;

import com.commonsware.cwac.netseccfg.TrustManagerBuilder;

public class DenyOrDefaultTest extends SimpleHTTPSTest {
  @Override
  protected TrustManagerBuilder getBuilder() throws Exception {
    return(new TrustManagerBuilder().denyAll().or().useDefault());
  }
}
