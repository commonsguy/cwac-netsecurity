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

import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class Entry(
  val title: String,
  val url: String
)

data class SearchResultModel(
  val title: String,
  val snippet: String
)

@JsonClass(generateAdapter = true)
data class SearchResult(
  val snippets: List<String>,
  val sectionTitle: String
) {
  fun toModel() = SearchResultModel(
    title = sectionTitle,
    snippet = snippets.first().orEmpty()
  )
}

interface SearchApi {
  @GET("/app/public/booksearch.json")
  suspend fun search(@Query("search") search: String): List<SearchResult>
}

class SearchRepository(ok: OkHttpClient) {
  private val retrofit = Retrofit.Builder()
    .baseUrl("https://wares.commonsware.com")
    .client(ok)
    .addConverterFactory(MoshiConverterFactory.create())
    .build()
  private val adapter = retrofit.create(SearchApi::class.java)

  suspend fun search(expr: String) = adapter.search(expr).map { it.toModel() }
}