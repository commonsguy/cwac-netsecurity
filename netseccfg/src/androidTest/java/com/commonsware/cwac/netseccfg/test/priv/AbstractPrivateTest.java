package com.commonsware.cwac.netseccfg.test.priv;

import com.commonsware.cwac.netseccfg.BuildConfig;
import com.commonsware.cwac.netseccfg.test.AbstractHURLTest;

abstract public class AbstractPrivateTest extends AbstractHURLTest {
  @Override
  protected String getUrl() {
    return("https://"+BuildConfig.TEST_SERVER_HOST+"/test.json");
  }
}
