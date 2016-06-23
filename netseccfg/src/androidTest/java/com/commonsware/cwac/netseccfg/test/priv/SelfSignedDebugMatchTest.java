package com.commonsware.cwac.netseccfg.test.priv;

import android.support.test.InstrumentationRegistry;
import com.commonsware.cwac.netseccfg.TrustManagerBuilder;
import com.commonsware.cwac.netseccfg.test.R;

public class SelfSignedDebugMatchTest extends AbstractPrivateTest {
  @Override
  protected TrustManagerBuilder getBuilder() throws Exception {
    return(new TrustManagerBuilder().withConfig(
      InstrumentationRegistry.getContext(), R.xml.selfsigned_debug,
      true));
  }
}
