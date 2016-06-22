package com.commonsware.cwac.netseccfg;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.X509TrustManager;

public interface X509Extensions extends X509TrustManager {
  List<X509Certificate> checkServerTrusted(X509Certificate[] chain,
                                           String authType,
                                           String host)
    throws CertificateException;
  boolean isUserAddedCertificate (X509Certificate cert);
}
