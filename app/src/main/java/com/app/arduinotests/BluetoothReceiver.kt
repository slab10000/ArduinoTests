package com.app.arduinotests

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets

class BluetoothReceiver(socket: BluetoothSocket?) {

    private var inputStream: InputStream? = null
    private var isListening = false
    private val buffer = ByteArray(1024) // Buffer de lectura

    init {
        try {
            inputStream = socket?.inputStream
        } catch (e: IOException) {
            Log.e("Bluetooth", "❌ Error al obtener InputStream: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun startListening(onDataReceived: (String) -> Unit) {
        isListening = true
        CoroutineScope(Dispatchers.IO).launch {
            while (isListening) {
                try {
                    val bytesRead = inputStream?.read(buffer) ?: -1
                    if (bytesRead > 0) {
                        val receivedText = String(buffer, 0, bytesRead, StandardCharsets.UTF_8)
                        Log.d("Bluetooth", "📩 Datos recibidos: $receivedText")
                        onDataReceived(receivedText) // Callback con los datos recibidos
                    }
                } catch (e: IOException) {
                    Log.e("Bluetooth", "❌ Error al leer datos: ${e.message}")
                    stopListening()
                }
            }
        }
    }

    fun stopListening() {
        isListening = false
        inputStream?.close()
    }
}
