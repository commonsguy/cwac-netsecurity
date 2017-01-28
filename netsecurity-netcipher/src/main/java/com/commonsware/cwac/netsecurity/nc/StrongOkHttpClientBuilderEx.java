/***
 Copyright (c) 2017 CommonsWare, LLC
 Licensed under the Apache License, Version 2.0 (the "License"); you may not
 use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
 by applicable law or agreed to in writing, software distributed under the
 License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 OF ANY KIND, either express or implied. See the License for the specific
 language governing permissions and limitations under the License.
 */

package com.commonsware.cwac.netsecurity.nc;

import android.content.Context;
import android.content.Intent;
import com.commonsware.cwac.netsecurity.OkHttp3Integrator;
import com.commonsware.cwac.netsecurity.TrustManagerBuilder;
import info.guardianproject.netcipher.client.StrongOkHttpClientBuilder;
import okhttp3.OkHttpClient;

public class StrongOkHttpClientBuilderEx extends StrongOkHttpClientBuilder {
  private final TrustManagerBuilder tmb;

  /**
   * Creates a StrongOkHttpClientBuilderEx using the strongest set
   * of options for security. Use this if the strongest set of
   * options is what you want; otherwise, create a
   * builder via the constructor and configure it as you see fit.
   *
   * @param ctxt any Context will do
   * @return a configured StrongOkHttpClientBuilderEx
   * @throws Exception
   */
  static public StrongOkHttpClientBuilderEx newInstance(Context ctxt,
                                                        TrustManagerBuilder tmb)
    throws Exception {
    return(StrongOkHttpClientBuilderEx)new StrongOkHttpClientBuilderEx(ctxt, tmb)
      .withBestProxy();
  }

  private StrongOkHttpClientBuilderEx(Context ctxt, TrustManagerBuilder tmb) {
    super(ctxt);
    this.tmb=tmb;
  }

  /**
   * This method is not supported at present.
   */
  @Override
  public StrongOkHttpClientBuilder withTorValidation() {
    throw new UnsupportedOperationException("withTorValidation() is not supported right now");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OkHttpClient.Builder applyTo(OkHttpClient.Builder builder,
                                      Intent status) {
    builder.proxy(buildProxy(status));

    try {
      OkHttp3Integrator.applyTo(tmb, builder);
    }
    catch (Exception e) {
      throw new
        RuntimeException("Exception adding configuration to OkHttpClient.Builder", e);
    }

    return(builder);
  }
}
