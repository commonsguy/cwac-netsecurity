/***
  Copyright (c) 2013-2017 CommonsWare, LLC
  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
  by applicable law or agreed to in writing, software distributed under the
  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
  OF ANY KIND, either express or implied. See the License for the specific
  language governing permissions and limitations under the License.
 */

package com.commonsware.cwac.netsecurity.demo;

import android.app.ListFragment;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.commonsware.cwac.netsecurity.CertificateNotMemorizedException;
import com.commonsware.cwac.netsecurity.MemorizationException;
import com.commonsware.cwac.netsecurity.MemorizingTrustManager;
import com.commonsware.cwac.netsecurity.OkHttp3Integrator;
import com.commonsware.cwac.netsecurity.TrustManagerBuilder;
import com.commonsware.cwac.netsecurity.nc.StrongOkHttpClientBuilderEx;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import javax.net.ssl.SSLHandshakeException;
import info.guardianproject.netcipher.client.StrongBuilder;
import info.guardianproject.netcipher.client.StrongOkHttpClientBuilder;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class QuestionsFragment extends ListFragment implements
    Callback<SOQuestions>, StrongBuilder.Callback<OkHttpClient> {
  private Picasso picasso;
  private OkHttpClient ok;
  private MemorizingTrustManager memo;
  private Retrofit restAdapter;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setRetainInstance(true);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    buildClient();
  }

  @Override
  public void onFailure(Call<SOQuestions> call, Throwable t) {
    if (t instanceof SSLHandshakeException) {
      Throwable cause=t.getCause();

      if (cause instanceof CertificateNotMemorizedException) {
        Toast.makeText(getActivity(), "Memorizing certificate...",
          Toast.LENGTH_LONG).show();

        try {
          memo.memorize((MemorizationException)cause);
          load();
        }
        catch (Exception e) {
          Toast.makeText(getActivity(), e.getMessage(),
            Toast.LENGTH_LONG).show();
          Log.e(getClass().getSimpleName(),
            "Exception from MemorizingTrustManager", e);
        }

        return;
      }
    }

    Toast.makeText(getActivity(), t.getMessage(),
                   Toast.LENGTH_LONG).show();
    Log.e(getClass().getSimpleName(),
          "Exception from Retrofit request to StackOverflow", t);
  }

  @Override
  public void onResponse(Call<SOQuestions> call,
                         Response<SOQuestions> response) {
    setListAdapter(new ItemsAdapter(response.body().items));
  }

  @Override
  public void onConnected(OkHttpClient okHttpClient) {
    ok=okHttpClient;
    onOkHttpReady();
  }

  @Override
  public void onConnectionException(final Exception e) {
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast
          .makeText(getActivity(), R.string.msg_crash, Toast.LENGTH_LONG)
          .show();
        Log.e(getClass().getSimpleName(),
          "Exception loading SO questions", e);
      }
    });
  }

  @Override
  public void onInvalid() {
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast
          .makeText(getActivity(), R.string.msg_timeout, Toast.LENGTH_LONG)
          .show();
      }
    });
  }

  @Override
  public void onTimeout() {
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast
          .makeText(getActivity(), R.string.msg_invalid, Toast.LENGTH_LONG)
          .show();
      }
    });
  }

  private void buildClient() {
    SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(getActivity());
    OkHttpClient.Builder okb=new OkHttpClient.Builder();

    if (prefs.getBoolean("proxy", false)) {
      InetSocketAddress addr=
        new InetSocketAddress(prefs.getString("proxy_host", "10.0.2.2"),
          Integer.parseInt(prefs.getString("proxy_port", "8080")));

      okb.proxy(new Proxy(Proxy.Type.HTTP, addr));
    }

    try {
      TrustManagerBuilder tmb=new TrustManagerBuilder();

      if (prefs.getBoolean("log_cert_chain", false)) {
        tmb.withCertChainListener(new LogCertChainListener());
      }

      if (prefs.getBoolean("memo", false)) {
        File memoDir=new File(getActivity().getCacheDir(), "memo");

        memoDir.mkdirs();

        MemorizingTrustManager.Builder memoBuilder=
          new MemorizingTrustManager.Builder()
            .saveTo(memoDir, "sekrit".toCharArray());

        if (!prefs.getBoolean("tofu", false)) {
          memoBuilder.noTOFU();
        }

        memo=memoBuilder.build();

        if (prefs.getBoolean("net_sec_config", false)) {
          tmb.withConfig(getActivity(), R.xml.net_security_config).and();
        }

        tmb.add(memo);
      }
      else {
        tmb.withConfig(getActivity(), R.xml.net_security_config);
      }

      if (prefs.getBoolean("netcipher", false)) {
        StrongOkHttpClientBuilderEx
          .newInstance(getActivity(), tmb)
          .build(this);
      }
      else {
        OkHttp3Integrator.applyTo(tmb, okb);
        ok=okb.build();
        onOkHttpReady();
      }
    }
    catch (Exception e) {
      Log.e(getClass().getSimpleName(),
        "Exception trying to configure TrustManagerBuilder", e);
    }
  }

  private void onOkHttpReady() {
    picasso=
      new Picasso.Builder(getActivity())
        .downloader(new OkHttp3Downloader(ok))
        .listener(new Picasso.Listener() {
          @Override
          public void onImageLoadFailed(Picasso picasso, Uri uri,
                                        Exception exception) {
            Log.e(getClass().getSimpleName(),
              "Exception loading "+uri.toString(), exception);
          }
        })
        .build();
    restAdapter=
      new Retrofit.Builder()
        .baseUrl("https://api.stackexchange.com")
        .client(ok)
        .addConverterFactory(GsonConverterFactory.create())
        .build();
    load();
  }

  private void load() {
    StackOverflowInterface so=
      restAdapter.create(StackOverflowInterface.class);

    so.questions("android").enqueue(this);
  }

  class ItemsAdapter extends ArrayAdapter<Item> {
    ItemsAdapter(List<Item> items) {
      super(getActivity(), R.layout.row, R.id.title, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View row=super.getView(position, convertView, parent);
      Item item=getItem(position);
      ImageView icon=(ImageView)row.findViewById(R.id.icon);

      picasso
        .load(item.owner.profileImage)
        .fit()
        .centerCrop()
        .placeholder(R.drawable.owner_placeholder)
        .error(R.drawable.owner_error).into(icon);

      TextView title=(TextView)row.findViewById(R.id.title);

      title.setText(Html.fromHtml(getItem(position).title));

      return(row);
    }
  }
}
