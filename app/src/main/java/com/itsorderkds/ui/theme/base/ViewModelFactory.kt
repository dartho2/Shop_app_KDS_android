package com.itsorderkds.ui.theme.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.itsorderkds.data.repository.AuthRepository
import com.itsorderkds.data.repository.BaseRepository
import com.itsorderkds.data.repository.ProductsRepository
import com.itsorderkds.ui.auth.AuthViewModel
import com.itsorderkds.ui.product.ProductsViewModel

@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val repository: BaseRepository
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> AuthViewModel(repository as AuthRepository) as T
            modelClass.isAssignableFrom(ProductsViewModel::class.java) -> ProductsViewModel(repository as ProductsRepository) as T
            else -> throw IllegalArgumentException("ViewModelClass Not Found")
        }
    }

}
