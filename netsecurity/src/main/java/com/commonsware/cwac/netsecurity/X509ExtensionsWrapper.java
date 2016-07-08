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

package com.commonsware.cwac.netsecurity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.X509TrustManager;

/**
 * Wrapper for an X509TrustManager, to try to access the
 * X509Extensions methods via reflection, while also honoring
 * the X509TrustManager contract.
 */
public class X509ExtensionsWrapper implements X509Extensions {
  private static final String ERROR_CONTRACT=
    "Supplied X509TrustManager does not implement X509Extensions contract";
  private final X509TrustManager tm;
  private final Method checkServerTrustedMethod;
  private Method isUserAddedCertificateMethod;

  /**
   * Standard constructor. Fails if the X509TrustManager does not
   * implement the three-parameter checkServerTrusted() method.
   *
   * @param tm X509TrustManager to wrap
   * @throws IllegalArgumentException
   */
  public X509ExtensionsWrapper(X509TrustManager tm) throws IllegalArgumentException {
    this.tm=tm;

    try {
      checkServerTrustedMethod=
        tm
          .getClass()
          .getMethod("checkServerTrusted",
            X509Certificate[].class,
            String.class,
            String.class);
    }
    catch (NoSuchMethodException e) {
      throw new IllegalArgumentException(ERROR_CONTRACT);
    }

    try {
      isUserAddedCertificateMethod=
        tm
          .getClass()
          .getMethod("isUserAddedCertificate", X509Certificate.class);
    }
    catch (NoSuchMethodException e) {
      // ok, we'll fail gracefully for this one
    }
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public List<X509Certificate> checkServerTrusted(X509Certificate[] chain,
                                                  String authType,
                                                  String host)
    throws CertificateException {
    try {
      return((List<X509Certificate>)checkServerTrustedMethod
        .invoke(tm, chain, authType, host));
    }
    catch (IllegalAccessException e) {
      throw new CertificateException(ERROR_CONTRACT, e);
    }
    catch (InvocationTargetException e) {
      if (e.getCause() instanceof CertificateException) {
        throw (CertificateException)e.getCause();
      }
      else if (e.getCause() instanceof RuntimeException) {
        throw (RuntimeException)e.getCause();
      }

      throw new CertificateException("checkServerTrusted() failure",
        e.getCause());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isUserAddedCertificate(X509Certificate cert) {
    if (isUserAddedCertificateMethod==null) {
      return(false);
    }

    try {
      return((Boolean)isUserAddedCertificateMethod.invoke(tm, cert));
    }
    catch (IllegalAccessException e) {
      throw new RuntimeException(ERROR_CONTRACT, e);
    }
    catch (InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException) {
        throw (RuntimeException)e.getCause();
      }
      else {
        throw new RuntimeException("isUserAddedCertificat() failure",
          e.getCause());
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void checkClientTrusted(X509Certificate[] x509Certificates,
                                 String s)
    throws CertificateException {
    tm.checkClientTrusted(x509Certificates, s);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void checkServerTrusted(X509Certificate[] x509Certificates,
                                 String s)
    throws CertificateException {
    tm.checkServerTrusted(x509Certificates, s);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return(tm.getAcceptedIssuers());
  }
}