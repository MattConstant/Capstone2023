package sheridan.bautisse.ble

import android.Manifest
import android.app.Service
import android.bluetooth.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import java.util.*

private const val TAG = "BluetoothLeService"
class BluetoothLeService(): Service() {
    private var btAdapter: BluetoothAdapter? = null
    private var connectionState: Int = STATE_DISCONNECTED
    private var bluetoothGatt: BluetoothGatt? = null

    private val gattCallback: BluetoothGattCallback = object: BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    broadcastUpdate(ACTION_GATT_CONNECTED)
                    connectionState = STATE_CONNECTED
                    if (ActivityCompat.checkSelfPermission(
                            this@BluetoothLeService,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {

                        return
                    }
                    bluetoothGatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    broadcastUpdate(ACTION_GATT_DISCONNECTED)
                    connectionState = STATE_CONNECTED
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS)
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
        }


        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (ActivityCompat.checkSelfPermission(
                    this@BluetoothLeService,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            Log.d(TAG, "Characteristic written: trying to read... [${bluetoothGatt?.readCharacteristic(characteristic)}]")
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, value, status)
            Log.d(TAG, "Result: ${value.toString(Charsets.UTF_8)} [$status]")
            Log.d(TAG, "Result: ${value.toUByteArray()} [$status]")
        }
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    inner class ServiceBinder: Binder() {
        fun getService(): BluetoothLeService {
            return this@BluetoothLeService
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return ServiceBinder()
    }

    override fun onCreate() {
        super.onCreate()
        // Check permissions
    }

    fun initialize(): Boolean {
        btAdapter = this.getSystemService(BluetoothManager::class.java).adapter
        if (btAdapter == null) {
            Log.e(TAG, "Unable to initialize adapter.")
            return false
        }
        Log.i(TAG, "Adapter initialized")
        return true
    }

    fun connect(address: String): Boolean {
        if (btAdapter == null) return false
        try {

            val device = btAdapter?.getRemoteDevice(address)

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
            }
            bluetoothGatt = device?.connectGatt(this, false, gattCallback)
        } catch (e: java.lang.IllegalArgumentException) {
            Log.e(TAG, "Device with provided address not found")
            return false
        }

        return true

    }

    fun getGattServices(): List<BluetoothGattService?>? {
        return bluetoothGatt?.services
    }

    fun writeToPengu(control: String) {
        val UUID_CONST = "-0000-1000-8000-00805f9b34fb"
        val chars = bluetoothGatt?.getService(UUID.fromString("0000aaa0$UUID_CONST"))
            ?.getCharacteristic(UUID.fromString("0000aaaa$UUID_CONST"))

        // Write characteristic here. wish me luck
        chars?.let {
            val packet = control.toByteArray(Charsets.UTF_8)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            Log.d(TAG, "Trying to write: ${bluetoothGatt
                ?.writeCharacteristic(it, packet, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)}")

            Log.d(TAG, "Tried to write: ${packet.toString(Charsets.UTF_8)}")

        }
    }

    fun disconnect() {
        bluetoothGatt?.let {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            it.close()
            bluetoothGatt = null
            broadcastUpdate(ACTION_GATT_DISCONNECTED)
        }
    }

    companion object {
        const val ACTION_GATT_CONNECTED =
            "sheridan.bautisse.ble.BluetoothLeService.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED =
            "sheridan.bautisse.ble.BluetoothLeService.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED =
            "sheridan.bautisse.ble.BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED"


        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTED = 2

    }





}