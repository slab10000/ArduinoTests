package com.app.arduinotests

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.UUID

class MainActivity : ComponentActivity() {

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(30.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                RequestBluetoothPermissions()
                val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
                val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
                if (bluetoothAdapter == null) {
                    // Device doesn't support Bluetooth
                }
                //Open dialog asking to enable bluetooth (if it is not enabled)
                if (bluetoothAdapter?.isEnabled == false) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivity(enableBtIntent)
                }

                var hc05: BluetoothDevice? = null
                val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
                pairedDevices?.forEach { device ->
                    val deviceName = device.name
                    val deviceHardwareAddress = device.address // MAC address
                    if (deviceName == "HC-05") {
                        hc05 = device
                    }
                    println("deviceName = $deviceName deviceHardwareAddress = $deviceHardwareAddress")
                }
                val socket = hc05?.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                LaunchedEffect(Unit) {
                    initializeConnection(socket)
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonColors(
                        containerColor = androidx.compose.ui.graphics.Color.Green,
                        contentColor = androidx.compose.ui.graphics.Color.Black,
                        disabledContainerColor = androidx.compose.ui.graphics.Color.Gray,
                        disabledContentColor = androidx.compose.ui.graphics.Color.Black
                    ),
                    onClick = {writeData(socket, "1")}
                ) {
                    Text("Encender LED verde")
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonColors(
                        containerColor = androidx.compose.ui.graphics.Color.Red,
                        contentColor = androidx.compose.ui.graphics.Color.Black,
                        disabledContainerColor = androidx.compose.ui.graphics.Color.Gray,
                        disabledContentColor = androidx.compose.ui.graphics.Color.Black
                    ),
                    onClick = {writeData(socket, "2")}
                ) {
                    Text("Encender LED rojo")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun initializeConnection(socket: BluetoothSocket?){
        socket?.connect()
        readData(socket)
    }
}

suspend fun readData(socket: BluetoothSocket?) {
    val buffer = ByteArray(1024) //buffer store for the stream
    var numBytes: Int //bytes returned from read()
    var receivedText = ""
    withContext(Dispatchers.IO) {
        val inputStream = socket?.inputStream
        // Keep listening to the InputStream until an exception occurs.
        while (true) {
            // Read from the InputStream.
            numBytes = try {
                inputStream?.read(buffer) ?: -1

            } catch (e: IOException) {
                Log.d(TAG, "Input stream was disconnected", e)
                break
            }
            if (numBytes > 0){
                receivedText += String(buffer, 0, numBytes, StandardCharsets.UTF_8)
                println(receivedText)
            }
        }
    }
}

fun writeData (socket: BluetoothSocket?, text: String){
    val outputStream = socket?.outputStream
    outputStream?.write(text.toByteArray(StandardCharsets.UTF_8))
}

@Composable
fun RequestBluetoothPermissions() {
    val context = LocalContext.current

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsGranted ->
        if (permissionsGranted.containsValue(false)) {
            // Algún permiso fue denegado, manejarlo aquí
        }
    }

    LaunchedEffect(Unit) {
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!allGranted) {
            requestPermissionLauncher.launch(permissions)
        }
    }
}