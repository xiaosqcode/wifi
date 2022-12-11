package com.xsq.wifi

import android.net.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject

/**
 * Created by Xiaoshiquan on 2021/11/12.
 */
class WiFiNetworkRequest(
    private val connectivityManager: ConnectivityManager,
    private val wifiNetworkSpecifier: NetworkSpecifier,
    private val handle: Handler = Handler(Looper.myLooper() ?: Looper.getMainLooper()),
    private val requestTimeout: Int = 15000
): ConnectivityManager.NetworkCallback() {

    private val eventBus = PublishSubject.create<RequestEvent>()

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
//        val result = connectivityManager.bindProcessToNetwork(network)
        val result = ConnectivityManager.setProcessDefaultNetwork(network)
        if (result) {
            val l = connectivityManager.getLinkProperties(network)
            if (l != null) {
                val gatewayIp = l.routes.firstOrNull { r -> r.isDefaultRoute }?.gateway?.hostAddress
                if (!gatewayIp.isNullOrEmpty()) {
                    Log.d(LogTag.TAG, "Box gateway is $gatewayIp")
                    eventBus.onNext(RequestEvent.AVAILABLE)
                    return
                } else {
                    Log.w(LogTag.TAG, "Cannot get gateway address")
                }
            } else {
                Log.w(LogTag.TAG, "Cannot get LinkProperties")
            }
        } else {
            Log.w(LogTag.TAG, "bindProcessToNetwork is failed")
        }
        eventBus.onNext(RequestEvent.UNAVAILABLE)
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        Log.w(LogTag.TAG, "Box wifi network is lost")
        eventBus.onNext(RequestEvent.LOST)
    }

    override fun onUnavailable() {
        super.onUnavailable()
        Log.w(LogTag.TAG, "Box wifi network is unavailable")
        eventBus.onNext(RequestEvent.UNAVAILABLE)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun exec(): Observable<RequestEvent> {
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(wifiNetworkSpecifier)
            .build()

        connectivityManager.requestNetwork(networkRequest, this, handle, requestTimeout)
        return eventBus.doOnDispose {
                connectivityManager.unregisterNetworkCallback(this)
            }
    }

    enum class RequestEvent {
        AVAILABLE,
        LOST,
        UNAVAILABLE,
    }
}