/***
  Copyright (c) 2014 CommonsWare, LLC
  
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

package com.commonsware.cwac.netseccfg;

import android.net.http.X509TrustManagerExtensions;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

public class CompositeTrustManager implements X509Extensions {
  private ArrayList<X509Extensions> managers=new ArrayList<>();
  private boolean matchAll;
  private String host;

  public static CompositeTrustManager matchAll(X509TrustManager... managers) {
    return(new CompositeTrustManager(managers, true));
  }

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

  void setHost(String host) {
    this.host=host;
  }

  public void add(X509TrustManager mgr) {
    if (mgr instanceof X509Extensions) {
      managers.add((X509Extensions)mgr);
    }
    else {
      managers.add(new X509ExtensionsWrapper(mgr));
    }
  }

  public void addAll(X509TrustManager[] mgrs) {
    for (X509TrustManager mgr : mgrs) {
      add(mgr);
    }
  }
  
  public boolean isMatchAll() {
    return(matchAll);
  }
  
  public void setMatchAll(boolean matchAll) {
    if (managers.size()>1) {
      throw new IllegalStateException("Cannot change mode once 2+ managers added");
    }
    
    this.matchAll=matchAll;
  }
  
  public int size() {
    return(managers.size());
  }

  X509TrustManager getFirst() {
    return(managers.get(0));
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain,
                                 String authType)
    throws CertificateException {
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

  @Override
  public void checkServerTrusted(X509Certificate[] chain,
                                 String authType)
    throws CertificateException {
    CertificateException first=null;
    boolean anyGoodResults=false;

    for (X509Extensions mgr : managers) {
      try {
        if (host==null) {
          mgr.checkServerTrusted(chain, authType);
        }
        else {
          mgr.checkServerTrusted(chain, authType, host);
        }

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

  public List<X509Certificate> checkServerTrusted(X509Certificate[] certs,
                                                  String authType,
                                                  String hostname)
    throws CertificateException {
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
}
