package com.commonsware.cwac.netseccfg.test.pub;

import android.support.test.InstrumentationRegistry;
import com.commonsware.cwac.netseccfg.TrustManagerBuilder;
import com.commonsware.cwac.netseccfg.test.R;

public class SpecificAuthorityMismatchTest extends SimpleHTTPSTest {
  @Override
  protected TrustManagerBuilder getBuilder() throws Exception {
    return(new TrustManagerBuilder().withConfig(
      InstrumentationRegistry.getContext(), R.xml.verisign, false));
  }

  @Override
  protected boolean isPositiveTest() {
    return(false);
  }
}
