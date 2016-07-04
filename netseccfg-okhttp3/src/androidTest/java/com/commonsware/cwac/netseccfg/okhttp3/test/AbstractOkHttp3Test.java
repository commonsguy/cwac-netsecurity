package com.commonsware.cwac.netseccfg.okhttp3.test;

import android.support.test.runner.AndroidJUnit4;
import com.commonsware.cwac.netseccfg.CertChainListener;
import com.commonsware.cwac.netseccfg.CertificateNotMemorizedException;
import com.commonsware.cwac.netseccfg.OkHttp3Integrator;
import com.commonsware.cwac.netseccfg.TrustManagerBuilder;
import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLHandshakeException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@RunWith(AndroidJUnit4.class)
abstract public class AbstractOkHttp3Test {
  abstract protected String getUrl();
  abstract protected TrustManagerBuilder getBuilder()
    throws Exception;
  private boolean receivedChain=false;

  @Test
  public void testRequest() throws Exception {
    final Request request = new Request.Builder()
      .url(getUrl())
      .build();
    final TrustManagerBuilder tmb=getBuilder();
    final OkHttpClient.Builder builder=new OkHttpClient.Builder();
    boolean hasBuilder=false;

    if (tmb!=null) {
      hasBuilder=true;

      tmb.withCertChainListener(new CertChainListener() {
        @Override
        public void onChain(X509Certificate[] chain,
                            String domain) {
          receivedChain=true;
        }
      });

      OkHttp3Integrator.applyTo(tmb, builder);
    }

    try {
      Response response=builder.build().newCall(request).execute();

      if (!isPositiveTest()) {
        throw new AssertionFailedError("Expected SSLHandshakeException, did not get!");
      }

      Assert.assertEquals(getExpectedResponse(), response.body().string());

      if (hasBuilder) {
        Assert.assertTrue("Received chain", receivedChain);
      }
    }
    catch (SSLHandshakeException e) {
      if (e.getCause() instanceof CertificateNotMemorizedException) {
        onNotMemorized((CertificateNotMemorizedException)e.getCause());

        Response response=builder.build().newCall(request).execute();
        Assert.assertEquals(getExpectedResponse(), response.body().string());
      }
      else {
        if (isPositiveTest()) {
          throw e;
        }
      }
    }
    catch (RuntimeException e) {
      if (isPositiveTest() ||
        !e.getClass().getSimpleName().equals("CleartextAttemptException")) {
        throw e;
      }
    }
  }

  protected String getExpectedResponse() {
    return("{\"Hello\": \"world\"}");
  }

  protected boolean isPositiveTest() {
    return(true);
  }

  protected void onNotMemorized(CertificateNotMemorizedException e)
    throws Exception {
    throw e;
  }
}
