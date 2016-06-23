package com.commonsware.cwac.netseccfg.okhttp3.test.priv;

import com.commonsware.cwac.netseccfg.BuildConfig;
import com.commonsware.cwac.netseccfg.TrustManagerBuilder;
import com.commonsware.cwac.netseccfg.okhttp3.test.AbstractOkHttp3Test;

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
