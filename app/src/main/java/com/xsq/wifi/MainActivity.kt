package com.xsq.wifi

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.net.wifi.ScanResult
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.ProgressBar
import com.tbruyelle.rxpermissions3.RxPermissions
import com.xsq.wifi.databinding.ActivityMainBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.Socket
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val compositeDisposable = CompositeDisposable()
    private val rxPermissions: RxPermissions by lazy {
        RxPermissions(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btToWifiScan.setOnClickListener {
            val d = rxPermissions.request(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CHANGE_WIFI_STATE
            )
                .subscribe { granted ->
                    if (granted) {
                        wifiScan()
                    } else {
                        // 没有授予获取位置的权限
                    }
                }
            compositeDisposable.add(d)
        }

        binding.btToWifiConnect.setOnClickListener {
            if (!Settings.System.canWrite(this)) {
                BaseDialogFragment.Builder(this, 0)
                    .setMessage("使用WiFi自动连接功能需要授予系统设置的权限")
                    .setPositiveButton("去设置", DialogInterface.OnClickListener { dialog, which ->
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                        intent.data = Uri.parse("package:" + this.packageName)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    })
                    .setNegativeButton("取消", DialogInterface.OnClickListener { dialog, which -> dialog.dismiss() })
                    .create()
                    .show(supportFragmentManager, null)
                return@setOnClickListener
            }

            var disp: Disposable? = null
            disp = wifiConnect("Test", "test-1234")
                .delay(1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .subscribe { connected ->
                    if (connected) {
                        try {
                            Socket("10.38.178.131", 8989).use { socket ->
                                socket.getOutputStream().write("Hello, Server\n".toByteArray())
                                val bufferedReader =
                                    BufferedReader(InputStreamReader(socket.getInputStream()))
                                val response = bufferedReader.readLine()
                                Log.d("TEST", "response = $response")
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    disp?.dispose()
                }
        }
    }

    @SuppressLint("MissingPermission")
    private fun wifiScan() {
        val wifiHelper = WiFiHelper(this.applicationContext, object : WiFiSSIDFilter {
            override fun doFilter(ssid: String): Boolean {
                return true
            }
        },
            object : WiFiSSIDFilter {
                override fun doFilter(ssid: String): Boolean {
                    return true
                }
            })

        if (!wifiHelper.isEnabled()) {
            val result = wifiHelper.enable()
            if (!result) {
                return
            }
        }

        val progressBar = ProgressBar(this)
        val waitingDialog = BaseDialogFragment.Builder(this, 0)
            .setCancelable(false)
            .setView(progressBar)
            .create()

        waitingDialog.show(supportFragmentManager, null)

        wifiHelper.getConnectHistory().forEach {
            val address = it.BSSID
            val name = it.SSID
            Log.d("WiFiSaveResult", "SSID -> $name($address)")
        }

        val wifiList = ArrayList<Map<String, String>>()
        wifiHelper.scan()
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ScanResult> {
                override fun onSubscribe(d: Disposable?) {
                }

                override fun onNext(t: ScanResult) {
                    val map = HashMap<String, String>()
                    map[t.BSSID] = t.SSID
                    wifiList.add(map)
                }

                override fun onError(e: Throwable?) {
                    e?.printStackTrace()
                    waitingDialog.dismiss()
                }

                override fun onComplete() {
                    wifiList.forEach {
                        val address = it.keys.first()
                        val name = it[address]
                        Log.d("WiFiScanResult", "SSID -> $name($address)")
                    }
                    waitingDialog.dismiss()
                }
            })
    }

    private fun wifiConnect(ssid: String, password: String): Observable<Boolean> {
        val wifiHelper = WiFiHelper(this.applicationContext)
        return wifiHelper.connect(ssid, password, WiFiHelper.WIFI_CIPHER_WPA)
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

}