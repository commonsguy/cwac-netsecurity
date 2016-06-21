package com.commonsware.cwac.netseccfg.test.pub;

import com.commonsware.cwac.netseccfg.TrustManagerBuilder;
import com.commonsware.cwac.netseccfg.test.AbstractHURLTest;

public class SimpleHTTPSTest extends AbstractHURLTest {
  @Override
  protected String getUrl() {
    return("https://wares.commonsware.com/test.json");
  }

  @Override
  protected TrustManagerBuilder getBuilder() {
    return(null);
  }
}
