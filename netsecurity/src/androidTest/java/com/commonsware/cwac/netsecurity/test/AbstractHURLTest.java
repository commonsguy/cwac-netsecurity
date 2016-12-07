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
import com.commonsware.cwac.netsecurity.TrustManagerBuilder;
import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;

@RunWith(AndroidJUnit4.class)
abstract public class AbstractHURLTest {
  abstract protected String getUrl();
  abstract protected TrustManagerBuilder getBuilder()
    throws Exception;
  private boolean receivedChain=false;

  @Test
  public void testRequest() throws Exception {
    if (isExceptionExpectedForBuilder()) {
      try {
        TrustManagerBuilder builder=getBuilder();
        Assert.fail("expected exception, did not get one");
      }
      catch (Exception e) {
        // life is good
      }
    }
    else {
      HttpURLConnection c=
        (HttpURLConnection)new URL(getUrl()).openConnection();
      boolean hasBuilder=false;

      if (c instanceof HttpsURLConnection) {
        TrustManagerBuilder builder=getBuilder();

        if (builder!=null) {
          hasBuilder=true;

          builder.withCertChainListener(new CertChainListener() {
            @Override
            public void onChain(X509Certificate[] chain,
                                String domain) {
              receivedChain=true;
            }
          }).applyTo(c);
        }
      }

      try {
        InputStream in=c.getInputStream();

        try {
          if (!isPositiveTest()) {
            throw new AssertionFailedError(
              "Expected SSLHandshakeException, did not get!");
          }

          Assert.assertEquals(getExpectedResponse(), slurp(in));

          if (hasBuilder) {
            Assert.assertTrue("Received chain", receivedChain);
          }
        }
        finally {
          in.close();
        }
      }
      catch (SSLHandshakeException e) {
        if (isPositiveTest()) {
          throw e;
        }
      }
    }
  }

  protected String getExpectedResponse() {
    return("{\"Hello\": \"world\"}");
  }

  protected boolean isPositiveTest() {
    return(true);
  }

  protected boolean isExceptionExpectedForBuilder() {
    return(false);
  }

  // based on http://stackoverflow.com/a/309718/115145

  public static String slurp(final InputStream is)
    throws IOException {
    final char[] buffer = new char[128];
    final StringBuilder out = new StringBuilder();
    final Reader in = new InputStreamReader(is, "UTF-8");

    for (;;) {
      int rsz = in.read(buffer, 0, buffer.length);
      if (rsz < 0)
        break;
      out.append(buffer, 0, rsz);
    }

    return out.toString();
  }
}
