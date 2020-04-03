package com.example.xmaster.ui.market

import android.app.Application
import androidx.lifecycle.*
import androidx.paging.PagedList
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.xmaster.data.model.Coin
import com.example.xmaster.domain.coin.GetCoinsUseCase
import com.example.xmaster.domain.coin.LoadCoinsUseCase
import com.example.xmaster.utils.*
import com.example.xmaster.utils.map
import kotlinx.coroutines.*
import java.lang.Exception
import java.lang.NullPointerException
import javax.inject.Inject

class MarketViewModel @Inject constructor(
    getCoinsUseCase: GetCoinsUseCase,
    private val loadCoinsUseCase: LoadCoinsUseCase,
    private val errorHandler: ErrorHandler
) : ViewModel(), SwipeRefreshLayout.OnRefreshListener {

    private val _coins = MediatorLiveData<PagedList<Coin>>()
    val coins: LiveData<PagedList<Coin>>
        get() = _coins

    val isErrorHappen: LiveData<Boolean>

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean>
        get() = _loading

    private val _errorMessage = MutableLiveData<Event<String?>>()
    val errorMessage: LiveData<Event<String?>>
        get() = _errorMessage

    init {
        val coinsResult = getCoinsUseCase(Unit)
        _coins.addSource(coinsResult) {
            it.handleResult(blockSuccess = {
                this._coins.value = it
            })
        }
        isErrorHappen = coinsResult.map {
            (it is Result.Error && _coins.value.isNullOrEmpty())
                    || (it is Result.Success && it.data.isNullOrEmpty())
        }
        viewModelScope.launch {
            loadCoinsUseCase(Unit).handleResult(blockError =  {
                _errorMessage.value = Event(errorHandler.convertError(it))
            })
        }
    }

    override fun onRefresh() {
        viewModelScope.launch {
            _loading.value = true
            loadCoinsUseCase(Unit).handleResult(blockError = {
                _errorMessage.value = Event(errorHandler.convertError(it))
            })
            _loading.value = false
        }
    }


}
