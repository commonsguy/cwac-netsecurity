package com.commonsware.cwac.netseccfg.okhttp3.test.priv;

import com.commonsware.cwac.netseccfg.okhttp3.test.AbstractOkHttp3Test;
import com.commonsware.cwac.netseccfg.BuildConfig;
import com.commonsware.cwac.netseccfg.TrustManagerBuilder;

public class SimpleHTTPTest extends AbstractOkHttp3Test {
  @Override
  protected String getUrl() {
    return("http://"+BuildConfig.TEST_SERVER_HOST+"/test.json");
  }

  @Override
  protected TrustManagerBuilder getBuilder() {
    return(null);
  }
}
