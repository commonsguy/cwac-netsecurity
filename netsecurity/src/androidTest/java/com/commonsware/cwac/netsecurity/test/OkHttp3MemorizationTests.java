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
import com.commonsware.cwac.netsecurity.OkHttp3Integrator;
import com.commonsware.cwac.netsecurity.TrustManagerBuilder;
import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import javax.net.ssl.SSLHandshakeException;
import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import static com.commonsware.cwac.netsecurity.DomainMatchRule.is;

@RunWith(AndroidJUnit4.class)
public class OkHttp3MemorizationTests {
  private final OkHttpClient.Builder builder=new OkHttpClient.Builder();
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

    OkHttp3Integrator.applyTo(tmb, builder);
    OkHttpClient client=builder.build();
    CertificateNotMemorizedException memoEx;

    try {
      client.newCall(buildRequest()).execute();

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

    Response response=client.newCall(buildRequest()).execute();
    Assert.assertEquals(getExpectedResponse(), response.body().string());

    OkHttpClient.Builder freshBuilder=new OkHttpClient.Builder();
    OkHttp3Integrator.applyTo(tmb, freshBuilder);

    client=freshBuilder.build();
    memo.clearAll(false);

    try {
      client.newCall(buildRequest()).execute();

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

    OkHttp3Integrator.applyTo(tmb, builder);
    OkHttpClient client=builder.build();
    CertificateNotMemorizedException memoEx;

    try {
      client.newCall(buildRequest()).execute();

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

    Response response=client.newCall(buildRequest()).execute();
    Assert.assertEquals(getExpectedResponse(), response.body().string());

    OkHttpClient.Builder freshBuilder=new OkHttpClient.Builder();
    OkHttp3Integrator.applyTo(tmb, freshBuilder);

    client=freshBuilder.build();
    memo.clearAll(true);

    try {
      client.newCall(buildRequest()).execute();

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

    OkHttp3Integrator.applyTo(tmb, builder);
    OkHttpClient client=builder.build();
    Response response=client.newCall(buildRequest()).execute();
    Assert.assertEquals(getExpectedResponse(), response.body().string());

    response=client.newCall(buildRequest()).execute();
    Assert.assertEquals(getExpectedResponse(), response.body().string());

    MemorizingTrustManager memoNoTofu=new MemorizingTrustManager.Builder()
      .saveTo(memoDir, "sekrit".toCharArray())
      .noTOFU()
      .build();
    TrustManagerBuilder tmbNoTofu=new TrustManagerBuilder().add(memoNoTofu);
    OkHttpClient.Builder builderNoTofu=new OkHttpClient.Builder();

    OkHttp3Integrator.applyTo(tmbNoTofu, builderNoTofu);

    OkHttpClient clientNoTofu=builderNoTofu.build();

    response=clientNoTofu.newCall(buildRequest()).execute();
    Assert.assertEquals(getExpectedResponse(), response.body().string());

    memoNoTofu.clearAll(true);
    builderNoTofu=new OkHttpClient.Builder();
    OkHttp3Integrator.applyTo(tmbNoTofu, builderNoTofu);
    clientNoTofu=builderNoTofu.build();

    try {
      clientNoTofu.newCall(buildRequest()).execute();

      throw new AssertionFailedError("Expected SSLHandshakeException, did not get!");
    }
    catch (SSLHandshakeException e) {
      if (!(e.getCause() instanceof CertificateNotMemorizedException)) {
        throw e;
      }
    }
  }

  @Test
  public void testDomainMatchRule() throws Exception {
    MemorizingTrustManager memo=new MemorizingTrustManager.Builder()
      .saveTo(memoDir, "sekrit".toCharArray())
      .noTOFU()
      .forDomains(is("this-so-does-not-exist.com"))
      .build();

    final TrustManagerBuilder tmb=new TrustManagerBuilder().add(memo);

    OkHttp3Integrator.applyTo(tmb, builder);
    OkHttpClient client=builder.build();

    Response response=client.newCall(buildRequest()).execute();
    Assert.assertEquals(getExpectedResponse(), response.body().string());
  }

  @Test
  public void testOr() throws Exception {
    MemorizingTrustManager memo=new MemorizingTrustManager.Builder()
      .saveTo(memoDir, "sekrit".toCharArray())
      .noTOFU()
      .build();

    final TrustManagerBuilder tmb=new TrustManagerBuilder()
      .withConfig(InstrumentationRegistry.getContext(),
        R.xml.okhttp3_selfsigned_debug, false)
      .or()
      .add(memo);

    OkHttp3Integrator.applyTo(tmb, builder);
    OkHttpClient client=builder.build();
    CertificateNotMemorizedException memoEx;

    try {
      client.newCall(buildRequest()).execute();

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

    Response response=client.newCall(buildRequest()).execute();
    Assert.assertEquals(getExpectedResponse(), response.body().string());
  }

  @Test
  public void testAnd() throws Exception {
    MemorizingTrustManager memo=new MemorizingTrustManager.Builder()
      .saveTo(memoDir, "sekrit".toCharArray())
      .noTOFU()
      .build();

    final TrustManagerBuilder tmb=new TrustManagerBuilder()
      .withConfig(InstrumentationRegistry.getContext(),
        R.xml.okhttp3_selfsigned_debug, true)
      .and()
      .add(memo);

    OkHttp3Integrator.applyTo(tmb, builder);
    OkHttpClient client=builder.build();
    CertificateNotMemorizedException memoEx;

    try {
      client.newCall(buildRequest()).execute();

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

    Response response=client.newCall(buildRequest()).execute();
    Assert.assertEquals(getExpectedResponse(), response.body().string());
  }

  private Request buildRequest() {
    return(new Request.Builder()
      .url(getUrl())
      .cacheControl(CacheControl.FORCE_NETWORK)
      .build());
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
