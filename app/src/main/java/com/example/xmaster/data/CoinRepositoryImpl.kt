package com.example.xmaster.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.bumptech.glide.Glide
import com.example.xmaster.data.database.AppDataBase
import com.example.xmaster.data.model.Coin
import com.example.xmaster.data.network.ApiService
import com.example.xmaster.data.network.ConnectivityDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject


class CoinRepositoryImpl @Inject constructor(
    val connectivityDispatcher: ConnectivityDispatcher,
    val appDataBase: AppDataBase,
    val context: Context,
    val apiService: ApiService
) : CoinRepository {


    override fun getAllCoinsFromDb(): LiveData<PagedList<Coin>> {
        return liveData {
            val dataFromDataBase =
                LivePagedListBuilder(appDataBase.coinsDao().getAllCoins(), 20).build()
            emitSource(dataFromDataBase)
        }
    }

    override suspend fun loadCoins() {
        if (!connectivityDispatcher.hasConnection()) {

        } else {
            loadCoinsFromNetwork()
        }
    }

    suspend fun loadCoinsFromNetwork() {
        withContext(Dispatchers.IO) {
            val response = apiService.getAll()
            if (response.isSuccessful) {
                response.body()?.coins?.let {
                    appDataBase.coinsDao().insert(it)
                    loadPictures()
                }
            }
        }
    }

    suspend fun loadPictures() {
        val coinsWithoutPicture =
            appDataBase.coinsDao().getAllCoinsList().filter { it.imageURL == null }
        coinsWithoutPicture.chunked(500)
            .forEach { listCoins ->
                val coinsWithoutPictureIDsString =
                    listCoins.map { it.id }.joinToString(separator = ",")
                val response = apiService.getPicture(coinsWithoutPictureIDsString)
                if (response.isSuccessful) {
                    response.body()?.images?.forEach { images ->
                        coinsWithoutPicture.find { coin ->
                            coin.id.toInt() === images.id.toInt()
                        }?.imageURL = images.logo;
                        Glide.with(context)
                            .load(images.logo)
                            .preload(500, 500)
                    }
                }
            }
    }
}
