package com.commonsware.cwac.netseccfg.test.pub;

import android.os.Build;
import com.commonsware.cwac.netseccfg.TrustManagerBuilder;
import com.commonsware.cwac.netseccfg.test.AbstractHURLTest;

public class SimpleHTTPSTest extends AbstractHURLTest {
  @Override
  protected String getUrl() {
    return("https://wares.commonsware.com/test.json");
  }

  @Override
  protected TrustManagerBuilder getBuilder() throws Exception {
    return(null);
  }

  // on N+, fail because of manifest config
  @Override
  protected boolean isPositiveTest() {
    return(Build.VERSION.SDK_INT<Build.VERSION_CODES.N);
  }
}
