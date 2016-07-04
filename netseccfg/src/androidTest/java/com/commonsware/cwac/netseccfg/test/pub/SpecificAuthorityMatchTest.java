package com.commonsware.cwac.netseccfg.test.pub;

import android.support.test.InstrumentationRegistry;
import com.commonsware.cwac.netseccfg.TrustManagerBuilder;
import com.commonsware.cwac.netseccfg.test.R;

public class SpecificAuthorityMatchTest extends SimpleHTTPSTest {
  @Override
  protected TrustManagerBuilder getBuilder() throws Exception {
    return(new TrustManagerBuilder()
      .withConfig(InstrumentationRegistry.getContext(), R.xml.thawte, false));
  }

  @Override
  protected boolean isPositiveTest() {
    return(true);
  }
}
