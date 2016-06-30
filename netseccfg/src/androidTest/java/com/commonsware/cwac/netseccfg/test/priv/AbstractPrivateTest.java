package com.commonsware.cwac.netseccfg.test.priv;

import com.commonsware.cwac.netseccfg.BuildConfig;
import com.commonsware.cwac.netseccfg.test.AbstractHURLTest;

abstract public class AbstractPrivateTest extends AbstractHURLTest {
  @Override
  protected String getUrl() {
    return(BuildConfig.TEST_PRIVATE_HTTPS_URL);
  }
}
