package com.playrtc.wooridoori.handler

import android.text.TextUtils
import android.util.Log

import com.playrtc.wooridoori.PlayRTCActivity
import com.playrtc.wooridoori.view.PlayRTCChannelView
import com.sktelecom.playrtc.exception.RequiredConfigMissingException

import org.json.JSONException
import org.json.JSONObject

/**
 * PlayRTCChannelView에서 채널 서비스에 채널 생성/입장 요청을 받아 처리하기 위해  <br></br>
 * PlayRTCChannelView.PlayRTCChannelViewListener를 구현한 리스너 구현 Class<br></br>
 * <pre>
 * - onClickCreateChannel : 채널 생성 버튼을 늘렀을 때
 * - onClickConnectChannel : 채널 입장 보튼을 눌렀을때
</pre> *
 *
 * @see com.playrtc.wooridoori.view.PlayRTCChannelView.PlayRTCChannelViewListener
 */
class PlayRTCChannelViewListener(activity: PlayRTCActivity) : PlayRTCChannelView.PlayRTCChannelViewListener {

    private var activity: PlayRTCActivity? = null

    init {
        this.activity = activity                         //yn var로 바꿈
    }

    /*
     * 채널 생성 버튼을 늘렀을 때
     *
     * @param channelName String, 채널 이름
     * @param userId      String, 사용자 Application 사용자 아이디
     * @param userName    String, 사용자 이름
     */
    override fun onClickCreateChannel(channelName: String, userId: String, userName: String) {
        Log.d(LOG_TAG, "onClickCreateChannel channelName[$channelName] userId[$userId] userName[$userName]")

        // 채널방 정보 생성
        val parameters = JSONObject()

        if (TextUtils.isEmpty(channelName) == false) {
            // 채널정보를 정의한다.
            val channel = JSONObject()
            try {
                // 채널에 대한 이름을 지정한다.
                channel.put("channelName", channelName)
                parameters.put("channel", channel)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }
        if (TextUtils.isEmpty(userId) == false || TextUtils.isEmpty(userName) == false) {
            // 채널 사용자에 대한 정보를 정의한다.
            val peer = JSONObject()
            try {
                if (TextUtils.isEmpty(userId) == false) {
                    // application에서 사용하는 사용자 아이디를 지정
                    peer.put("uid", userId)
                }
                if (TextUtils.isEmpty(userName) == false) {
                    // 사용자에 대한 별칭을 지정한다.
                    peer.put("userName", userName)
                }
                parameters.put("peer", peer)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }

        Log.d(LOG_TAG, "playRTC.createChannel " + parameters.toString())
        try {
            activity!!.playRTCHandler!!.createChannel(parameters)
        } catch (e: RequiredConfigMissingException) {
            e.printStackTrace()
        }

    }

    /*
     * 채널 입장 버튼을 눌렀을 때
     *
     * @param channelId String, 입장할 채널의 아이디
     * @param userId    String, 사용자 Application 사용자 아이디
     * @param userName  String, 사용자 이름
     */
    override fun onClickConnectChannel(channelId: String, userId: String, userName: String) {
        Log.d("LOG_TAG", "onConnectChannel channelId[$channelId] userId[$userId] userName[$userName]")

        // 채널방 정보 생성
        val parameters = JSONObject()

        if (TextUtils.isEmpty(userId) == false || TextUtils.isEmpty(userName) == false) {

            val peer = JSONObject()
            try {
                if (TextUtils.isEmpty(userId) == false) {
                    peer.put("uid", userId)
                }
                if (TextUtils.isEmpty(userName) == false) {
                    peer.put("userName", userName)
                }
                parameters.put("peer", peer)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }
        Log.d("LOG_TAG", "onConnectChannel call playRTC.createChannel($channelId, parameters)")
        try {
            activity!!.playRTCHandler!!.connectChannel(channelId, parameters)
        } catch (e: RequiredConfigMissingException) {

            e.printStackTrace()
        }

    }

    companion object {

        private val LOG_TAG = "CHANNEL"
    }
}