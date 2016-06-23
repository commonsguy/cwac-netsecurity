package com.commonsware.cwac.netseccfg.test.priv;

import android.support.test.InstrumentationRegistry;
import com.commonsware.cwac.netseccfg.TrustManagerBuilder;
import com.commonsware.cwac.netseccfg.test.R;

public class SelfSignedDebugReleaseTest extends AbstractPrivateTest {
  @Override
  protected TrustManagerBuilder getBuilder() throws Exception {
    return(new TrustManagerBuilder().withConfig(
      InstrumentationRegistry.getContext(), R.xml.selfsigned_debug,
      false));
  }

  @Override
  protected boolean isPositiveTest() {
    return(false);
  }
}
