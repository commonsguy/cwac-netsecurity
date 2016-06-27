package com.commonsware.cwac.netseccfg.test.pub;

import android.os.Build;
import com.commonsware.cwac.netseccfg.TrustManagerBuilder;

public class ManifestDefaultTest extends SimpleHTTPSTest {
  @Override
  protected TrustManagerBuilder getBuilder() throws Exception {
    return(null);
  }

  // if <N, succeeds because we are not applying any rules
  // if >=N, fails because manifest setting is for invalid CA
  @Override
  protected boolean isPositiveTest() {
    return(Build.VERSION.SDK_INT<Build.VERSION_CODES.N);
  }
}
