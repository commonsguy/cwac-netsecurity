/*
  Copyright (c) 2019 CommonsWare, LLC
  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
  by applicable law or agreed to in writing, software distributed under the
  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
  OF ANY KIND, either express or implied. See the License for the specific
  language governing permissions and limitations under the License.
*/

package com.commonsware.cwac.netsecurity.demo.search

import android.app.Application
import com.commonsware.cwac.netsecurity.OkHttp3Integrator
import okhttp3.OkHttpClient
import org.koin.android.ext.android.startKoin
import org.koin.androidx.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module
import com.commonsware.cwac.netsecurity.TrustManagerBuilder
import org.koin.android.ext.koin.androidContext


class KoinApp : Application() {
  private val koinModule = module {
    single {
      val tmb = TrustManagerBuilder().withManifestConfig(androidContext())
      val okBuilder = OkHttpClient.Builder()

      OkHttp3Integrator.applyTo(tmb, okBuilder);

      okBuilder.build()
    }
    single { SearchRepository(get()) }
    viewModel { MainMotor(get()) }
  }

  override fun onCreate() {
    super.onCreate()

    startKoin(this, listOf(koinModule))
  }
}