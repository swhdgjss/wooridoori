package com.playrtc.wooridoori.handler

import android.text.format.Formatter

import com.playrtc.wooridoori.PlayRTCActivity
import com.sktelecom.playrtc.PlayRTC
import com.sktelecom.playrtc.PlayRTCStatsReport
import com.sktelecom.playrtc.observer.PlayRTCStatsReportObserver

class PlayRTCStatsReportHandler(activity: PlayRTCActivity) : PlayRTCStatsReportObserver {

    private var activity: PlayRTCActivity?=null
    private var playrtc: PlayRTC?=null

    init {
        this.activity=activity  //yn var로 바꿈
    }

    fun start(playrtc: PlayRTC?, peerId: String) {

        playrtc?.startStatsReport(TIMER_INTERVAL, this as PlayRTCStatsReportObserver, peerId)
        this.playrtc=playrtc
    }

    fun stop() {
        if (playrtc != null) {
            playrtc!!.stopStatsReport()
        }
    }


    /*
	 * PlayRTCStatsReportObserver Interface 구현
	 * @param report PlayRTCStatsReport
	 *
	 * PlayRTCStatsReport Interface
	 * - String getLocalCandidate();
	 *   자신의 ICE 서버 연결상태를 반환한다.
	 * - String getRemoteCandidate();
     *   상대방의 ICE 서버 연결상태를 반환한다.
	 * - String getLocalVideoCodec();
	 *   자신의 VideoCodec을 반환한다.
	 * - String getLocalAudioCodec();
	 *   자신의 AudioCodec을 반환한다.
	 * - String getRemoteVideoCodec();
	 *   상대방의 VideoCodec을 반환한다.
	 * - String getRemoteAudioCodec();
	 *   상대방의 AudioCodec을 반환한다.
     * - int getLocalFrameWidth();
     *   상대방에게 전송하는 영상의 해상도 가로 크기를 반환한다.
     * - int getLocalFrameWidth();
     *   상대방에게 전송하는 영상의 해상도 가로 크기를 반환한다.
     * - int getLocalFrameHeight();
     *   상대방에게 전송하는 영상의 해상도 세로 크기를 반환한다.
     * - int getRemoteFrameWidth();
     *   상대방 수신  영상의 해상도 가로 크기를 반환한다.
     * - int getRemoteFrameHeight();
     *   상대방 수신  영상의 해상도 세로 크기를 반환한다.
     * - int getLocalFrameRate();
     *   상대방에게 전송하는 영상의 Bit-Rate를 반환한다.
     * - int getRemoteFrameRate();
     *   상대방 수신  영상의 Bit-Rate를 반환한다.
     * - int getAvailableSendBandWidth();
     *   상대방에게 전송할 수 있는 네트워크 대역폭을 반환한다.
     * - int getAvailableReceiveBandWidth();
     *   상대방으로부터 수신할 수 있는 네트워크 대역폭을 반환한다.
     * - int getRtt();
     *   자신의 Rount Trip Time을 반환한다
     * - RatingValue getRttRating();
     *   RTT값을 기반으로 네트워크 상태를 5등급으로 분류하여 RttRating 를 반환한다.
     * - RatingValue getFractionRating();
     *   Packet Loss 값을 기반으로 상대방의 영상 전송 상태를 5등급으로 분류하여 RatingValue 를 반환한다.
     * - RatingValue getLocalAudioFractionLost();
     *   Packet Loss 값을 기반으로 자신의 음성 전송 상태를 5등급으로 분류하여RatingValue 를 반환한다.
     * - RatingValue getLocalVideoFractionLost();
     *   Packet Loss 값을 기반으로 자신의 영상 전송 상태를 5등급으로 분류하여RatingValue 를 반환한다.
     * - RatingValue getRemoteAudioFractionLost();
     *   Packet Loss 값을 기반으로 상대방의 음성 전송 상태를 5등급으로 분류하여RatingValue 를 반환한다.
     * - RatingValue getRemoteVideoFractionLost();
     *   Packet Loss 값을 기반으로 상대방의 영상 전송 상태를 5등급으로 분류하여RatingValue 를 반환한다.
	 */
    override fun onStatsReport(report: PlayRTCStatsReport) {

        val localVideoFl=report.localVideoFractionLost
        val localAudioFl=report.localAudioFractionLost
        val remoteVideoFl=report.remoteVideoFractionLost
        val remoteAudioFl=report.remoteAudioFractionLost


        val text=java.lang.String.format("Local\n ICE:%s\n Frame:%sx%sx%s\n 코덱:%s,%s\n Bandwidth[%sps]\n RTT[%s]\n RttRating[%d/%.4f]\n VFLost[%d/%.4f]\n AFLost[%d/%.4f]\n\nRemote\n ICE:%s\n Frame:%sx%sx%s\n 코덱:%s,%s\n Bandwidth[%sps]\n VFLost[%d/%.4f]\n AFLost[%d/%.4f]\n",
                report.localCandidate,
                report.localFrameWidth,
                report.localFrameHeight,
                report.localFrameRate,
                report.localVideoCodec,
                report.localAudioCodec,
                Formatter.formatFileSize(activity!!.applicationContext, report.availableSendBandwidth.toLong()) + "",
                report.rtt,
                report.rttRating.level,
                report.rttRating.value,
                localVideoFl.level,
                localVideoFl.value,
                localAudioFl.level,
                localAudioFl.value,
                report.remoteCandidate,
                report.remoteFrameWidth,
                report.remoteFrameHeight,
                report.remoteFrameRate,
                report.remoteVideoCodec,
                report.remoteAudioCodec,
                Formatter.formatFileSize(activity!!.applicationContext, report.availableReceiveBandwidth.toLong()) + "",   //yn 느낌표 붙임
                remoteVideoFl.level,
                remoteVideoFl.value,
                remoteAudioFl.level,
                remoteAudioFl.value)   //java.lang. yn 이거 왜 노랑색이냐..


        activity!!.printRtcStatReport(text)
    }

    companion object {

        //StatsReport 조회 주기 msec
        private val TIMER_INTERVAL: Long=5000//5 sec
    }

}
