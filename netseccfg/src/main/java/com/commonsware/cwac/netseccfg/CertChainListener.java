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

package com.commonsware.cwac.netseccfg;

import java.security.cert.X509Certificate;

/**
 * A listener interface to use with TrustManagerBuilder to be handed
 * a copy of each certificate chain validated by the trust manager.
 */
public interface CertChainListener {
  /**
   * Called before validating each certificate chain
   *
   * @param chain the certificate chain
   * @param domain the domain name the chain applies to, or null if
   *               this is not known
   */
  void onChain(X509Certificate[] chain, String domain);
}
