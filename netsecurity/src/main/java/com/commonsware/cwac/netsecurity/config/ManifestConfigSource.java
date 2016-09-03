/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.commonsware.cwac.netsecurity.config;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.util.Pair;
import java.util.Set;

/** @hide */
public class ManifestConfigSource implements ConfigSource {
    public static final String META_DATA_NETWORK_SECURITY_CONFIG =
      "android.security.net.config";
    private static final boolean DBG = true;
    private static final String LOG_TAG = "NetworkSecurityConfig";

    private final Object mLock = new Object();
    private final Context mContext;
    private final int mApplicationInfoFlags;
    private final int mTargetSdkVersion;
    // private final int mConfigResourceId;

    private ConfigSource mConfigSource;

    public ManifestConfigSource(Context context) {
        mContext = context;
        // Cache values because ApplicationInfo is mutable and apps do modify it :(
        ApplicationInfo info = context.getApplicationInfo();
        mApplicationInfoFlags = info.flags;
        mTargetSdkVersion = info.targetSdkVersion;
        // mConfigResourceId = info.networkSecurityConfigRes;
    }

    @Override
    public Set<Pair<Domain, NetworkSecurityConfig>> getPerDomainConfigs() {
        return getConfigSource().getPerDomainConfigs();
    }

    @Override
    public NetworkSecurityConfig getDefaultConfig() {
        return getConfigSource().getDefaultConfig();
    }

    private ConfigSource getConfigSource() {
        synchronized (mLock) {
            if (mConfigSource != null) {
                return mConfigSource;
            }

            ConfigSource source;

            /* MLM use meta-data approach from earlier N Developer Preview */

            ApplicationInfo info;
            try {
                info = mContext.getPackageManager().getApplicationInfo(mContext.getPackageName(),
                  PackageManager.GET_META_DATA);
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException("Failed to look up ApplicationInfo", e);
            }
            int configResourceId = 0;
            if (info != null && info.metaData != null) {
                configResourceId = info.metaData.getInt(META_DATA_NETWORK_SECURITY_CONFIG);
            }

            if (configResourceId != 0) {
                boolean debugBuild = (mApplicationInfoFlags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
                if (DBG) {
                    Log.d(LOG_TAG, "Using Network Security Config from resource "
                            + mContext.getResources().getResourceEntryName(configResourceId)
                            + " debugBuild: " + debugBuild);
                }
                source = new XmlConfigSource(mContext, configResourceId, debugBuild,
                        mTargetSdkVersion);
            } else {
                if (DBG) {
                    Log.d(LOG_TAG, "No Network Security Config specified, using platform default");
                }
                boolean usesCleartextTraffic =
                        (mApplicationInfoFlags & ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC) != 0;
                source = new DefaultConfigSource(usesCleartextTraffic, mTargetSdkVersion);
            }
            mConfigSource = source;
            return mConfigSource;
        }
    }

    private static final class DefaultConfigSource implements ConfigSource {

        private final NetworkSecurityConfig mDefaultConfig;

        public DefaultConfigSource(boolean usesCleartextTraffic, int targetSdkVersion) {
            mDefaultConfig = NetworkSecurityConfig.getDefaultBuilder(targetSdkVersion)
                    .setCleartextTrafficPermitted(usesCleartextTraffic)
                    .build();
        }

        @Override
        public NetworkSecurityConfig getDefaultConfig() {
            return mDefaultConfig;
        }

        @Override
        public Set<Pair<Domain, NetworkSecurityConfig>> getPerDomainConfigs() {
            return null;
        }
    }
}
