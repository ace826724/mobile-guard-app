package com.mga.mobileguard

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log

/**
 * MGA - Mobile Guard App
 *
 * İlk TUN altyapısı.
 *
 * Bu sürüm:
 * - Android VPN servisini başlatır.
 * - TUN arayüzünü oluşturur.
 * - Varsayılan rota eklemez.
 * - Paket yönlendirme veya DNS filtreleme yapmaz.
 */
class MgaVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.mga.mobileguard.START_VPN"
        const val ACTION_STOP = "com.mga.mobileguard.STOP_VPN"

        private const val TAG = "MGA-VPN"

        const val STATUS_STOPPED = "STOPPED"
        const val STATUS_ENGINE_NOT_READY = "ENGINE_NOT_READY"
        const val STATUS_TUN_CREATED = "TUN_CREATED"

        private const val VPN_ADDRESS = "10.10.0.1"
        private const val VPN_PREFIX_LENGTH = 32
        private const val DNS_ADDRESS = "10.10.0.2"
        private const val MTU = 1500
    }

    @Volatile
    private var serviceRunning = false

    @Volatile
    private var tunInterface: ParcelFileDescriptor? = null

    @Volatile
    private var currentStatus = STATUS_STOPPED

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {
            ACTION_START -> startVpn()
            ACTION_STOP -> stopVpn()

            else -> {
                Log.w(TAG, "Bilinmeyen veya boş servis komutu.")
            }
        }

        return START_NOT_STICKY
    }

    @Synchronized
    private fun startVpn() {

        if (serviceRunning && tunInterface != null) {
            Log.i(TAG, "MGA TUN arayüzü zaten açık.")
            return
        }

        closeTunInterface()

        try {
            val builder = Builder()
                .setSession("MGA Mobile Guard")
                .setMtu(MTU)
                .addAddress(
                    VPN_ADDRESS,
                    VPN_PREFIX_LENGTH
                )
                .addDnsServer(DNS_ADDRESS)

            /*
             * ÖNEMLİ:
             * addRoute() çağrısı bilerek eklenmedi.
             * Böylece varsayılan internet trafiği
             * bu aşamada TUN arayüzüne yönlendirilmez.
             */

            val newInterface = builder.establish()

            if (newInterface == null) {
                currentStatus = STATUS_STOPPED
                serviceRunning = false

                Log.e(TAG, "TUN arayüzü oluşturulamadı.")
                stopSelf()
                return
            }

            tunInterface = newInterface
            serviceRunning = true
            currentStatus = STATUS_TUN_CREATED

            Log.i(TAG, "MGA TUN arayüzü oluşturuldu.")
            Log.i(TAG, "Paket işleme henüz etkin değil.")

        } catch (exception: Exception) {
            Log.e(TAG, "TUN oluşturulurken hata oluştu.", exception)

            closeTunInterface()
            currentStatus = STATUS_STOPPED
            serviceRunning = false

            stopSelf()
        }
    }

    @Synchronized
    private fun stopVpn() {

        serviceRunning = false

        closeTunInterface()

        currentStatus = STATUS_STOPPED

        Log.i(TAG, "MGA VPN servisi durduruldu.")

        stopSelf()
    }

    @Synchronized
    private fun closeTunInterface() {

        try {
            tunInterface?.close()
        } catch (exception: Exception) {
            Log.e(TAG, "TUN arayüzü kapatılırken hata oluştu.", exception)
        } finally {
            tunInterface = null
        }
    }

    override fun onRevoke() {
        Log.w(TAG, "VPN izni sistem tarafından kaldırıldı.")

        stopVpn()

        super.onRevoke()
    }

    override fun onDestroy() {

        serviceRunning = false
        closeTunInterface()

        currentStatus = STATUS_STOPPED

        Log.i(TAG, "MGA VPN servisi kapatıldı.")

        super.onDestroy()
    }
}
