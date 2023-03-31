package sheridan.bautisse.ble

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import sheridan.bautisse.ble.databinding.ActivityMainBinding
import java.util.*

const val DTAG = "MYAPP"
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var device: BluetoothDevice? = null

    private var scanning = false
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
               Log.d(DTAG, "Permission granted")
            }
        }

    private val multiplePermissionLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            results.forEach {
                Log.d("BOOBA", "${it.key}: ${it.value}")
            }
        }

    private val scanCallback: ScanCallback = object: ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val d: BluetoothDevice? = result?.device
            if (d != null) {
                binding.connect.isEnabled = false
                val name = result.scanRecord?.deviceName
//                Log.d(DTAG, "$name: ${d.address}")
                if (name == "Penguino") {
                    val btMgr = getSystemService(BluetoothManager::class.java)
                    val scanner = btMgr.adapter.bluetoothLeScanner
                    Log.d("TEM", "FOUND PENGU!")
                    device = result.device
                    binding.connect.isEnabled = true
                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.BLUETOOTH_SCAN
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    scanner.stopScan(this)

                }
            }
//            Log.d(DTAG, result?.scanRecord?.deviceName ?: "<Nameless device>")
            super.onScanResult(callbackType, result)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.d(DTAG, "HMMM")
            super.onScanFailed(errorCode)
        }
    }

    private var bluetoothLeService: BluetoothLeService? = null
    private val bluetoothServiceConn = object: ServiceConnection {
        private val TAG = "BluetoothServiceConnection"
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service Bound")
            bluetoothLeService = (service as BluetoothLeService.ServiceBinder).getService()
            Log.d(TAG, "BluetoothService populated: ${bluetoothLeService != null}")
            bluetoothLeService?.let { bluetooth ->
                // Connect here and do stuff here on service created
                if (!bluetooth.initialize()) {
                    Log.d(TAG, "Unable to initialize service")
                    finish()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service Unbound")
        }

    }

    private val gattUpdateCallback: BroadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    Toast.makeText(this@MainActivity, "Yooo! Im connected!", Toast.LENGTH_SHORT).show()
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    Toast.makeText(this@MainActivity, "Disconnected", Toast.LENGTH_SHORT).show()
                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    val services: List<BluetoothGattService?>? = bluetoothLeService?.getGattServices()
                    services?.let { s ->
                        s.forEach { service ->
                            Log.d("SERVICES", "${service?.uuid}: ${service.toString()}")
                        }
                    }
                }

            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ask permissions DO SOMETHING ABOUT THIS!!!
        multiplePermissionLauncher.launch(arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ))


        // Bind service. Unbind connection when binding fails
        Intent(this, BluetoothLeService::class.java).also {
            if (!bindService(it, bluetoothServiceConn, Context.BIND_AUTO_CREATE)) {
                unbindService(bluetoothServiceConn)
            }
        }
        
        binding.connect.isEnabled = false;


        binding.buttonScan.setOnClickListener {
            scanBle()
        }

        binding.connect.setOnClickListener {
//            if (device == null) return@setOnClickListener
            bluetoothLeService?.connect(device!!.address)
        }

        binding.buttonDisconnect.setOnClickListener {
            bluetoothLeService?.disconnect()
        }

        binding.buttonServices.setOnClickListener {
            bluetoothLeService?.getGattServices()?.forEach {
//                Log.d("SERVICES", "${it?.uuid.toString()}: ${it?.uuid == UUID.fromString("00001800-0000-1000-8000-00805f9b34fb")}")
            }
        }

        binding.ledOn.setOnClickListener {
            bluetoothLeService?.writeToPengu("ON")
        }

        binding.ledOff.setOnClickListener {
            bluetoothLeService?.writeToPengu("OFF")
        }

    }

    override fun onResume() {
        super.onResume()
        registerReceiver(gattUpdateCallback, makeGattUpdateIntentFilter())
        if (bluetoothLeService != null) {
            val result = bluetoothLeService!!.connect(device!!.address)
            Log.d(DTAG, "Connect request result=$result")
        }

    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(gattUpdateCallback)
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter? {
        return IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        }
    }

    private fun scanBle() {
        if (scanning) return
        val btMgr = getSystemService(BluetoothManager::class.java)
        val scanner = btMgr.adapter.bluetoothLeScanner

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN)

        }

//        scanner.startScan(filter, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_OPPORTUNISTIC).build(), scanCallback)
        scanner.startScan(scanCallback)
        Log.d(DTAG, "Scanning")
        scanning = true
        Handler(Looper.getMainLooper()).postDelayed({ ->
            Log.d(DTAG, "Stopping scan")
            scanner.stopScan(scanCallback)
            scanning = false

        }, 3000)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(bluetoothServiceConn)
    }


}