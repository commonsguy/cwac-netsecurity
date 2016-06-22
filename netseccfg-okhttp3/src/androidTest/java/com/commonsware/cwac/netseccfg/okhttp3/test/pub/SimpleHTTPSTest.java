package com.commonsware.cwac.netseccfg.okhttp3.test.pub;

import com.commonsware.cwac.netseccfg.okhttp3.test.AbstractOkHttp3Test;
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
