package com.commonsware.cwac.netseccfg.okhttp3.test.pub;

import android.support.test.InstrumentationRegistry;
import com.commonsware.cwac.netseccfg.CertificateNotMemorizedException;
import com.commonsware.cwac.netseccfg.MemorizingTrustManager;
import com.commonsware.cwac.netseccfg.TrustManagerBuilder;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class MemoTOFUTest extends SimpleHTTPSTest {
  private MemorizingTrustManager memo;
  private boolean onNotMemorizedCalled=false;

  @Before
  public void initMemo()
    throws CertificateException, NoSuchAlgorithmException,
    KeyStoreException, IOException {
    MemorizingTrustManager.Options opts=new MemorizingTrustManager.Options(
      InstrumentationRegistry.getContext(), "memo", "sekrit");

    memo=new MemorizingTrustManager(opts.trustOnFirstUse());
  }

  @After
  public void cleanupMemo()
    throws CertificateException, NoSuchAlgorithmException,
    KeyStoreException, IOException {
    memo.clear(true);
  }

  @Override
  public void testRequest() throws Exception {
    super.testRequest();

    /*
      Should not be called, because we trust on first use.
     */

    Assert.assertFalse("onNotMemorized() called", onNotMemorizedCalled);

    /*
      So, let's try again, with a fresh MemorizingTrustManager, to
      confirm that our TOFU was persisted.
     */

    /*
      So now we have the certificate memorized. Create a fresh
      MemorizingTrustManager instance, which should be reading from the
      file. In this case, we expect onNotMemorized() to *not* be called,
      since the cert is already memorized.
     */
    initMemo();
    super.testRequest();
    Assert.assertFalse("onNotMemorized() called", onNotMemorizedCalled);
  }

  @Override
  protected TrustManagerBuilder getBuilder() throws Exception {
    return(new TrustManagerBuilder().useDefault().and().add(memo));
  }

  @Override
  protected void onNotMemorized(CertificateNotMemorizedException e)
    throws Exception {
    throw e;
  }
}
