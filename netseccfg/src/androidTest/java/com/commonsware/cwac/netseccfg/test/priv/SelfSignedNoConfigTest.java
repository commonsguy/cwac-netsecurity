package com.commonsware.cwac.netseccfg.test.priv;

import com.commonsware.cwac.netseccfg.TrustManagerBuilder;

public class SelfSignedNoConfigTest extends AbstractPrivateTest {
  @Override
  protected TrustManagerBuilder getBuilder() {
    return(null);
  }

  @Override
  protected boolean isPositiveTest() {
    return(false);
  }
}
