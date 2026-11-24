
package com.mga.mobileguard

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log

class MgaVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.mga.mobileguard.START_VPN"
        const val ACTION_STOP = "com.mga.mobileguard.STOP_VPN"

        private const val TAG = "MGA-VPN"
    }

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {
            ACTION_START -> startVpn()
            ACTION_STOP -> stopVpn()
        }

        return START_NOT_STICKY
    }

    private fun startVpn() {
        if (vpnInterface != null) {
            Log.i(TAG, "VPN servisi zaten başlatılmış.")
            return
        }

        try {
            val builder = Builder()
                .setSession("MGA Mobile Guard")
                .addAddress("10.10.10.1", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("10.10.10.2")
                .setMtu(1500)

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                Log.e(TAG, "VPN arayüzü oluşturulamadı.")
                stopSelf()
                return
            }

            Log.i(TAG, "VPN arayüzü oluşturuldu.")

        } catch (exception: Exception) {
            Log.e(TAG, "VPN başlatma hatası.", exception)
            stopVpn()
        }
    }

    private fun stopVpn() {
        try {
            vpnInterface?.close()
        } catch (exception: Exception) {
            Log.e(TAG, "VPN kapatma hatası.", exception)
        } finally {
            vpnInterface = null
            stopSelf()
        }
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
