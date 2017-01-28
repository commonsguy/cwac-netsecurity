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

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import com.commonsware.cwac.netsecurity.CertificateNotMemorizedException;
import com.commonsware.cwac.netsecurity.MemorizingTrustManager;
import com.commonsware.cwac.netsecurity.TrustManagerBuilder;
import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.net.ssl.SSLHandshakeException;

@RunWith(AndroidJUnit4.class)
public class HURLMemorizationTests {
  private File memoDir=
    new File(InstrumentationRegistry.getTargetContext().getCacheDir(),
      "memo");

  @Before
  public void init() {
    delete(memoDir);
    memoDir.mkdirs();
  }

  @Test
  public void testForNow() throws Exception {
    MemorizingTrustManager memo=new MemorizingTrustManager.Builder()
      .saveTo(memoDir, "sekrit".toCharArray())
      .noTOFU()
      .build();

    final TrustManagerBuilder tmb=new TrustManagerBuilder().add(memo);

    HttpURLConnection c=
      (HttpURLConnection)new URL(getUrl()).openConnection();

    tmb.applyTo(c);

    CertificateNotMemorizedException memoEx;

    try {
      c.getInputStream();

      throw new AssertionFailedError("Expected SSLHandshakeException, did not get!");
    }
    catch (SSLHandshakeException e) {
      if (e.getCause() instanceof CertificateNotMemorizedException) {
        memoEx=(CertificateNotMemorizedException)e.getCause();
      }
      else {
        throw new AssertionFailedError("Expected CertificateNotMemorizedException, did not get!");
      }
    }

    memo.memorizeForNow(memoEx);

    c=(HttpURLConnection)new URL(getUrl()).openConnection();
    tmb.applyTo(c);

    InputStream in=c.getInputStream();

    try {
      Assert.assertEquals(getExpectedResponse(), AbstractHURLTest.slurp(in));
    }
    finally {
      in.close();
    }

    memo.clearAll(false);

    try {
      c=(HttpURLConnection)new URL(getUrl()).openConnection();
      tmb.applyTo(c);
      c.getInputStream();

      throw new AssertionFailedError("Expected SSLHandshakeException, did not get!");
    }
    catch (SSLHandshakeException e) {
      if (!(e.getCause() instanceof CertificateNotMemorizedException)) {
        throw e;
      }
    }
  }

  @Test
  public void testNoTOFU() throws Exception {
    MemorizingTrustManager memo=new MemorizingTrustManager.Builder()
      .saveTo(memoDir, "sekrit".toCharArray())
      .noTOFU()
      .build();

    final TrustManagerBuilder tmb=new TrustManagerBuilder().add(memo);

    HttpURLConnection c=
      (HttpURLConnection)new URL(getUrl()).openConnection();

    tmb.applyTo(c);

    CertificateNotMemorizedException memoEx;

    try {
      c.getInputStream();

      throw new AssertionFailedError("Expected SSLHandshakeException, did not get!");
    }
    catch (SSLHandshakeException e) {
      if (e.getCause() instanceof CertificateNotMemorizedException) {
        memoEx=(CertificateNotMemorizedException)e.getCause();
      }
      else {
        throw new AssertionFailedError("Expected CertificateNotMemorizedException, did not get!");
      }
    }

    memo.memorize(memoEx);

    c=(HttpURLConnection)new URL(getUrl()).openConnection();
    tmb.applyTo(c);

    InputStream in=c.getInputStream();

    try {
      Assert.assertEquals(getExpectedResponse(), AbstractHURLTest.slurp(in));
    }
    finally {
      in.close();
    }

    memo.clearAll(true);

    try {
      c=(HttpURLConnection)new URL(getUrl()).openConnection();
      tmb.applyTo(c);
      c.getInputStream();

      throw new AssertionFailedError("Expected SSLHandshakeException, did not get!");
    }
    catch (SSLHandshakeException e) {
      if (!(e.getCause() instanceof CertificateNotMemorizedException)) {
        throw e;
      }
    }
  }

  @Test
  public void testTOFU() throws Exception {
    MemorizingTrustManager memo=new MemorizingTrustManager.Builder()
      .saveTo(memoDir, "sekrit".toCharArray())
      .build();

    final TrustManagerBuilder tmb=new TrustManagerBuilder().add(memo);

    HttpURLConnection c=
      (HttpURLConnection)new URL(getUrl()).openConnection();

    tmb.applyTo(c);

    InputStream in=c.getInputStream();

    try {
      Assert.assertEquals(getExpectedResponse(), AbstractHURLTest.slurp(in));
    }
    finally {
      in.close();
    }

    c=(HttpURLConnection)new URL(getUrl()).openConnection();
    tmb.applyTo(c);
    in=c.getInputStream();

    try {
      Assert.assertEquals(getExpectedResponse(), AbstractHURLTest.slurp(in));
    }
    finally {
      in.close();
    }

    memo.clearAll(true); // clear TOFU before creating non-TOFU trust manager

    memo=new MemorizingTrustManager.Builder()
      .saveTo(memoDir, "sekrit".toCharArray())
      .noTOFU()
      .build();

    c=(HttpURLConnection)new URL(getUrl()).openConnection();
    new TrustManagerBuilder().add(memo).applyTo(c);

    try {
      c.getInputStream();

      throw new AssertionFailedError("Expected SSLHandshakeException, did not get!");
    }
    catch (SSLHandshakeException e) {
      if (!(e.getCause() instanceof CertificateNotMemorizedException)) {
        throw e;
      }
    }
  }

  private String getExpectedResponse() {
    return("{\"Hello\": \"world\"}");
  }

  protected String getUrl() {
    return(BuildConfig.TEST_PRIVATE_HTTPS_URL);
  }

  // inspired by http://pastebin.com/PqJyzQUx

  /**
   * Recursively deletes a directory and its contents.
   *
   * @param f The directory (or file) to delete
   * @return true if the delete succeeded, false otherwise
   */
  public static boolean delete(File f) {
    if (f.isDirectory()) {
      for (File child : f.listFiles()) {
        if (!delete(child)) {
          return(false);
        }
      }
    }

    return(f.delete());
  }
}
