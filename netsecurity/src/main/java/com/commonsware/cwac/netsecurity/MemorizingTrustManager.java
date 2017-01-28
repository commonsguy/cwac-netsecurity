/***
  Copyright (c) 2014-2017 CommonsWare, LLC
  
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

import android.support.annotation.NonNull;
import android.util.LruCache;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Implementation of a memorizing trust manager, inspired by
 * https://github.com/ge0rg/MemorizingTrustManager, but
 * designed to be used by CompositeTrustManager.
 *
 * If you use this class, you will get SSLHandshakeExceptions in two
 * cases. One is if we do not have a certificate for the domain (host)
 * used for this request, and you called noTOFU() on the
 * Builder. In that case, getCause() of the
 * SSLHandshakeException will contain a CertificateNotMemorizedException.
 * You can deal with that as you see fit, including passing it to
 * memorize() or memorizeForNow() on the MemorizingTrustManager to
 * memorize it.
 *
 * The other SSLHandshakeException scenario of note is if we *do*
 * have a certificate for this host, but it does not match the
 * server when we tried connecting to it. In that case, getCause()
 * on the SSLHandshakeException will return a MemorizationMismatchException.
 * Either the server legitimately changed SSL certificates since
 * you memorized the last one, or an MITM attack is going on.
 *
 * Use MemorizingTrustManager.Builder to create instances of this.
 */
public class MemorizingTrustManager implements X509Extensions {
  private final File workingDir;
  private final char[] storePassword;
  private final String storeType;
  private final boolean noTOFU;
  private final LruCache<String, MemorizingStore> stores;

  private MemorizingTrustManager(File workingDir, char[] storePassword,
                                 String storeType, boolean noTOFU,
                                 int cacheSize) {
    this.workingDir=workingDir;
    this.storePassword=storePassword;
    this.storeType=storeType;
    this.noTOFU=noTOFU;
    this.stores=new LruCache<>(cacheSize);
  }

  /*
   * {@inheritDoc}
   */
  @Override
  public void checkClientTrusted(@NonNull X509Certificate[] chain,
                                              String authType)
    throws CertificateException {
    throw new UnsupportedOperationException("Client checks not supported");
  }

  /*
   * {@inheritDoc}
   */
  @Override
  public void checkServerTrusted(@NonNull X509Certificate[] chain,
                                              String authType)
    throws CertificateException {
    throw new IllegalStateException("Must use three-parameter checkServerTrusted()");
  }

