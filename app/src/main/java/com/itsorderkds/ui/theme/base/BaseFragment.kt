package com.itsorderkds.ui.theme.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.itsorderkds.data.preferences.UserPreferences
import com.itsorderkds.data.repository.BaseRepository

abstract class BaseFragment< B : ViewBinding> : Fragment() {

    protected lateinit var binding: B

    // te obiekty zostaną stworzone dopiero, gdy ktoś pierwszy raz je użyje,
    // a w tym momencie fragment na pewno będzie dołączony
    protected val userPreferences   by lazy { UserPreferences(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = getFragmentBinding(inflater, container)
        return binding.root
    }

    abstract fun getFragmentBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): B
}