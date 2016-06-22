package com.commonsware.cwac.netseccfg.priv;

import com.commonsware.cwac.netseccfg.AbstractHURLTest;
import com.commonsware.cwac.netseccfg.BuildConfig;
import com.commonsware.cwac.netseccfg.TrustManagerBuilder;

public class SimpleHTTPTest extends AbstractHURLTest {
  @Override
  protected String getUrl() {
    return("http://"+BuildConfig.TEST_SERVER_HOST+"/test.json");
  }

  @Override
  protected TrustManagerBuilder getBuilder() {
    return(null);
  }
}
