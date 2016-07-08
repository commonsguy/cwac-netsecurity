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
import android.support.annotation.NonNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
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
 * designed to be used by TrustManagerBuilder.
 *
 * Still a work in progress, as this really needs domain-specific
 * stores.
 */
public class MemorizingTrustManager implements X509Extensions {
  private KeyStore keyStore=null;
  private Options options=null;
  private X509TrustManager storeTrustManager=null;
  private KeyStore transientKeyStore=null;
  private X509TrustManager transientTrustManager=null;

  /**
   * Standard constructor
   *
   * @param options
   *          a MemorizingTrustManager.Options object, to
   *          configure the memorization behavior
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws CertificateException
   * @throws IOException
   */
  public MemorizingTrustManager(@NonNull Options options)
      throws KeyStoreException, NoSuchAlgorithmException,
      CertificateException, IOException {
    this.options=options;

    clear(false);
  }

  /*
   * {@inheritDoc}
   */
  @Override
  synchronized public void checkClientTrusted(@NonNull X509Certificate[] chain,
                                              String authType)
    throws CertificateException {
    try {
      storeTrustManager.checkClientTrusted(chain, authType);
    }
    catch (CertificateException e) {
      try {
        transientTrustManager.checkClientTrusted(chain, authType);
      }
      catch (CertificateException e2) {
        throw new CertificateNotMemorizedException(chain);
      }
    }
  }

  /*
   * {@inheritDoc}
   */
  @Override
  synchronized public void checkServerTrusted(@NonNull X509Certificate[] chain,
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
        throw new CertificateNotMemorizedException(chain);
      }
    }
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
  synchronized public List<X509Certificate> checkServerTrusted(
    @NonNull X509Certificate[] chain, String authType, String host)
    throws CertificateException {
    checkServerTrusted(chain, authType);

    return(Arrays.asList(chain));
  }

  /*
   * {@inheritDoc}
   */
  @Override
  synchronized public boolean isUserAddedCertificate(X509Certificate cert) {
    return(false);
  }

  /**
   * If you catch an SSLHandshakeException when performing
   * HTTPS I/O, and its getCause() is a
   * CertificateNotMemorizedException, then you know that
   * you configured certificate memorization using
   * memorize(), and the SSL certificate for your request
   * was not recognized.
   *
   * If the user agrees that your app should use the SSL
   * certificate forever (or until you clear it), call
   * this method, supplying the certificate chain you get
   * by calling getCertificateChain() on the
   * CertificateNotMemorizedException. Note that this will
   * perform disk I/O and therefore should be done on a
   * background thread. But, your network I/O is already being done
   * on a background thread, right? Right?!?
   *
   * @param chain
   *          user-approved certificate chain
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws CertificateException
   * @throws IOException
   */
  synchronized public void memorizeCert(@NonNull X509Certificate[] chain)
    throws KeyStoreException, NoSuchAlgorithmException,
    CertificateException, IOException {
    for (X509Certificate cert : chain) {
      String alias=cert.getSubjectDN().getName();

      keyStore.setCertificateEntry(alias, cert);
    }

    initTrustManager();

    FileOutputStream fos=new FileOutputStream(options.store);

    keyStore.store(fos, options.storePassword.toCharArray());
    fos.flush();
    fos.close();
  }

  /**
   * If you catch an SSLHandshakeException when performing
   * HTTPS I/O, and its getCause() is a
   * CertificateNotMemorizedException, then you know that
   * you configured certificate memorization using
   * memorize(), and the SSL certificate for your request
   * was not recognized.
   *
   * If the user agrees that your app should use the SSL
   * certificate for the lifetime of this process only, but
   * not retain it beyond that, call this method,
   * supplying the certificate chain you get by calling
   * getCertificateChain() on the
   * CertificateNotMemorizedException. Once your process is
   * terminated, this cached certificate is lost, and you
   * will get a CertificateNotMemorizedException again later
   * on.
   *
   * @param chain
   *          user-approved certificate chain
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   */
  synchronized public void allowCertForProcess(@NonNull X509Certificate[] chain)
    throws KeyStoreException, NoSuchAlgorithmException {
    for (X509Certificate cert : chain) {
      String alias=cert.getSubjectDN().getName();

      transientKeyStore.setCertificateEntry(alias, cert);
    }

    initTrustManager();
  }

  /**
   * Clears the transient key store used by allowCertForProcess(),
   * and optionally clears the persistent key store (by deleting
   * its file and re-initializing it).
   * 
   * @param clearPersistent
   *          true to clear both key stores, false to clear
   *          only the transient one
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws CertificateException
   * @throws IOException
   */
  synchronized public void clear(boolean clearPersistent)
    throws KeyStoreException, NoSuchAlgorithmException,
    CertificateException, IOException {
    if (clearPersistent) {
      options.store.delete();
    }

    initTransientStore();
    initPersistentStore();
    initTrustManager();
  }

  private void initTransientStore() throws KeyStoreException,
    NoSuchAlgorithmException, CertificateException, IOException {
    transientKeyStore=KeyStore.getInstance(options.storeType);
    transientKeyStore.load(null, null);
  }

  private void initPersistentStore() throws KeyStoreException,
    NoSuchAlgorithmException, CertificateException, IOException {
    keyStore=KeyStore.getInstance(options.storeType);

    if (options.store.exists()) {
      keyStore.load(new FileInputStream(options.store),
                    options.storePassword.toCharArray());
    }
    else {
      keyStore.load(null, options.storePassword.toCharArray());
    }
  }

  private void initTrustManager() throws KeyStoreException,
    NoSuchAlgorithmException {
    TrustManagerFactory tmf=TrustManagerFactory.getInstance("X509");

    tmf.init(keyStore);

    for (TrustManager t : tmf.getTrustManagers()) {
      if (t instanceof X509TrustManager) {
        storeTrustManager=(X509TrustManager)t;
        break;
      }
    }

    tmf=TrustManagerFactory.getInstance("X509");

    tmf.init(transientKeyStore);

    for (TrustManager t : tmf.getTrustManagers()) {
      if (t instanceof X509TrustManager) {
        transientTrustManager=(X509TrustManager)t;
        break;
      }
    }
  }

  /**
   * Configuration options for certificate memorization.
   * This class has a builder-style API, so you can
   * configure an instance via a chained set of method
   * calls.
   */
  public static class Options {
    private File workingDir=null;
    private File store=null;
    private String storePassword;
    private String storeType=KeyStore.getDefaultType();

    /**
     * Constructor. Note that the Context is not held by the
     * Options instance, and so any handy Context should be
     * fine.
     * 
     * @param ctxt
     *          a Context
     * @param storeRelPath
     *          a relative path within internal storage to a
     *          working directory for the
     *          MemorizingTrustManager (parent directories
     *          will be created for you as needed)
     * @param storePassword
     *          the password under which to store these
     *          certificates
     */
    public Options(Context ctxt, String storeRelPath,
                   String storePassword) {
      workingDir=new File(ctxt.getFilesDir(), storeRelPath);
      workingDir.mkdirs();
      store=new File(workingDir, "memorized.bks");

      this.storePassword=storePassword;
    }
  }
}
