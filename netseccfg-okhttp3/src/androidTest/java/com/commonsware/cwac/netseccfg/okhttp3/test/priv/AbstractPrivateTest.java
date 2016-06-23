package com.commonsware.cwac.netseccfg.okhttp3.test.priv;

import com.commonsware.cwac.netseccfg.BuildConfig;
import com.commonsware.cwac.netseccfg.TrustManagerBuilder;
import com.commonsware.cwac.netseccfg.okhttp3.test.AbstractOkHttp3Test;

abstract public class AbstractPrivateTest extends AbstractOkHttp3Test {
  @Override
  protected String getUrl() {
    return("https://"+BuildConfig.TEST_SERVER_HOST+"/test.json");
  }
}
