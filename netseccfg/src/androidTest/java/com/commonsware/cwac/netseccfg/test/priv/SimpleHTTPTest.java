package com.commonsware.cwac.netseccfg.test.priv;

import com.commonsware.cwac.netseccfg.test.AbstractHURLTest;
import com.commonsware.cwac.netseccfg.BuildConfig;
import com.commonsware.cwac.netseccfg.TrustManagerBuilder;

public class SimpleHTTPTest extends AbstractHURLTest {
  @Override
  protected String getUrl() {
    return(BuildConfig.TEST_PRIVATE_HTTP_URL);
  }

  @Override
  protected TrustManagerBuilder getBuilder() {
    return(null);
  }
}