  /*
   * {@inheritDoc}
   */
  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return(new X509Certificate[0]);
  }

  /*
   * {@inheritDoc}
   */
  @Override
  public List<X509Certificate> checkServerTrusted(
    @NonNull X509Certificate[] chain, String authType, String host)
    throws CertificateException {

    try {
      getStoreForHost(host).checkServerTrusted(chain, authType);
    }
    catch (Exception e) {
      if (e instanceof CertificateNotMemorizedException ||
        e instanceof MemorizationMismatchException) {
        throw (CertificateException)e;
      }
      else {
        throw new CertificateException("Exception setting up memoization", e);
      }
    }

    return(Arrays.asList(chain));
  }

  /*
   * {@inheritDoc}
   */
  @Override
  public boolean isUserAddedCertificate(X509Certificate cert) {
    return(false);
  }

  /**
   * If you catch an SSLHandshakeException when performing
   * HTTPS I/O, and its getCause() is a
   * CertificateNotMemorizedException, then you know that
   * you configured certificate memorization and the SSL
   * certificate for your request was not recognized.
   *
   * If the user agrees that your app should use the SSL
   * certificate forever (or until you clear it), call
   * this method, supplying the CertificateNotMemorizedException.
   * Note that this will perform disk I/O and therefore
   * should be done on a background thread.
   *
   * @param ex  exception with details of the certificate to be memorized
   * @throws Exception if there is a problem in memorizing the certificate
   */
  public void memorize(@NonNull MemorizationException ex)
    throws Exception {
    getStoreForHost(ex.host).memorize(ex.chain);
  }

  /**
   * If you catch an SSLHandshakeException when performing
   * HTTPS I/O, and its getCause() is a
   * CertificateNotMemorizedException, then you know that
   * you configured certificate memorization and the SSL
   * certificate for your request was not recognized.
   *
   * If the user agrees that your app should use the SSL
   * certificate for the lifetime of this process only, but
   * not retain it beyond that, call this method,
   * supplying the CertificateNotMemorizedException. Once
   * your process is terminated, this cached certificate is
   * lost, and you will get a CertificateNotMemorizedException
   * again later on. Also, this class only caches a certain
   * number of domains' worth of certificates, so if you are
   * hitting a wide range of sites, the certificate may be lost.
   *
   * @param ex  exception with details of the certificate to be memoized
   */
  synchronized public void memorizeForNow(@NonNull MemorizationException ex)
    throws Exception {
    getStoreForHost(ex.host).memorizeForNow(ex.chain);
  }

  /**
   * Clears the transient key store used by memorizeForNow(),
   * and optionally clears the persistent key store used by
   * memorize().
   *
   * Note that some caching HTTP clients (e.g., OkHttp) may
   * have live SSLSession objects that they reuse. In that
   * case, the effects of this method will not be seen until
   * those SSLSession objects are purged (e.g., you create
   * a fresh OkHttpClient).
   *
   * @param host  host whose stores should be cleared
   * @param clearPersistent
   *          true to clear both key stores, false to clear
   *          only the transient one
   */
  public void clear(String host, boolean clearPersistent)
    throws Exception {
    getStoreForHost(host).clear(clearPersistent);
  }

  /**
   * Clears details for all domains.
   *
   * Note that some caching HTTP clients (e.g., OkHttp) may
   * have live SSLSession objects that they reuse. In that
   * case, the effects of this method will not be seen until
   * those SSLSession objects are purged (e.g., you create
   * a fresh OkHttpClient).
   *
   * @param clearPersistent true to clear both memorize() and
   *                        memorizeForNow() data; false to just
   *                        clear memorizeForNow()
   * @throws Exception
   */
  synchronized public void clearAll(boolean clearPersistent) throws Exception {
    for (String host : stores.snapshot().keySet()) {
      clear(host, clearPersistent);
    }
  }

  private MemorizingStore getStoreForHost(String host) throws Exception {
    MemorizingStore store;

    synchronized(this) {
      store=stores.get(host);

      if (store==null) {
        store=new MemorizingStore(host, workingDir, storePassword, storeType,
          noTOFU);
        stores.put(host, store);
      }
    }

    return(store);
  }

  /**
   * Builder-style API for creating instances of MemorizingTrustManager.
   * Create an instance of this class, call either version of saveTo()
   * (and optionally other configuration methods), then call build()
   * to get a MemorizingTrustManager.
   */
  public static class Builder {
    private File workingDir=null;
    private char[] storePassword;
    private String storeType;
    private boolean noTOFU=false;
    private int cacheSize=128;

    /**
     * Indicates where the keystores associated with memorize() should
     * go. This should be an empty directory that you are not using for
     * any other purpose. Also, please put it on internal storage
     * (e.g., subdirectory off of getFilesDir() or getCacheDir()), for
     * security.
     *
     * @param workingDir where we should store memorized certificates
     * @param storePassword passphrase to use for the keystore files
     * @return the builder, for further configuration
     */
    public Builder saveTo(File workingDir, char[] storePassword) {
      return(saveTo(workingDir, storePassword, KeyStore.getDefaultType()));
    }

    /**
     * Indicates where the keystores associated with memorize() should
     * go. This should be an empty directory that you are not using for
     * any other purpose. Also, please put it on internal storage
     * (e.g., subdirectory off of getFilesDir() or getCacheDir()), for
     * security.
     *
     * @param workingDir where we should store memorized certificates
     * @param storePassword passphrase to use for the keystore files
     * @param storeType specific type of keystore file to use
     * @return the builder, for further configuration
     */
    public Builder saveTo(File workingDir, char[] storePassword,
                          String storeType) {
      this.workingDir=workingDir;
      this.storePassword=storePassword;
      this.storeType=storeType;

      return(this);
    }

    /**
     * By default, trust on first use (TOFU) is enabled, and so all unrecognized
     * certificates are memorized automatically.
     *
     * If you call noTOFU(), and we encounter a certificate for a domain for
     * which we have no other certificates, you will get a
     * CertificateNotMemorizedException via a wrapper SSLHandshakeException.
     *
     * @return the builder, for further configuration
     */
    public Builder noTOFU() {
      this.noTOFU=true;

      return(this);
    }

    /**
     * Indicates the number of domains for which to cache certificates in
     * memory. Domains ejected from the cache will lose any transient
     * certificates (memorizeForNow()) but will retain and persistent
     * certificates (memorize()). Value must be greater than zero (duh).
     *
     * @param cacheSize number of domains to keep in cache (default: 128)
     * @return the builder, for further configuration
     */
    public Builder cacheSize(int cacheSize) {
      if (cacheSize<=0) {
        throw new IllegalArgumentException("Please provide a sensible cache size");
      }

      this.cacheSize=cacheSize;

      return(this);
    }

    /**
     * Validates your configuration and builds the MemorizingTrustManager.
     *
     * This creates a new instance each time, so it is safe to hold onto
     * this Builder and create more than one MemorizingTrustManager. However,
     * do not use more than one MemorizingTrustManager at a time, as
     * multiple instances do not coordinate with one another, and so each
     * instance will be oblivious to memorizations (or clear() calls) made
     * on other instances.
     *
     * @return the MemorizingTrustManager, built to your exacting specifications
     */
    public MemorizingTrustManager build() {
      if (workingDir==null) {
        throw new IllegalStateException("You have not configured this builder!");
      }

      workingDir.mkdirs();

      return(new MemorizingTrustManager(workingDir, storePassword, storeType,
        noTOFU, cacheSize));
    }
  }

  private static class MemorizingStore {
    private final String host;
    private final File store;
    private final char[] storePassword;
    private final String storeType;
    private final boolean noTOFU;
    private KeyStore keyStore;
    private X509TrustManager storeTrustManager;
    private KeyStore transientKeyStore;
    private X509TrustManager transientTrustManager;

    MemorizingStore(String host, File workingDir, char[] storePassword,
                    String storeType, boolean noTOFU) throws Exception {
      this.host=host;
      store=new File(workingDir, host);
      this.storePassword=storePassword;
      this.storeType=storeType;
      this.noTOFU=noTOFU;

      init();
    }

    synchronized void checkServerTrusted(@NonNull X509Certificate[] chain,
                                                String authType)
      throws CertificateException {
      try {
        storeTrustManager.checkServerTrusted(chain, authType);
      }
      catch (CertificateException e) {
        try {
          transientTrustManager.checkServerTrusted(chain, authType);
        }
        catch (CertificateException e2) {
          try {
            if (keyStore.size()==0 && transientKeyStore.size()==0) {
              if (!noTOFU) {
                try {
                  memorize(chain);
                  return;
                }
                catch (Exception e4) {
                  throw new CertificateException("Problem while memorizing", e4);
                }
              }

              throw new CertificateNotMemorizedException(chain, host);
            }
          }
          catch (KeyStoreException kse) {
            // srsly?
          }

          throw new MemorizationMismatchException(chain, host, e2);
        }
      }
    }

    synchronized void memorize(@NonNull X509Certificate[] chain)
      throws Exception {
      for (X509Certificate cert : chain) {
        String alias=cert.getSubjectDN().getName();

        keyStore.setCertificateEntry(alias, cert);
      }

      TrustManagerFactory tmf=TrustManagerFactory.getInstance("X509");

      tmf.init(keyStore);
      storeTrustManager=findX509TrustManager(tmf);

      FileOutputStream fos=new FileOutputStream(store);

      keyStore.store(fos, storePassword);
      fos.flush();
      fos.close();
    }

    synchronized void memorizeForNow(@NonNull X509Certificate[] chain)
      throws Exception {
      for (X509Certificate cert : chain) {
        String alias=cert.getSubjectDN().getName();

        transientKeyStore.setCertificateEntry(alias, cert);
      }

      TrustManagerFactory tmf=TrustManagerFactory.getInstance("X509");

      tmf.init(transientKeyStore);
      transientTrustManager=findX509TrustManager(tmf);
    }

    synchronized void clear(boolean clearPersistent) throws Exception {
      if (clearPersistent) {
        store.delete();
      }

      init();
    }

    private void init() throws Exception {
      transientKeyStore=KeyStore.getInstance(storeType);
      transientKeyStore.load(null, null);

      TrustManagerFactory tmf=TrustManagerFactory.getInstance("X509");

      tmf.init(transientKeyStore);
      transientTrustManager=findX509TrustManager(tmf);

      keyStore=KeyStore.getInstance(storeType);

      if (store.exists()) {
        keyStore.load(new FileInputStream(store), storePassword);
      }
      else {
        keyStore.load(null, storePassword);
      }

      tmf=TrustManagerFactory.getInstance("X509");
      tmf.init(keyStore);
      storeTrustManager=findX509TrustManager(tmf);
    }

    private X509TrustManager findX509TrustManager(TrustManagerFactory tmf) {
      for (TrustManager t : tmf.getTrustManagers()) {
        if (t instanceof X509TrustManager) {
          return (X509TrustManager)t;
        }
      }

      return(null);
    }
  }
}
