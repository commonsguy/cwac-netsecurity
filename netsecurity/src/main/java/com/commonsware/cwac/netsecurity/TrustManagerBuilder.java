/***
  Copyright (c) 2014-2016 CommonsWare, LLC
  
  Licensed under the Apache License, Version 2.0 (the "License"); you may
  not use this file except in compliance with the License. You may obtain
  a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package com.commonsware.cwac.netsecurity;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.XmlRes;
import com.commonsware.cwac.netsecurity.config.ApplicationConfig;
import com.commonsware.cwac.netsecurity.config.ConfigSource;
import com.commonsware.cwac.netsecurity.config.ManifestConfigSource;
import com.commonsware.cwac.netsecurity.config.XmlConfigSource;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Class for building TrustManager instances for use with
 * HttpsURLConnection, OkHttp3, and kin.
 * 
 * This class has a builder-style fluent interface. Create
 * an instance, and you can call various methods on it,
 * chained one after the next, as most methods return the
 * builder itself.
 * 
 * The end of the chained method calls should be build() (to
 * create a single TrustManager representing what you want)
 * or buildArray() (a convenience method to wrap that single
 * TrustManager in a TrustManager[], which many APIs
 * require). Or, if you are using HttpURLConnection, call
 * applyTo() to apply the contents of the builder to that
 * connection.
 *
 * If you are using OkHttp3, use the netseccfg-okhttp3 artifact.
 * In there, OkHttp3Integrator has an applyTo() method that
 * attaches a TrustManagerBuilder to an OkHttpClient.Builder.
 */
public class TrustManagerBuilder {
  private CompositeTrustManager mgr=CompositeTrustManager.matchAll();
  private ApplicationConfig appConfig=null;

  /**
   * @return the CompositeTrustManager representing the particular
   *         rules you want to apply
   */
  public CompositeTrustManager build() {
    return(mgr);
  }

  /**
   * @return the TrustManager from build(), wrapped into a
   *         one-element array, for convenience
   */
  public X509TrustManager[] buildArray() {
    return(new X509TrustManager[] { build() });
  }

  /**
   * Configures the supplied HttpURLConnection to use the trust
   * manager configured via this builder. This will only be done
   * if the connection really is an HttpsURLConnection (a subclass
   * of HttpURLConnection).
   *
   * @param c the connection to configure
   * @return the connection passed in, for chaining
   * @throws NoSuchAlgorithmException
   * @throws KeyManagementException
   */
  public HttpURLConnection applyTo(HttpURLConnection c)
    throws NoSuchAlgorithmException, KeyManagementException {
    if (c instanceof HttpsURLConnection && mgr.size()>0) {
      SSLContext ssl=SSLContext.getInstance("TLS");
      TrustManager[] trustManagers=buildArray();

      ssl.init(null, trustManagers, null);
      ((HttpsURLConnection)c).setSSLSocketFactory(ssl.getSocketFactory());
    }

    return(c);
  }

  /**
   * Use this to add arbitrary TrustManagers to
   * the mix. Only the X509TrustManager instances in the
   * array will be used. This is also used, under the
   * covers, by most of the other builder methods, to add
   * configured trust managers.
   *
   * @param mgrs
   *          the TrustManager instances to add
   * @return the builder for chained calls
   */
  public TrustManagerBuilder add(TrustManager... mgrs) {
    for (TrustManager tm : mgrs) {
      if (tm instanceof X509TrustManager) {
        mgr.add((X509TrustManager)tm);
      }
    }

    return(this);
  }

  /**
   * Any subsequent configuration of this builder, until the
   * next and() call (or build()/buildArray()), will be
   * logically OR'd with whatever came previously. For
   * example, if you need to support two possible
   * self-signed certificates, use
   * selfSigned(...).or().selfSigned(...) to accept either
   * one.
   * 
   * @return the builder for chained calls
   */
  public TrustManagerBuilder or() {
    if (mgr.isMatchAll()) {
      if (mgr.size() < 2) {
        mgr.setMatchAll(false);
      }
      else {
        mgr=CompositeTrustManager.matchAny(mgr);
      }
    }

    return(this);
  }

