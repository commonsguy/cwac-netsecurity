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

package com.commonsware.cwac.netsecurity.demo;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.view.Menu;
import android.view.MenuItem;
import java.io.File;

public class ConfigActivity extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (getFragmentManager().findFragmentById(android.R.id.content)==null) {
      getFragmentManager().beginTransaction()
        .add(android.R.id.content, new Prefs())
        .commit();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.actions, menu);

    return(super.onCreateOptionsMenu(menu));
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId()==R.id.run) {
      startActivity(new Intent(this, DemoActivity.class));

      return(true);
    }
    else if (item.getItemId()==R.id.clear) {
      new Thread() {
        @Override
        public void run() {
          delete(new File(getCacheDir(), "memo"));
        }
      }.start();
    }

    return(super.onOptionsItemSelected(item));
  }

  // inspired by http://pastebin.com/PqJyzQUx

  /**
   * Recursively deletes a directory and its contents.
   *
   * @param f The directory (or file) to delete
   * @return true if the delete succeeded, false otherwise
   */
  public static boolean delete(File f) {
    if (f.isDirectory()) {
      for (File child : f.listFiles()) {
        if (!delete(child)) {
          return(false);
        }
      }
    }

    return(f.delete());
  }

  public static class Prefs extends PreferenceFragment
    implements Preference.OnPreferenceChangeListener {
    SwitchPreference proxy;
    SwitchPreference netcipher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      addPreferencesFromResource(R.xml.prefs);

      EditTextPreference pref=(EditTextPreference)findPreference("proxy_host");

      onPreferenceChange(pref, pref.getText());
      pref.setOnPreferenceChangeListener(this);

      pref=(EditTextPreference)findPreference("proxy_port");
      onPreferenceChange(pref, pref.getText());
      pref.setOnPreferenceChangeListener(this);

      netcipher=(SwitchPreference)findPreference("netcipher");
      netcipher.setOnPreferenceChangeListener(this);
      proxy=(SwitchPreference)findPreference("proxy");
      proxy.setOnPreferenceChangeListener(this);

      onPreferenceChange(netcipher, netcipher.isChecked());
      onPreferenceChange(proxy, proxy.isChecked());
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object o) {
      if (pref instanceof EditTextPreference) {
        pref.setSummary(o.toString());
      }
      else if (pref==proxy) {
        Boolean value=(Boolean)o;

        if (value) netcipher.setChecked(false);
        netcipher.setEnabled(!value);
      }
      else {
        Boolean value=(Boolean)o;

        if (value) proxy.setChecked(false);
        proxy.setEnabled(!value);
      }

      return(true);
    }
  }
}
