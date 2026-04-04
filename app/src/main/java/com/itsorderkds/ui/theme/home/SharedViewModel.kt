package com.itsorderkds.ui.theme.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.itsorderkds.data.model.BalanceItem

class SharedViewModel : ViewModel() {
    private val _balanceList = MutableLiveData<List<BalanceItem>>()
    val balanceList: LiveData<List<BalanceItem>>
        get() = _balanceList

    fun setBalanceList(balanceList: List<BalanceItem>) {
        _balanceList.value = balanceList
    }
}
