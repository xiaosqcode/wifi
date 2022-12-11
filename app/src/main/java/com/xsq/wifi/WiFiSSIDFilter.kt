package com.xsq.wifi

/**
 * 无线信号过滤器
 *
 * Created by Xiaoshiquan on 2021/4/19.
 */
interface WiFiSSIDFilter {

    /**
     * 过滤判定
     *
     * 根据返回结果判定是否要从结果中剔除
     *  - 返回 false，表示剔除
     *  - 返回 true，表示保留
     */
    fun doFilter(ssid: String): Boolean

}