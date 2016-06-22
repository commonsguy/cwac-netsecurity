package com.commonsware.cwac.netseccfg;

import android.support.test.runner.AndroidJUnit4;
import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.IOException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@RunWith(AndroidJUnit4.class)
abstract public class AbstractOkHttp3Test {
  abstract protected String getUrl();
  abstract protected TrustManagerBuilder getBuilder()
    throws Exception;

  @Test
  public void testRequest() throws Exception {
    final Request request = new Request.Builder()
      .url(getUrl())
      .build();
    final TrustManagerBuilder tmb=getBuilder();
    final OkHttpClient.Builder builder=new OkHttpClient.Builder();

    if (tmb!=null) {
      SSLContext ssl=SSLContext.getInstance("TLS");
      CompositeTrustManager trustManager=tmb.build();

      ssl.init(null, new TrustManager[] { trustManager }, null);
      builder.sslSocketFactory(ssl.getSocketFactory(), trustManager);

      X509Interceptor interceptor=new X509Interceptor(trustManager);

      builder.addInterceptor(interceptor);
      builder.addNetworkInterceptor(interceptor);
    }

    try {
      Response response=builder.build().newCall(request).execute();

      if (!isPositiveTest()) {
        throw new AssertionFailedError("Expected SSLHandshakeException, did not get!");
      }

      Assert.assertEquals(getExpectedResponse(), response.body().string());
    }
    catch (SSLHandshakeException e) {
      if (isPositiveTest()) {
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

  static class X509Interceptor implements Interceptor {
    private final CompositeTrustManager trustManager;

    public X509Interceptor(
      CompositeTrustManager trustManager) {
      this.trustManager=trustManager;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
      Request request=chain.request();

      trustManager.setHost(request.url().host());

      return(chain.proceed(request));
    }
  }
}
