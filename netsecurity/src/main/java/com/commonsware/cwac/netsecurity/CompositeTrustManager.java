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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.net.ssl.X509TrustManager;

/**
 * X509TrustManager that handles combinations of child managers,
 * apply OR and AND logic to determine the response.
 */
public class CompositeTrustManager implements X509Extensions {
  private static final ThreadLocal<String> host=new ThreadLocal<>();
  private ArrayList<X509Extensions> managers=new ArrayList<>();
  private boolean matchAll;
  private ArrayList<CertChainListener> certChainListeners=
    new ArrayList<>();

  /**
   * Factory method to wrap 1+ x509TrustManagers in a CompositeTrustManager
   * that implements AND logic across them.
   *
   * @param managers the managers to AND together
   * @return the CompositeTrustManager
   */
  public static CompositeTrustManager matchAll(X509TrustManager... managers) {
    return(new CompositeTrustManager(managers, true));
  }

  /**
   * Factory method to wrap 1+ x509TrustManagers in a CompositeTrustManager
   * that implements OR logic across them.
   *
   * @param managers the managers to AND together
   * @return the CompositeTrustManager
   */
  public static CompositeTrustManager matchAny(X509TrustManager... managers) {
    return(new CompositeTrustManager(managers, false));
  }

  protected CompositeTrustManager(X509TrustManager[] mgrs,
                                  boolean matchAll) {
    if (mgrs != null) {
      addAll(mgrs);
    }

    setMatchAll(matchAll);
  }

  /**
   * Provide the hostname to use for subsequent certificate chain
   * evaluations. This is not needed for API Level 24+ but is needed
   * for older versions.
   *
   * @param host
   */
  public void setHost(String host) {
    this.host.set(host);
  }

  /**
   /**
   * Add a listener to be handed all certificate chains. Use this
   * solely for diagnostic purposes (e.g., to understand what
   * root CA to add to a network security configuration). Do not use
   * this in production code.
   *
   * @param listener a listener to be notified of certificate chains
   */
  public void addCertChainListener(CertChainListener listener) {
    certChainListeners.add(listener);
  }

  /**
   * Remove a previously-registered CertChainListener.
   *
   * @param listener CertChainListener to remove
   */
  public void removeCertChainListener(CertChainListener listener) {
    certChainListeners.remove(listener);
  }

  /**
   * @return true if this manager has 1+ CertChainListeners, false
   * otherwise
   */
  public boolean hasCertChainListeners() {
    return(certChainListeners.size()>0);
  }

  /**
   * Add an X509TrustManager to the ones being governed by this
   * composite. Ideally, this implements X509Extensions. If not,
   * it will be wrapped in an X509ExtensionsWrapper, to try to call
   * additional methods via reflection.
   *
   * @param mgr the X509TrustManager to add
   */
  public void add(X509TrustManager mgr) {
    if (mgr instanceof X509Extensions) {
      managers.add((X509Extensions)mgr);
    }
    else {
      managers.add(new X509ExtensionsWrapper(mgr));
    }
  }

  /**
   * Adds several X509TrustManagers, by delegation to add()
   *
   * @param mgrs the trust managers to add
   */
  public void addAll(X509TrustManager[] mgrs) {
    for (X509TrustManager mgr : mgrs) {
      add(mgr);
    }
  }

  /**
   * @return true if AND logic will be applied to the managers in
   * this composite, false if OR logic will be applied
   */
  public boolean isMatchAll() {
    return(matchAll);
  }

  /**
   * Attempt to change the AND/OR rule. If there is more than one
   * trust manager, this will be rejected via an IllegalStateException.
   *
   * @param matchAll true if AND logic will be applied to the managers in
   * this composite, false if OR logic will be applied
   */
  public void setMatchAll(boolean matchAll) {
    if (managers.size()>1) {
      throw new IllegalStateException("Cannot change mode once 2+ managers added");
    }
    
    this.matchAll=matchAll;
  }

  /**
   * @return the number of managers in this composite
   */
  public int size() {
    return(managers.size());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void checkClientTrusted(X509Certificate[] chain,
                                 String authType)
    throws CertificateException {
    passChainToListeners(chain);

    CertificateException first=null;

    for (X509TrustManager mgr : managers) {
      try {
        mgr.checkClientTrusted(chain, authType);

        if (!matchAll) {
          return;
        }
      }
      catch (CertificateException e) {
        if (matchAll) {
          throw e;
        }
        else {
          first=e;
        }
      }
    }

    if (first != null) {
      throw first;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void checkServerTrusted(X509Certificate[] chain,
                                 String authType)
    throws CertificateException {
    if (host==null) {
      passChainToListeners(chain);

      CertificateException first=null;
      boolean anyGoodResults=false;

      for (X509Extensions mgr : managers) {
        try {
          mgr.checkServerTrusted(chain, authType);
          anyGoodResults=true;
        }
        catch (CertificateException e) {
          if (matchAll) {
            throw e;
          }
          else {
            first=e;
          }
        }
      }

      if (!matchAll && !anyGoodResults && first!=null) {
        throw first;
      }
    }
    else {
      checkServerTrusted(chain, authType, host.get());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<X509Certificate> checkServerTrusted(X509Certificate[] certs,
                                                  String authType,
                                                  String hostname)
    throws CertificateException {
    passChainToListeners(certs, hostname);

    CertificateException first=null;
    boolean anyGoodResults=false;
    List<X509Certificate> result=null;

    for (X509Extensions mgr : managers) {
      try {
        result=mgr.checkServerTrusted(certs, authType, hostname);
        anyGoodResults=true;
      }
      catch (CertificateException e) {
        if (matchAll) {
          throw e;
        }
        else {
          first=e;
        }
      }
    }

    if (!matchAll && !anyGoodResults && first!=null) {
      throw first;
    }

    return(result);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isUserAddedCertificate(X509Certificate cert) {
    boolean result=false;

    for (X509Extensions mgr : managers) {
      boolean localResult=mgr.isUserAddedCertificate(cert);

      if (matchAll) {
        result=result && localResult;
      }
      else if (localResult) {
        return(true);
      }
    }

    return(result);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public X509Certificate[] getAcceptedIssuers() {
    HashSet<X509Certificate> issuers=new HashSet<X509Certificate>();

    for (X509TrustManager mgr : managers) {
      for (X509Certificate cert : mgr.getAcceptedIssuers()) {
        issuers.add(cert);
      }
    }

    return(issuers.toArray(new X509Certificate[issuers.size()]));
  }

  private void passChainToListeners(X509Certificate[] chain) {
    passChainToListeners(chain, host.get());
  }

  private void passChainToListeners(X509Certificate[] chain,
                                    String hostname) {
    for (CertChainListener listener : certChainListeners) {
      listener.onChain(chain, hostname);
    }
  }
}
