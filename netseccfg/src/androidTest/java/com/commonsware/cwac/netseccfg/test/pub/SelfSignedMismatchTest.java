package com.commonsware.cwac.netseccfg.test.pub;

import android.support.test.InstrumentationRegistry;
import com.commonsware.cwac.netseccfg.TrustManagerBuilder;
import com.commonsware.cwac.netseccfg.test.R;

public class SelfSignedMismatchTest extends SimpleHTTPSTest {
  @Override
  protected TrustManagerBuilder getBuilder() throws Exception {
    return(new TrustManagerBuilder().withConfig(
      InstrumentationRegistry.getContext(), R.xml.selfsigned, false));
  }

  @Override
  protected boolean isPositiveTest() {
    return(false);
  }
}
