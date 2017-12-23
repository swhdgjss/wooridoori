package com.playrtc.wooridoori

import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RadioGroup

/*
 * PlayRTC Sample App Main Activity Class
 */

class MainActivity : Activity() {
    /**
     * isCloesActivity가 false이면 Dialog를 통해 사용자의 종료 의사를 확인하고<br></br>
     * Activity를 종료 처리.
     */
    private var isCloesActivity=false

    private val MY_PERMISSION_REQUEST_STORAGE=100


    private var channelRing="false"
    private var videoCodec="vp8"
    private var audioCodec="isac"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 버튼 이벤트 등록
        initUIControls()

        // Application permission 23
        if (android.os.Build.VERSION.SDK_INT >= 23) {

            checkPermission(MANDATORY_PERMISSIONS)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == LAUNCHED_PLAYRTC) {
            if (resultCode == Activity.RESULT_OK) {
            }
        }
    }

    public override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        android.os.Process.killProcess(android.os.Process.myPid())
        super.onDestroy()
    }

    /**
     * isCloesActivity가 false이면 Dialog를 통해 사용자의 종료 의사를 확인하고<br></br>
     * Activity를 종료 처리.
     */
    override fun onBackPressed() {
        // isCloesActivity가 true이면 Activity를 종료 처리.
        if (isCloesActivity) {
            super.onBackPressed()
        } else {
            val alert=AlertDialog.Builder(this)
            alert.setTitle("WooriDoori")
            alert.setMessage("WooriDoori App을 종료하겠습니까?")

            alert.setPositiveButton("종료") { dialog, which ->
                dialog.dismiss()
                isCloesActivity=true
                onBackPressed()
            }
            alert.setNegativeButton("취소") { dialog, whichButton ->
                dialog.dismiss()
                isCloesActivity=false
            }
            alert.show()
        }// isCloesActivity가 false이면 Dialog를 통해 사용자의 종료 의사를 확인
    }

    /**
     * Sample Type 별 버튼 이벤트 등록
     */
    private fun initUIControls() {
        this.findViewById(R.id.btn_go_sample2).setOnClickListener{
            excutePlayRTCSample()
        }
/*
        (findViewById(R.id.radio_ring_group) as RadioGroup).setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.radio_ring_false) {
                channelRing="false"
            } else {
                channelRing="true"
            }
        }
        (findViewById(R.id.radio_video_codec_group) as RadioGroup).setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.radio_video_codec_vp8) {
                videoCodec="vp8"
            } else if (checkedId == R.id.radio_video_codec_vp9) {
                videoCodec="vp9"
            } else {
                videoCodec="h264"
            }
        }
        (findViewById(R.id.radio_audio_codec_group) as RadioGroup).setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.radio_audio_codec_isac) {
                audioCodec="isac"
            } else {
                audioCodec="opus"
            }
        }*/
    }

    @TargetApi(23)
    private fun checkPermission(permissions: Array<String>) {

        requestPermissions(permissions, MY_PERMISSION_REQUEST_STORAGE)
    }

    /**
     * Application permission, android build target 23
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSION_REQUEST_STORAGE -> {
                val cnt=permissions.size
                for (i in 0..cnt - 1) {

                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {

                        Log.i(LOG_TAG, "Permission[" + permissions[i] + "] = PERMISSION_GRANTED")

                    } else {

                        Log.i(LOG_TAG, "permission[" + permissions[i] + "] always deny")
                    }
                }
            }
        }
    }

    /**
     * PlayRTCActivity 이동
     *
     */
    private fun excutePlayRTCSample() {
        val intent = Intent(this, PlayRTCActivity::class.java)
        // PlayRTC Sample 유형 전달
        intent.putExtra("channelRing", channelRing)
        intent.putExtra("videoCodec", videoCodec)
        intent.putExtra("audioCodec", audioCodec)
        this@MainActivity.startActivityForResult(intent, LAUNCHED_PLAYRTC)
    }

    companion object {

        private val LOG_TAG="MainActivity"

        private val LAUNCHED_PLAYRTC=100
        /**
         * Application permission 목록, android build target 23
         */
        val MANDATORY_PERMISSIONS=arrayOf("android.permission.INTERNET", "android.permission.CAMERA", "android.permission.RECORD_AUDIO", "android.permission.MODIFY_AUDIO_SETTINGS", "android.permission.ACCESS_NETWORK_STATE", "android.permission.CHANGE_WIFI_STATE", "android.permission.ACCESS_WIFI_STATE", "android.permission.READ_PHONE_STATE", "android.permission.BLUETOOTH", "android.permission.BLUETOOTH_ADMIN", "android.permission.WRITE_EXTERNAL_STORAGE")
    }

}