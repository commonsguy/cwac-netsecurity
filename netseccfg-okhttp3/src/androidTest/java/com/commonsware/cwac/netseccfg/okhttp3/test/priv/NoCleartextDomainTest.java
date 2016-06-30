package com.commonsware.cwac.netseccfg.okhttp3.test.priv;

import android.support.test.InstrumentationRegistry;
import com.commonsware.cwac.netseccfg.BuildConfig;
import com.commonsware.cwac.netseccfg.TrustManagerBuilder;
import com.commonsware.cwac.netseccfg.okhttp3.test.AbstractOkHttp3Test;
import com.commonsware.cwac.netseccfg.okhttp3.test.R;

public class NoCleartextDomainTest extends AbstractOkHttp3Test {
  @Override
  protected String getUrl() {
    return(BuildConfig.TEST_PRIVATE_HTTP_URL);
  }

  @Override
  protected TrustManagerBuilder getBuilder() {
    return(new TrustManagerBuilder().withConfig(
      InstrumentationRegistry.getContext(), R.xml.selfsigned_noclear_domain,
      true));
  }

  @Override
  protected boolean isPositiveTest() {
    return(false);
  }
}
