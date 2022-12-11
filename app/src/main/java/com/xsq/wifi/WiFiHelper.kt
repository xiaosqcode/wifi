package com.xsq.wifi

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.*
import android.net.wifi.WifiManager.SCAN_RESULTS_AVAILABLE_ACTION
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresPermission
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import java.math.BigInteger
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteOrder

/**
 * WiFi连接助手
 *
 * 提供的能力
 * - 功能开关
 * - 扫描
 * - 获取已保存的配置
 * - 连接信号
 *
 * @param context 应用上下文
 * @param filters 扫描过滤器
 *
 * Created by Xiaoshiquan on 2021/4/19.
 */
class WiFiHelper constructor(
    context: Context,
    vararg filters: WiFiSSIDFilter
) {

    private var context: Context? = null
    private val wifiManager: WifiManager
    private val connectivityManager: ConnectivityManager
    private var scanReceiver: WifiScanReceiver? = null
    private val filterList = ArrayList<WiFiSSIDFilter>()
    private var scanning = false
    private var scanDisposable: Disposable? = null
    private var wiFiNetworkRequest: WiFiNetworkRequest? = null
    private var wiFiNetworkRequestDisposable: Disposable? = null

    init {
        this.context = context.applicationContext
        wifiManager = this.context!!.getSystemService(Context.WIFI_SERVICE) as WifiManager
        connectivityManager =
            this.context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (filters.isNotEmpty()) {
            filterList.addAll(filters)
        }
    }

    /**
     * 销毁助手
     */
    fun destroy() {
        this.context = null
    }

    /**
     * WiFi管理器
     */
    fun wifiManager() = wifiManager

    /**
     * 网络连接管理器
     */
    fun connectivityManager() = connectivityManager

    /**
     * 判断模块是否开启
     */
    fun isEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }

    /**
     * 开启模块
     */
    fun enable(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            this.context?.startActivity(Intent(Settings.Panel.ACTION_WIFI))
            return true
        }
        return wifiManager.setWifiEnabled(true)
    }

    /**
     * 关闭模块
     */
    fun disable(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            this.context?.startActivity(Intent(Settings.Panel.ACTION_WIFI))
            return true
        }
        return wifiManager.setWifiEnabled(false)
    }

    /**
     * 开始扫描
     *
     * 可以调用 [stopScan] 停止当前扫描
     */
    fun scan(): Observable<ScanResult> {
        val scanFinishedListener = PublishSubject.create<Boolean>()
        scanReceiver = WifiScanReceiver(scanFinishedListener)
        val intentFilter = IntentFilter(SCAN_RESULTS_AVAILABLE_ACTION)
        this.context?.registerReceiver(scanReceiver, intentFilter)
        return scanFinishedListener.subscribeOn(Schedulers.computation())
            .flatMap { Observable.fromIterable(wifiManager.scanResults) }
            .filter { scanResult ->
                if (filterList.isEmpty()) {
                    return@filter true
                }
                for (filter in filterList) {
                    if (filter.doFilter(scanResult.SSID)) {
                        return@filter true
                    }
                }
                return@filter false
            }
            .doOnSubscribe { d ->
                wifiManager.startScan()
                scanDisposable = d
                scanning = true
            }
            .doOnError {
                stopScan()
            }
    }

    /**
     * 停止扫描
     */
    fun stopScan() {
        if (scanning && scanReceiver != null) {
            this.context?.unregisterReceiver(scanReceiver)
            scanReceiver = null
        }
        scanning = false
        scanDisposable?.dispose()
        scanDisposable = null
    }

    /**
     * 获取已保存的配置
     */
    @RequiresPermission(allOf = [permission.ACCESS_FINE_LOCATION, permission.ACCESS_WIFI_STATE])
    fun getConnectHistory(): List<WifiConfiguration> {
        return wifiManager.configuredNetworks
            .filter { scanResult ->
                if (filterList.isEmpty()) {
                    return@filter true
                }
                for (filter in filterList) {
                    if (filter.doFilter(scanResult.SSID)) {
                        return@filter true
                    }
                }

                return@filter false
            }
    }

    /**
     * 连接指定的WiFi
     *
     * @param ssid 信号名称
     * @param password 连接密码
     * @param type 信号安全协议类别（见：[WIFI_CIPHER_NO_PASS], [WIFI_CIPHER_WEP], [WIFI_CIPHER_WPA]）
     * @param connectTimeout 信号连接超时设置
     * @return 取消订阅则会断开连接
     */
    @JvmOverloads
    fun connect(
        ssid: String,
        password: String,
        type: Int,
        connectTimeout: Int = 15000
    ): Observable<Boolean> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android Q及其以上版本
            val wifiNetworkSpecifier = when (type) {
                WIFI_CIPHER_NO_PASS -> {
                    WifiNetworkSpecifier.Builder()
                        .setSsid(ssid)
                        .build()
                }
                WIFI_CIPHER_WEP -> {
                    val wec = WifiEnterpriseConfig()
                    wec.password = password
                    WifiNetworkSpecifier.Builder()
                        .setSsid(ssid)
                        .setWpa2EnterpriseConfig(wec)
                        .build()
                }
                else -> {
                    WifiNetworkSpecifier.Builder()
                        .setSsid(ssid)
                        .setWpa2Passphrase(password)
                        .build()
                }
            }

            if (wiFiNetworkRequestDisposable != null) {
                wiFiNetworkRequestDisposable?.dispose()
                wiFiNetworkRequestDisposable = null
            }
            if (wiFiNetworkRequest == null) {
                wiFiNetworkRequest = WiFiNetworkRequest(
                    connectivityManager = connectivityManager,
                    wifiNetworkSpecifier = wifiNetworkSpecifier,
                    requestTimeout = connectTimeout
                )
            }
            return wiFiNetworkRequest!!.exec()
                .doOnSubscribe { d -> wiFiNetworkRequestDisposable = d }
                .map { e -> e == WiFiNetworkRequest.RequestEvent.AVAILABLE }
        } else {
            // 小于Android Q的版本
            var config = isExist(ssid)
            val networkId = if (config == null) {
                config = createWifiConfig(ssid, password, WIFI_CIPHER_WPA)
                wifiManager.addNetwork(config)
            } else {
                config.networkId
            }
            return connect(networkId)
        }
    }

    private var currentNetworkId = -1

    /**
     * 使用指定配置进行连接
     *
     * 取消订阅则会断开连接
     */
    fun connect(networkId: Int): Observable<Boolean> {
        currentNetworkId = networkId
        return Observable.create<Boolean> { emitter ->
            wifiManager.run {
                val enableNetwork = enableNetwork(currentNetworkId, true)
                emitter.onNext(enableNetwork)
            }
        }
            .doOnDispose {
                wifiManager.disableNetwork(currentNetworkId)
            }
    }

    /**
     * 创建配置对象，只适用于低于 [Build.VERSION_CODES.Q] 版本的环境使用
     */
    fun createWifiConfig(ssid: String, password: String, type: Int): WifiConfiguration {
        val config = WifiConfiguration()
        config.allowedAuthAlgorithms.clear()
        config.allowedGroupCiphers.clear()
        config.allowedKeyManagement.clear()
        config.allowedPairwiseCiphers.clear()
        config.allowedProtocols.clear()

        //指定对应的SSID
        config.SSID = "\"$ssid\""
        when (type) {
            WIFI_CIPHER_NO_PASS -> {
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            }
            WIFI_CIPHER_WEP -> {
                config.hiddenSSID = true
                config.wepKeys[0] = "\"$password\""
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED)
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                config.wepTxKeyIndex = 0
            }
            WIFI_CIPHER_WPA -> {
                config.preSharedKey = "\"$password\""
                config.hiddenSSID = true
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
                config.status = WifiConfiguration.Status.ENABLED
            }
        }
        return config
    }

    /**
     * 判断是否存在某个配置
     */
    @SuppressLint("MissingPermission")
    fun isExist(ssid: String): WifiConfiguration? {
        return wifiManager.configuredNetworks.firstOrNull { c -> c.SSID == "\"$ssid\"" }
    }

    /**
     * 获取网关地址
     *
     * @return 获取失败则返回空字符
     */
    fun getGateway(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val network = connectivityManager.boundNetworkForProcess ?: return ""
            val linkProperties = connectivityManager.getLinkProperties(network) ?: return ""
            linkProperties.routes.firstOrNull { routeInfo -> routeInfo.isDefaultRoute }?.gateway?.hostAddress
                ?: ""
        } else {
            getHotspotAddress()
        }
    }

    private fun getHotspotAddress(): String {
        val dhcp = wifiManager.dhcpInfo
        var ipAddress = dhcp.gateway
        ipAddress = if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            Integer.reverseBytes(ipAddress)
        } else {
            ipAddress
        }
        val ipAddressByte = BigInteger.valueOf(ipAddress.toLong()).toByteArray()
        try {
            return InetAddress.getByAddress(ipAddressByte)?.hostAddress ?: ""
        } catch (e: UnknownHostException) {
            Log.e("Wifi Class", "Error getting Hotspot IP address ", e)
        }
        return ""
    }

    companion object {
        const val WIFI_CIPHER_NO_PASS = 0
        const val WIFI_CIPHER_WEP = 1
        const val WIFI_CIPHER_WPA = 2
    }
}