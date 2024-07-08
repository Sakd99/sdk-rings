package com.qcwireless.sdksample

import android.Manifest
import android.bluetooth.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission.BLUETOOTH_CONNECT
import com.hjq.permissions.XXPermissions
import com.oudmon.ble.base.bluetooth.BleOperateManager
import com.oudmon.ble.base.bluetooth.DeviceManager
import com.oudmon.ble.base.bluetooth.ListenerKey
import com.oudmon.ble.base.communication.CommandHandle
import com.oudmon.ble.base.communication.LargeDataHandler
import com.oudmon.ble.base.communication.bigData.AlarmNewEntity
import com.oudmon.ble.base.communication.entity.AlarmEntity
import com.oudmon.ble.base.communication.req.SetDrinkAlarmReq
import com.oudmon.ble.base.communication.responseImpl.DeviceNotifyListener
import com.oudmon.ble.base.communication.rsp.BaseRspCmd
import com.oudmon.ble.base.communication.rsp.DeviceNotifyRsp
import com.oudmon.ble.base.util.DateUtil
import com.qcwireless.sdksample.databinding.ActivityMainBinding
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val test = Test()
    private lateinit var myDeviceNotifyListener: MyDeviceNotifyListener
    private var bluetoothGatt: BluetoothGatt? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        setOnClickListener(binding.tvScan) {
            requestLocationPermission(this@MainActivity, PermissionCallback())
        }

        myDeviceNotifyListener = MyDeviceNotifyListener()

        binding.run {
            tvName.text = DeviceManager.getInstance().deviceName
            val connect = BleOperateManager.getInstance().isConnected
            tvConnect.text = if (connect) "Connected" else getString(R.string.disconnected)

            setOnClickListener(
                btnSetTime,
                btnFindWatch,
                btnBattery,
                btnTimeUnit,
                btnMsgTest,
                btnAlarmRead,
                btnAlarmWrite,
                btnSyncBloodoxygen,
                btnTakePicture,
                btnMusic,
                btnCall,
                btnTarget,
                tvReconnect,
                tvDisconnect,
                btnWatchFace,
                btnSleep,
                btnSleepCalc,
                heart1,
                heart2,
                heart3,
                push1,
                push2,
                temperature1,
                temperature2,
                userprofile1,
                userprofile2,
                addListener1,
                addListener2,
                addListener3,
                ota,
                bt,
                btnDrink,
                btnSport,
                pressure1
            ) {
                when (this) {
                    tvReconnect -> {
                        reconnectDevice()
                    }
                    tvDisconnect -> {
                        disconnectDevice()
                    }
                    btnSetTime -> {
                        test.setTime()
                    }
                    btnFindWatch -> {
                        test.findPhone()
                    }
                    btnBattery -> {
                        test.battery
                    }
                    btnTimeUnit -> {
                        test.readMetricAndTimeFormat()
                    }
                    btnMsgTest -> {
                        test.pushMsg()
                    }
                    btnAlarmRead -> {
                        test.readAlarm()
                    }
                    btnAlarmWrite -> {
                        val list = mutableListOf<AlarmNewEntity.AlarmBean>()
                        val bean = AlarmNewEntity.AlarmBean()
                        bean.content = "1234"
                        bean.min = DateUtil().todayMin + 1
                        bean.repeatAndEnable = 0xff
                        bean.alarmLength = 4 + bean.content.encodeToByteArray().size
                        list.add(bean)
                        val entity = AlarmNewEntity()
                        entity.total = 1
                        entity.data = list
                        test.writeAlar(entity)
                    }
                    btnSyncBloodoxygen -> {
                        test.bloodOxygen()
                    }
                    btnTakePicture -> {
                        test.takePicture()
                    }
                    btnMusic -> {
                        test.music()
                    }
                    btnCall -> {
                        test.call()
                    }
                    btnTarget -> {
                        test.setTarget()
                    }
                    btnWatchFace -> {
                        test.watchFace()
                    }
                    btnSleep -> {
                        test.sleep()
                    }
                    btnSleepCalc -> {
                        test.contactList()
                    }
                    heart1 -> {
                        test.heartSync()
                    }
                    heart2 -> {
                        test.bp()
                    }
                    heart3 -> {
                        test.spo2()
                    }
                    pressure1 -> {
                        test.pressure()
                    }
                    push1 -> {
                        test.push1()
                    }
                    push2 -> {
                        test.push2()
                    }
                    temperature1 -> {
                        test.registerTempCallback()
                        test.syncAutoTemperature()
                    }
                    temperature2 -> {
                        test.syncManual()
                    }
                    userprofile1 -> {
                        test.setUserProfile()
                    }
                    userprofile2 -> {
                        test.getUserProfile()
                    }
                    addListener1 -> {
                        BleOperateManager.getInstance().addOutDeviceListener(ListenerKey.Heart, myDeviceNotifyListener)
                    }
                    addListener2 -> {
                        BleOperateManager.getInstance().removeNotifyListener(ListenerKey.Heart)
                    }
                    addListener3 -> {
                        BleOperateManager.getInstance().removeNotifyListener(ListenerKey.All)
                    }
                    ota -> {
                        startKtxActivity<OtaActivity>()
                    }
                    bt -> {
                    }
                    btnDrink -> {
                        for (index in 0..6) {
                            val entity = AlarmEntity(index, 1, index + 5, index * 5, 0x80.toByte())
                            CommandHandle.getInstance().executeReqCmdNoCallback(
                                SetDrinkAlarmReq(entity)
                            )
                        }
                    }
                    btnSport -> {
                        binding.tvHard.text = MyApplication.getInstance.hardwareVersion
                        binding.tvFir.text = MyApplication.getInstance.firmwareVersion
                    }
                }
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.i("MainActivity", "Connected to GATT server.")
                gatt.discoverServices()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.i("MainActivity", "Disconnected from GATT server. Status: $status")
                runOnUiThread {
                    binding.tvConnect.text = getString(R.string.disconnected)
                }
            } else {
                Log.i("MainActivity", "Connection state changed. New state: $newState, Status: $status")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("MainActivity", "Services discovered successfully.")
                enableNotifications(gatt)
            } else {
                Log.w("MainActivity", "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            Log.i("MainActivity", "Characteristic changed: ${characteristic.uuid}")
            when (characteristic.uuid) {
                UUID.fromString(STEP_COUNT_CHARACTERISTIC_UUID) -> {
                    val steps = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0)
                    runOnUiThread { updateSteps(steps) }
                }
                UUID.fromString(SPO2_CHARACTERISTIC_UUID) -> {
                    val spo2 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                    runOnUiThread { updateSpO2(spo2) }
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("MainActivity", "Characteristic read: ${characteristic.uuid}")
                when (characteristic.uuid) {
                    UUID.fromString(STEP_COUNT_CHARACTERISTIC_UUID) -> {
                        val steps = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0)
                        runOnUiThread { updateSteps(steps) }
                    }
                    UUID.fromString(SPO2_CHARACTERISTIC_UUID) -> {
                        val spo2 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                        runOnUiThread { updateSpO2(spo2) }
                    }
                }
            } else {
                Log.w("MainActivity", "onCharacteristicRead received: $status for ${characteristic.uuid}")
            }
        }
    }

    private fun enableNotifications(gatt: BluetoothGatt) {
        val stepCharacteristic = gatt.getService(UUID.fromString(STEP_COUNT_SERVICE_UUID))
            ?.getCharacteristic(UUID.fromString(STEP_COUNT_CHARACTERISTIC_UUID))
        val spo2Characteristic = gatt.getService(UUID.fromString(SPO2_SERVICE_UUID))
            ?.getCharacteristic(UUID.fromString(SPO2_CHARACTERISTIC_UUID))

        gatt.setCharacteristicNotification(stepCharacteristic, true)
        gatt.setCharacteristicNotification(spo2Characteristic, true)

        stepCharacteristic?.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG_UUID))?.let { descriptor ->
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }
        spo2Characteristic?.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG_UUID))?.let { descriptor ->
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }
    }

    private fun updateSteps(steps: Int) {
        binding.tvSteps.text = "Steps: $steps"
    }

    private fun updateSpO2(spo2: Int) {
        binding.tvSpo2.text = "SpO2: $spo2%"
    }

    private fun reconnectDevice() {
        val deviceAddress = DeviceManager.getInstance().deviceAddress
        if (deviceAddress != null) {
            val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
            bluetoothGatt = device.connectGatt(this, false, gattCallback)
        } else {
            Log.w("MainActivity", "Device address is null, cannot reconnect")
        }
    }

    private fun disconnectDevice() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        binding.tvConnect.text = getString(R.string.disconnected)
    }

    override fun onResume() {
        super.onResume()
        try {
            if (!BluetoothUtils.isEnabledBluetooth(this)) {
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                }
                startActivityForResult(intent, 300)
            }
        } catch (e: Exception) {
        }
        if (!hasBluetooth(this)) {
            requestBluetoothPermission(this, BluetoothPermissionCallback())
        }

        binding.tvName.text = DeviceManager.getInstance().deviceName
        requestAllPermission(this, OnPermissionCallback { permissions, all ->  })
    }

    inner class PermissionCallback : OnPermissionCallback {
        override fun onGranted(permissions: MutableList<String>, all: Boolean) {
            if (!all) {
                // Handle partial permission grant
            } else {
                startKtxActivity<DeviceBindActivity>()
            }
        }

        override fun onDenied(permissions: MutableList<String>, never: Boolean) {
            super.onDenied(permissions, never)
            if (never) {
                XXPermissions.startPermissionActivity(this@MainActivity, permissions)
            }
        }
    }

    inner class BluetoothPermissionCallback : OnPermissionCallback {
        override fun onGranted(permissions: MutableList<String>, all: Boolean) {
            if (!all) {
                // Handle partial permission grant
            }
        }

        override fun onDenied(permissions: MutableList<String>, never: Boolean) {
            super.onDenied(permissions, never)
            if (never) {
                XXPermissions.startPermissionActivity(this@MainActivity, permissions)
            }
        }
    }

    inner class AllPermissionCallback : OnPermissionCallback {
        override fun onGranted(permissions: MutableList<String>, all: Boolean) {
            // Handle all permissions granted
        }

        override fun onDenied(permissions: MutableList<String>, never: Boolean) {
            super.onDenied(permissions, never)
            // Handle permissions denied
        }
    }

    inner class MyDeviceNotifyListener : DeviceNotifyListener() {
        override fun onDataResponse(resultEntity: DeviceNotifyRsp?) {
            if (resultEntity!!.status == BaseRspCmd.RESULT_OK) {
                BleOperateManager.getInstance().removeOthersListener()
                when (resultEntity.dataType) {
                    1 -> {
                        // Handle heart rate data
                    }
                    2 -> {
                        // Handle blood pressure data
                    }
                    3 -> {
                        // Handle blood oxygen data
                        val spo2 = resultEntity.dataType.toInt() // Adjust as necessary
                        updateSpO2(spo2)
                    }
                    4 -> {
                        // Handle step count data
                        val steps = resultEntity.dataType.toInt() // Adjust as necessary
                        updateSteps(steps)
                    }
                    5 -> {
                        // Handle temperature data
                    }
                }
            }
        }
    }

    companion object {
        private const val STEP_COUNT_SERVICE_UUID = "0000181D-0000-1000-8000-00805f9b34fb"
        private const val STEP_COUNT_CHARACTERISTIC_UUID = "00002A5B-0000-1000-8000-00805f9b34fb"
        private const val SPO2_SERVICE_UUID = "00001822-0000-1000-8000-00805f9b34fb"
        private const val SPO2_CHARACTERISTIC_UUID = "00002A5F-0000-1000-8000-00805f9b34fb"
        private const val CLIENT_CHARACTERISTIC_CONFIG_UUID = "00002902-0000-1000-8000-00805f9b34fb"
    }
}
