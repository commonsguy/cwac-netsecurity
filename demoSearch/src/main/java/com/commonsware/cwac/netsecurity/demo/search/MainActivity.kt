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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.commonsware.cwac.netsecurity.demo.search.databinding.ActivityMainBinding
import com.commonsware.cwac.netsecurity.demo.search.databinding.RowBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {
  private val motor: MainMotor by viewModel()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val binding = DataBindingUtil.setContentView<ActivityMainBinding>(
      this,
      R.layout.activity_main
    )
    val adapter = SearchAdapter(layoutInflater)

    binding.items.layoutManager = LinearLayoutManager(this)
    binding.items.adapter = adapter

    motor.states.observe(this) { state ->
      when (state) {
        MainViewState.Initial -> {
          binding.items.visibility = View.GONE
          binding.message.visibility = View.VISIBLE
          binding.message.setText(R.string.msg_initial)
          binding.progress.visibility = View.GONE
        }
        MainViewState.Loading -> {
          binding.items.visibility = View.GONE
          binding.message.visibility = View.GONE
          binding.progress.visibility = View.VISIBLE
        }
        is MainViewState.Content -> {
          binding.items.visibility = View.VISIBLE
          adapter.items = state.items
          binding.message.visibility = View.GONE
          binding.progress.visibility = View.GONE
        }
        is MainViewState.Error -> {
          binding.items.visibility = View.GONE
          binding.message.visibility = View.VISIBLE
          binding.message.text = state.throwable.localizedMessage
          binding.progress.visibility = View.GONE
        }
      }
    }

    binding.doSearch.setOnClickListener { motor.search(binding.search.text.toString()) }
  }
}

class SearchAdapter(private val inflater: LayoutInflater) :
  RecyclerView.Adapter<RowHolder>() {
  var items = listOf<SearchResultModel>()
    set(value) {
      field = value
      notifyDataSetChanged()
    }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
    RowHolder(RowBinding.inflate(inflater, parent, false))

  override fun getItemCount() = items.size

  override fun onBindViewHolder(holder: RowHolder, position: Int) {
    holder.bind(items[position])
  }
}

class RowHolder(private val binding: RowBinding) :
  RecyclerView.ViewHolder(binding.root) {
  fun bind(model: SearchResultModel) {
    binding.pageTitle.text = model.title
    binding.snippet.text =
      HtmlCompat.fromHtml(model.snippet, HtmlCompat.FROM_HTML_MODE_COMPACT)
  }
}