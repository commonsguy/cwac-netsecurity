package com.commonsware.cwac.netseccfg.okhttp3.test.priv;

import android.support.test.InstrumentationRegistry;
import com.commonsware.cwac.netseccfg.BuildConfig;
import com.commonsware.cwac.netseccfg.TrustManagerBuilder;
import com.commonsware.cwac.netseccfg.okhttp3.test.AbstractOkHttp3Test;
import com.commonsware.cwac.netseccfg.okhttp3.test.R;

public class NoCleartextTest extends AbstractOkHttp3Test {
  @Override
  protected String getUrl() {
    return("http://"+BuildConfig.TEST_SERVER_HOST+"/test.json");
  }

  @Override
  protected TrustManagerBuilder getBuilder() {
    return(new TrustManagerBuilder().withConfig(
      InstrumentationRegistry.getContext(), R.xml.selfsigned_noclear,
      true));
  }

  @Override
  protected boolean isPositiveTest() {
    return(false);
  }
}
