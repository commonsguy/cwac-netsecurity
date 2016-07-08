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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * X509TrustManager that rejects everything. Useful for testing.
 * Not so useful anywhere else.
 */
public class DenyAllTrustManager implements X509Extensions {
  /**
   * {@inheritDoc}
   */
  @Override
  public void checkClientTrusted(X509Certificate[] chain,
                                 String authType)
    throws CertificateException {
    throw new CertificateException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void checkServerTrusted(X509Certificate[] chain,
                                 String authType)
    throws CertificateException {
    throw new CertificateException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return(new X509Certificate[0]);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<X509Certificate> checkServerTrusted(
    X509Certificate[] chain, String authType, String host)
    throws CertificateException {
    throw new CertificateException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isUserAddedCertificate(X509Certificate cert) {
    return(false);
  }
}
