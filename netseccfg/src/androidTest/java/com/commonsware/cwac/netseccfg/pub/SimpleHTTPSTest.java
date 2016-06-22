package com.commonsware.cwac.netseccfg.pub;

import com.commonsware.cwac.netseccfg.AbstractOkHttp3Test;
import com.commonsware.cwac.netseccfg.TrustManagerBuilder;

public class SimpleHTTPSTest extends AbstractOkHttp3Test {
  @Override
  protected String getUrl() {
    return("https://wares.commonsware.com/test.json");
  }

  @Override
  protected TrustManagerBuilder getBuilder() throws Exception {
    return(null);
  }
}
