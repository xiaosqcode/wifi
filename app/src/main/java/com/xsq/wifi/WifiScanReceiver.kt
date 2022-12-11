package com.xsq.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import io.reactivex.rxjava3.core.Observer

/**
 * Created by Xiaoshiquan on 2021/4/19.
 */
class WifiScanReceiver(
    private val listener: Observer<Boolean>
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//大于6.0的版本
                val success = intent?.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success == null || !success) {
                    // 失败
                    //此时获取的scanResults可能为空或者是之前老的扫描结果
                    listener.onNext(false)
                } else { // 成功
                    // 此时获取的是最新的扫描结果
                    listener.onNext(true)
                }
            } else {
                // 小于6.0的版本
                listener.onNext(true)
            }
        }
        listener.onComplete()
    }
}