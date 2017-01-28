/***
 Copyright (c) 2016 CommonsWare, LLC
 Licensed under the Apache License, Version 2.0 (the "License"); you may not
 use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
 by applicable law or agreed to in writing, software distributed under the
 License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 OF ANY KIND, either express or implied. See the License for the specific
 language governing permissions and limitations under the License.
 */

package com.commonsware.cwac.netsecurity.test;

import android.support.test.runner.AndroidJUnit4;
import com.commonsware.cwac.netsecurity.CertChainListener;
import com.commonsware.cwac.netsecurity.CertificateNotMemorizedException;
import com.commonsware.cwac.netsecurity.OkHttp3Integrator;
import com.commonsware.cwac.netsecurity.TrustManagerBuilder;
import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import org.junit.Test;
import org.junit.runner.RunWith;
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
      if (isPositiveTest()) {
        throw e;
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
}
