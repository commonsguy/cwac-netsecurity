package com.commonsware.cwac.netseccfg.okhttp3.test.priv;

import android.support.test.InstrumentationRegistry;
import com.commonsware.cwac.netseccfg.BuildConfig;
import com.commonsware.cwac.netseccfg.TrustManagerBuilder;
import com.commonsware.cwac.netseccfg.okhttp3.test.R;
import com.commonsware.cwac.netseccfg.okhttp3.test.pub.SimpleHTTPSTest;

public class RedirMatchTest extends SimpleHTTPSTest {
  @Override
  protected String getUrl() {
    return(BuildConfig.TEST_PRIVATE_HTTP_REDIR_URL);
  }

  @Override
  protected TrustManagerBuilder getBuilder() throws Exception {
    return(new TrustManagerBuilder().withConfig(
      InstrumentationRegistry.getContext(), R.xml.selfsigned, false));
  }
}
