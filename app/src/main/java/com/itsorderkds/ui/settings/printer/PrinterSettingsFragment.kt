//package com.itsorderkds.ui.settings.printer
//
//import android.Manifest
//import android.content.pm.PackageManager
//import android.os.Build
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Toast
//import androidx.appcompat.app.AlertDialog
//import androidx.core.content.ContextCompat
//import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
//import com.itsorderkds.R
//import com.itsorderkds.databinding.FragmentPrinterSettingsBinding
//import com.itsorderkds.util.AppPrefs
//import com.itsorderkds.ui.theme.base.BaseFragment
//import dagger.hilt.android.AndroidEntryPoint
//
//class PrinterSettingsFragment :
//    BaseFragment<FragmentPrinterSettingsBinding>() {
//
//    /* -------- uprawnienia BT (Android 12+) -------- */
//    private val btPerms =
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) arrayOf(
//            Manifest.permission.BLUETOOTH_CONNECT,
//            Manifest.permission.BLUETOOTH_SCAN
//        ) else emptyArray()
//
//    private val reqCode = 1001
//
//    /* ---------------- lifecycle UI ---------------- */
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        binding.btnBluetoothPrinters.setOnClickListener { pickBluetoothPrinter() }
//        binding.btnTestPrint.setOnClickListener         { doTestPrint() }
//
//        ensureBtPermission()
//        refreshUi()
//    }
//
//    /* ---------- wybór drukarki ---------- */
//    private fun pickBluetoothPrinter() {
//        if (!hasBtPermission()) {
//            Toast.makeText(requireContext(),
//                "Brak uprawnień Bluetooth", Toast.LENGTH_SHORT).show()
//            ensureBtPermission(); return
//        }
//
//        // try/catch zabezpiecza przed SecurityException → Lint milczy
//        val printers = try {
//            BluetoothPrintersConnections().getList()
//        } catch (e: SecurityException) {
//            Toast.makeText(requireContext(),
//                "Brak uprawnień Bluetooth", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        if (printers.isNullOrEmpty()) {
//            Toast.makeText(requireContext(),
//                "Brak sparowanych drukarek BT", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val names = printers.map { it.device.name ?: it.device.address }
//        AlertDialog.Builder(requireContext())
//            .setTitle("Wybierz drukarkę Bluetooth")
//            .setItems(names.toTypedArray()) { _, which ->
//                val conn = printers[which]
//                AppPrefs.savePrinter("bluetooth", conn.device.address)   // zapis
//                refreshUi()
//                PrinterTestHelper.printTest(requireContext(), conn)       // test
//            }
//            .show()
//    }
//
//    /* ---------- test wydruku ---------- */
//    private fun doTestPrint() {
//        val connection = PrinterManager.getConnection(requireContext())
//        if (connection == null) {
//            Toast.makeText(requireContext(),
//                "Brak skonfigurowanej drukarki", Toast.LENGTH_SHORT).show()
//            return
//        }
//        PrinterTestHelper.printTest(requireContext(), connection)
//    }
//
//    /* ---------- helpers – permissions ---------- */
//    private fun hasBtPermission(): Boolean =
//        btPerms.all {
//            ContextCompat.checkSelfPermission(
//                requireContext(), it
//            ) == PackageManager.PERMISSION_GRANTED
//        }
//
//    private fun ensureBtPermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBtPermission()) {
//            requestPermissions(btPerms, reqCode)
//        }
//    }
//
//    override fun onRequestPermissionsResult(
//        code: Int, perms: Array<out String>, res: IntArray
//    ) {
//        super.onRequestPermissionsResult(code, perms, res)
//        if (code == reqCode) {
//            val ok = res.all { it == PackageManager.PERMISSION_GRANTED }
//            Toast.makeText(requireContext(),
//                if (ok) "Bluetooth: uprawnienia przyznane"
//                else   "Bluetooth: odmówiono uprawnień",
//                Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    /* ---------- UI state ---------- */
//    private fun refreshUi() {
//        val (_, id) = AppPrefs.getPrinter(requireContext())
//        binding.tvSelectedPrinter.text =
//            id ?: getString(R.string.no_printer_selected)
//    }
//
//    /* ---------- ViewBinding ---------- */
//    override fun getFragmentBinding(
//        inflater: LayoutInflater,
//        container: ViewGroup?
//    ) = FragmentPrinterSettingsBinding.inflate(inflater, container, false)
//}