  /**
   * Any subsequent configuration of this builder, until the
   * next or() call (or build()/buildArray()), will be
   * logically AND'd with whatever came previously. Note
   * that this is the default state or the builder, so you
   * only need an and() to reverse a previous or().
   * 
   * @return the builder for chained calls
   */
  public TrustManagerBuilder and() {
    if (!mgr.isMatchAll()) {
      if (mgr.size() < 2) {
        mgr.setMatchAll(true);
      }
      else {
        mgr=CompositeTrustManager.matchAll(mgr);
      }
    }

    return(this);
  }

  /**
   * Tells the builder to add the default (system)
   * TrustManagers to the roster of ones to consider. For
   * example, to support normal certificates plus a
   * self-signed certificate, use
   * useDefault().or().selfSigned(...).
   * 
   * @return the builder for chained calls
   * @throws NoSuchAlgorithmException
   * @throws KeyStoreException
   */
  public TrustManagerBuilder useDefault()
    throws NoSuchAlgorithmException, KeyStoreException {
    TrustManagerFactory tmf=
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

    tmf.init((KeyStore)null);
    add(tmf.getTrustManagers());

    return(this);
  }

  /**
   * Rejects all certificates. At most, this is useful in
   * testing. Use in a production app will cause us to
   * question your sanity.
   * 
   * @return the builder for chained calls
   */
  public TrustManagerBuilder denyAll() {
    mgr.add(new DenyAllTrustManager());

    return(this);
  }

  /**
   * Use the network security configuration identified by the supplied
   * XML resource ID.
   *
   * @param ctxt any Context will work
   * @param resourceId an R.xml value pointing to the configuration
   * @return the builder for chained calls
   */
  public TrustManagerBuilder withConfig(@NonNull Context ctxt,
                                        @XmlRes int resourceId) {
    return(withConfig(new XmlConfigSource(ctxt, resourceId, false)));
  }

  /**
   * Use the network security configuration identified by the supplied
   * XML resource ID.
   *
   * @param ctxt any Context will work
   * @param resourceId an R.xml value pointing to the configuration
   * @param isDebugBuild true if this should be treated as a debug
   *                     build, false otherwise
   * @return the builder for chained calls
   */
  public TrustManagerBuilder withConfig(@NonNull Context ctxt,
                                        @XmlRes int resourceId,
                                        boolean isDebugBuild) {
    return(withConfig(new XmlConfigSource(ctxt, resourceId,
      isDebugBuild)));
  }

  /**
   * Use the network security configuration identified configured
   * in the app's manifest.
   *
   * @param ctxt any Context will work
   * @return the builder for chained calls
   */
  public TrustManagerBuilder withManifestConfig(@NonNull Context ctxt) {
    if (Build.VERSION.SDK_INT<Build.VERSION_CODES.N) {
      return(withConfig(new ManifestConfigSource(ctxt)));
    }

    return(this);
  }

  TrustManagerBuilder withConfig(@NonNull ConfigSource config) {
    appConfig=new ApplicationConfig(config);

    return(add(appConfig.getTrustManager()));
  }

  /**
   * Add a listener to be handed all certificate chains. Use this
   * solely for diagnostic purposes (e.g., to understand what
   * root CA to add to a network security configuration). Do not use
   * this in production code.
   *
   * @param listener a listener to be notified of certificate chains
   * @return the builder for chained calls
   */
  public TrustManagerBuilder withCertChainListener(CertChainListener listener) {
    mgr.addCertChainListener(listener);

    return(this);
  }

  /**
   * @return true if the network security configuration allows
   * cleartext traffic, false otherwise
   */
  public boolean isCleartextTrafficPermitted() {
    if (appConfig==null) {
      return(true);
    }

    return(appConfig.isCleartextTrafficPermitted());
  }

  /**
   * @param hostname the domain name to check for cleartext availability
   * @return true if the network security configuration allows
   * cleartext traffic for this domain name, false otherwise
   */
  public boolean isCleartextTrafficPermitted(String hostname) {
    if (appConfig==null) {
      return(true);
    }

    return(appConfig.isCleartextTrafficPermitted(hostname));
  }
}
