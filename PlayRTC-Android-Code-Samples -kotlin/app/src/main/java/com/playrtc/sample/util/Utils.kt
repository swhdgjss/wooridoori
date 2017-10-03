package com.playrtc.sample.util


import android.app.Activity
import android.widget.Toast

/**
 * PlayRTC Sample App Util Class
 * @author ds3grk
 */
object Utils {
    private var logToast: Toast?=null


    /**
     * 화면에 Toast를 짧게 출력
     * @param activity Activity
     * @param msg String, 출력 메세지
     */
    fun showToast(activity: Activity, msg: String) {
        activity.runOnUiThread {
            if (logToast != null) {
                logToast!!.cancel()
                logToast=null
            }
            logToast=Toast.makeText(activity.applicationContext, msg, Toast.LENGTH_SHORT)
            logToast!!.show()
        }
    }

    /**
     * 사용자 아이디를 랜덤하게 생성하여 반환 <br></br>
     * XXXXX@@playrtc.com
     * @return String
     */
    val randomServiceMailId: String
        get() {
            var userId=""
            val possible="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
            for (i in 0..4) {
                val randomVal=Math.random()
                val idx=Math.floor(randomVal * possible.length).toInt()
                userId+=possible[idx]
            }

            return userId + "@playrtc.com"
        }
}
