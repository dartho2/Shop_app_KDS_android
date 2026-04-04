package com.itsorderkds.ui.theme

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.itsorderkds.databinding.FragmentPermissionsBinding

class PermissionsFragment : Fragment() {

    private var _binding: FragmentPermissionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPermissionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkPermissions()
    }

    private fun checkPermissions() {
        val permissionsToCheck = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.FOREGROUND_SERVICE ,
            android.Manifest.permission.RECEIVE_BOOT_COMPLETED ,
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )

        val permissionsStatus = permissionsToCheck.map { permission ->
            permission to (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED)
        }.toMap()

        val permissionsText = permissionsStatus.entries.joinToString("\n") { (permission, isGranted) ->
            "$permission: ${if (isGranted) "Granted" else "Denied"}"
        }

        Log.d("PermissionsFragment", "Permissions status: $permissionsText")

      binding.textPermissionsStatus.text = permissionsText
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
