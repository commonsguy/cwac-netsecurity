package com.commonsware.cwac.netseccfg.okhttp3.test.priv;

import com.commonsware.cwac.netseccfg.okhttp3.test.BuildConfig;
import com.commonsware.cwac.netseccfg.okhttp3.test.AbstractOkHttp3Test;

abstract public class AbstractPrivateTest extends AbstractOkHttp3Test {
  @Override
  protected String getUrl() {
    return(BuildConfig.TEST_PRIVATE_HTTPS_URL);
  }
}
