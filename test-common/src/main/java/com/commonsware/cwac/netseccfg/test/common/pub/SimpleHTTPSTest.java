package com.commonsware.cwac.netseccfg.test.common.pub;

import com.commonsware.cwac.netseccfg.test.common.AbstractHURLTest;

public class SimpleHTTPSTest extends AbstractHURLTest {
  @Override
  protected String getUrl() {
    return("https://wares.commonsware.com/test.json");
  }

  @Override
  protected String getExpectedResponse() {
    return("{\"Hello\": \"world\"}");
  }
}
