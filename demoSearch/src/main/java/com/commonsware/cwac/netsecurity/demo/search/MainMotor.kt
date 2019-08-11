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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class MainViewState {
  object Initial : MainViewState()
  object Loading : MainViewState()
  data class Content(val items: List<SearchResultModel>) : MainViewState()
  data class Error(val throwable: Throwable) : MainViewState()
}

class MainMotor(private val repo: SearchRepository) : ViewModel() {
  private val _states =
    MutableLiveData<MainViewState>().apply { value = MainViewState.Initial }
  val states: LiveData<MainViewState> = _states

  fun search(expr: String) {
    _states.value = MainViewState.Loading

    viewModelScope.launch(Dispatchers.Main) {
      _states.value = try {
        MainViewState.Content(repo.search(expr))
      } catch (t: Throwable) {
        MainViewState.Error(t)
      }
    }
  }
}