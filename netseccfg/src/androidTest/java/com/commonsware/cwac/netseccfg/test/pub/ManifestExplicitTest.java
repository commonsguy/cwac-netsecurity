package com.commonsware.cwac.netseccfg.test.pub;

import android.support.test.InstrumentationRegistry;
import com.commonsware.cwac.netseccfg.TrustManagerBuilder;

public class ManifestExplicitTest extends SimpleHTTPSTest {
  @Override
  protected TrustManagerBuilder getBuilder() throws Exception {
    return(new TrustManagerBuilder()
      .withManifestConfig(InstrumentationRegistry.getContext()));
  }

  @Override
  protected boolean isPositiveTest() {
    return(false);
  }
}
