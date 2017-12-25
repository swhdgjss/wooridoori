package com.playrtc.wooridoori

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Point
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener


import com.playrtc.wooridoori.handler.PlayRTCChannelViewListener
import com.playrtc.wooridoori.handler.PlayRTCDataChannelHandler
import com.playrtc.wooridoori.handler.PlayRTCHandler
import com.playrtc.wooridoori.util.Utils
import com.playrtc.wooridoori.view.*
import com.sktelecom.playrtc.PlayRTC.PlayRTCWhiteBalance
import com.sktelecom.playrtc.exception.RequiredParameterMissingException
import com.sktelecom.playrtc.exception.UnsupportedPlatformVersionException
import com.sktelecom.playrtc.util.PlayRTCRange
import com.sktelecom.playrtc.util.ui.PlayRTCVideoView
import android.widget.Toast
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import android.provider.Telephony.Carriers.PORT
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import kotlinx.android.synthetic.main.activity_rtc.*
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*

/*
 * PlayRTC를 구현한 Activity Class
 *
 * 주요 멤버
 * - PlayRTCHandler playrtcHandler
 *     PlayRTC 인스턴스 및 관련 기능을 제공
 *     PlayRTCObserver Listener Interface 구현
 *
 * - PlayRTCDataChannelHandler dataHandler
 *     PlayRTCData를 위한 PlayRTCDataObserver Interface를 구현한 Handler Class
 *     PlayRTCData를 이용해 데이터 송/수신 처리
 *
 * - PlayRTCVideoViewGroup videoLayer
 *     영상 출력 뷰(PlayRTCVideoView)의 ViewGroup .
 *     Local/Remote 뷰 생성 및 인터페이스 제공
 *
 * - PlayRTCChannelView channelInfoView
 *     Sample에서 채널을 생성하거나 채널 목록을 조회하여 입장 할 채널을 선택하는 팝업 뷰
 *     생성/입장 버튼 이벤트를 받기 위해 PlayRTCChannelViewListener를 구현한다.
 *
 */
class PlayRTCActivity : Activity() {

    /*
     * 채널 팝업 뷰
     * 채널 서비스에 채널을 생성하거나 입장할 채널을 선택하는 UI
     *
     * @see com.playrtc.sample.view.PlayRTCChannelView
     */
    /*
     * PlayRTCChannelView 인스턴스를 반환한다.
     * @return PlayRTCChannelView
     */
    var channelInfoPopup: PlayRTCChannelView? = null
        private set

    /*
     * PlayRTC-Handler Class
     * PlayRTC 메소드 , PlayRTC객체의 이벤트 처리
     */
    /*
     * PlayRTCHandler 인스턴스를 반환한다.
     * @return PlayRTCHandler
     */
    var playRTCHandler: PlayRTCHandler? = null
        private set

    /*
     * PlayRTCVideoView를 위한 부모 뷰 그룹
     */
    var videoLayer: PlayRTCVideoViewGroup? = null
        private set

    /*
     * PlayRTCData를 위한 Handler Class
     *
     * @see com.playrtc.sample.handler.PlayRTCDataChannelHandler
     */
    /*
     * PlayRTCDataChannelHandler 인스턴스를 반환한다.
     * @return PlayRTCDataChannelHandler
     */
    var rtcDataHandler: PlayRTCDataChannelHandler? = null
        private set

    /*
     * 로그 출력 TextView
     *
     * @see com.playrtc.sample.view.PlayRTCLogView
     */
    private var logView: PlayRTCLogView? = null

    /*
     * PlayRTC P2P Status report 출력 TextView
     */
    private var txtStatReport: TextView? = null

    /*
     * 영상 뷰 Snapshot 이미지 요청 및 이미지 출력을 위한 뷰 그룹
     */
    private var snapshotLayer: PlayRTCSnapshotView? = null

    /*
     * isCloesActivity가 false이면 Dialog를 통해 사용자의 종료 의사를 확인하고<br>
     * Activity를 종료 처리. 만약 채널에 입장한 상태이면 먼저 채널을 종료한다.
     */
    private var isCloesActivity = false

    private var zoomRangeBar: PlayRTCVerticalSeekBar? = null

    internal lateinit var serversocket: ServerSocket
    internal lateinit var socket: Socket
    internal lateinit var inputStream: DataInputStream
    internal var outputStream: DataOutputStream? = null
    internal var ip = "192.168.193.140" //서버 단말기의 IP주소..
    internal var isConnected = true

    var server_stack = 0
    var client_stack = 0
    var send_stack = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rtc)

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)

        val intent = intent

        val channelRing = intent.getStringExtra("channelRing")
        val videoCodec = intent.getStringExtra("videoCodec")
        val audioCodec = intent.getStringExtra("audioCodec")

        // UI 인스턴스 변수 처리
        initUIControls()

        initSnapshotControlls()

        playRTCHandler = PlayRTCHandler(this)
        try {
            //  PlayRTC 인스턴스를 생성.
            playRTCHandler!!.createPlayRTC(channelRing, videoCodec, audioCodec)
        } catch (e: UnsupportedPlatformVersionException) {
            // Android SDK 버전 체크 Exception
            e.printStackTrace()
        } catch (e: RequiredParameterMissingException) {
            // 필수 Parameter 체크 Exception
            e.printStackTrace()
        }

        // P2P 데이터 통신을 위한 객체 생성
        this.rtcDataHandler = PlayRTCDataChannelHandler(this)

        // 채널 생성/입장 팝업 뷰 초기화 설정
        this.channelInfoPopup!!.init(this, playRTCHandler!!.playRTC, PlayRTCChannelViewListener(this))

        // PlayRTC 채널 서비스에서 채멀 목록을 조회하여 리스트에 출력한다.
        this.channelInfoPopup!!.showChannelList()

        // 채널 생성 또는 채널 입장하기 위한 팝업 레이어 출력
        this.channelInfoPopup!!.show(600)
    }

    // Activty의 포커스 여부를 확인
    // 영상 스트림 출력을 위한 PlayRTCVideoView(GLSurfaceView를 상속) 동적 코드 생성
    // 생성 시 스크린 사이즈를 생성자에 넘김
    // onWindowFocusChanged는 여러번 호출 되므로 관련 객체 초기화를 한번만 실행하도록 해야함.
    // hasFocus = true , 화면보여짐 , onCreate | onResume
    // hasFocus = false , 화면안보임 , onPause | onDestory
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        // Layout XML에 VideoView를 기술한 경우. v2.2.6
        if (hasFocus && videoLayer!!.isInitVideoView == false) {
            videoLayer!!.initVideoView()
        }
    }

    public override fun onPause() {
        super.onPause()
        // 미디어 스트리밍 처리 pause
        if (playRTCHandler != null) playRTCHandler!!.onActivityPause()
    }

    public override fun onResume() {
        super.onResume()
        // 미디어 스트리밍 처리 resume
        if (playRTCHandler != null) playRTCHandler!!.onActivityResume()
    }

    override fun onDestroy() {             //채널 나가는 함수
        Log.e(LOG_TAG, "onDestroy===============================")

        // PlayRTC 인스턴스 해제
        if (playRTCHandler != null) {
            playRTCHandler!!.close()
            playRTCHandler = null
        }
        // v2.2.6
        if (videoLayer != null) {
            videoLayer!!.releaseView()
        }
        this.finish()
        super.onDestroy()
    }

    /*
     * isCloesActivity가 false이면 Dialog를 통해 사용자의 종료 의사를 확인하고<br>
     * Activity를 종료 처리. 만약 채널에 입장한 상태이면 먼저 채널을 종료한다.
     */
    override fun onBackPressed() {
        Log.e(LOG_TAG, "onBackPressed===============================")

        // 채널 팝업이 보여지는 상태에서 onBackPressed()가 호출 되면
        // 팝업 창을 닫기만 한다.
        if (channelInfoPopup!!.isShown) {
            channelInfoPopup!!.hide(0)
            return
        }

        // Activity를 종료하도록 isCloesActivity가 true로 지정되어 있다면 종료 처리
        if (isCloesActivity) {
            // BackPress 처리 -> onDestroy 호출
            Log.e(LOG_TAG, "super.onBackPressed()===============================")
            setResult(Activity.RESULT_OK, Intent())
            super.onBackPressed()
        } else {
            // 만약 채널에 입장한 상태이면 먼저 채널을 종료한다.
            val alert = AlertDialog.Builder(this)
            alert.setTitle("WooriDoori")
            alert.setMessage("WooriDoori를 종료하겠습니까?")

            alert.setPositiveButton("종료") { dialog, which ->
                dialog.dismiss()
                // 채널에 입장한 상태라면 채널을 먼저 종료한다.
                // 종료 이벤트에서 isCloesActivity를 true로 설정하고 onBackPressed()를 호출하여
                // Activity를 종료 처리
                if (playRTCHandler!!.isChannelConnected == true) {
                    isCloesActivity = false
                    // PlayRTC 플랫폼 채널을 종료한다.
                    playRTCHandler!!.disconnectChannel()
                } else {
                    isCloesActivity = true
                    onBackPressed()
                }// 채널에 입장한 상태가 아니라면 바로 종료 처리
            }
            alert.setNegativeButton("취소") { dialog, whichButton ->
                dialog.dismiss()
                isCloesActivity = false
            }
            alert.show()
        }
    }

    /*
     * 로컬 영상 PlayRTCVideoView 인스턴스를 반환한다.
     * @return PlayRTCVideoView
     */
    val localVideoView: PlayRTCVideoView
        get() = videoLayer!!.localView

    /*
     * 상대방 영상 PlayRTCVideoView 인스턴스를 반환한다.
     * @return PlayRTCVideoView
     */
    val remoteVideoView: PlayRTCVideoView
        get() = videoLayer!!.remoteView

    /*
     * PlayRTCActivity를 종료한다.
     * PlayRTCHandler에서 채널이 종료 할 때 호출한다.
     * @param isClose boolean, 종료 처리 시 사용자의 종료 으의사를 묻는 여부
     */
    fun setOnBackPressed(isClose: Boolean) {
        if (channelInfoPopup!!.isShown) {
            channelInfoPopup!!.hide(0)
        }
        isCloesActivity = isClose
        this.onBackPressed()
    }

    /*
     * PlayRTCLogView의  하단에 로그 문자열을 추가 한다.
     * @param message String
     */
    fun appnedLogMessage(message: String) {
        if (logView != null) {
            logView!!.appnedLogMessage(message)
        }
    }

    /*
     * PlayRTCLogView의 최 하단에 로그 문자열을 추가 한다. <br>
     * 주로 진행 상태 메세지를 표시 하기 위해 최 하단의 진행 상태 메세지만 갱신한다.
     * @param message String
     */
    fun progressLogMessage(message: String) {
        if (logView != null) {
            logView!!.progressLogMessage(message)
        }
    }

    /*
     * PlayRTC P2P 상태 문자열을 출력한다.
     * @param resport
     */
    fun printRtcStatReport(resport: String) {
        txtStatReport!!.post { txtStatReport!!.text = resport }
    }

    /*
     * Layout 관련 인스턴스 설정 및 이벤트 정의
     */
    private fun initUIControls() {
        /* 채널 팝업 뷰 */
        channelInfoPopup = findViewById(R.id.channel_info) as PlayRTCChannelView

        /*video 스트림 출력을 위한 PlayRTCVideoView의 부모 ViewGroup */
        videoLayer = findViewById(R.id.videoarea) as PlayRTCVideoViewGroup

        /*video 스트림 출력을 위한 PlayRTCVideoView의 부모 ViewGroup */
        videoLayer = findViewById(R.id.videoarea) as PlayRTCVideoViewGroup

        snapshotLayer = this.findViewById(R.id.snapshot_area) as PlayRTCSnapshotView

        /* PlayRTC P2P Status report 출력 TextView */
        txtStatReport = this.findViewById(R.id.txt_stat_report) as TextView
        val text = "Local\n ICE:none\n Frame:0x0x0\n Bandwidth[0bps]\n RTT[0]\n eModel[-]\n VFLost[0]\n AFLost[0]\n\nRemote\n ICE:none\n Frame:0x0x0\n Bandwidth[0bps]\n VFLost[0]\n AFLost[0]"
        txtStatReport!!.text = text

        /* 채널 팝업 버튼 */
        val channelPopup = this.findViewById(R.id.btn_channel) as ImageButton
        channelPopup.setOnClickListener {
            if (channelInfoPopup!!.isShown) {
                channelInfoPopup!!.hide(0)
            } else {
                channelInfoPopup!!.showChannelList()
                channelInfoPopup!!.show(0)
            }
        }

        /* 카메라 전/후방 전환 버튼 */
        initSwitchVideoCameraFunctionUIControls()

        /* Peer 채널 퇴장/종료 버튼 */
        initChannelCloseFunctionUIControls()

        /* 미디어 스트림 Mute 버튼 */
        initMediaMuteFunctionUIControls()

        /* Video View ShowSnapshot 기능 버튼 */
        initVideoViewShowSnapshotFunctionUIControls()

        /* Menu 기능 버튼*/
        initMenuControls()
    }

    /* 카메라 전/후방 전환 버튼 */
    private fun initSwitchVideoCameraFunctionUIControls() {
        val cameraBtn = this.findViewById(R.id.btn_switch_camera) as ImageButton

        cameraBtn.setOnClickListener(object : View.OnClickListener {  //Button->view
            override fun onClick(v: View) {
                playRTCHandler!!.switchVideoCamera()
            }
        })
    }

    /* Peer 채널 종료 버튼 */
    private fun initChannelCloseFunctionUIControls() {
        val btnCloseChannel = this.findViewById(R.id.btn_chClose) as ImageButton

        btnCloseChannel.setOnClickListener(object : View.OnClickListener {  //Button->view
            override fun onClick(v: View) {
                if (playRTCHandler != null && playRTCHandler!!.isChannelConnected) {
                    playRTCHandler!!.delateChannel()
                }
            }
        })
    }

    /* 미디어 스트림 Mute 버튼 */
    private fun initMediaMuteFunctionUIControls() {
        /* Local Video Mute 버튼 */
        var setMute_LV = false
        val btnMuteLVideo = this.findViewById(R.id.btn_local_vmute) as ImageButton

        /* Local Video Mute 처리시 로컬 영상 스트림은 화면에 출력이 안되며 상대방에게 전달이 되지 않는다. */
        btnMuteLVideo.setOnClickListener(object : View.OnClickListener {  //Button->view
            override fun onClick(v: View) {
                if (playRTCHandler != null) {
                    if (setMute_LV == false) {
                        setMute_LV = true
                    } else {
                        setMute_LV = false
                    }
                    playRTCHandler!!.setLocalVideoPause(setMute_LV)
                    if (setMute_LV == true) {
                        btnMuteLVideo.setImageResource(R.drawable.video_on)
                    } else {
                        btnMuteLVideo.setImageResource(R.drawable.video_off)
                    }
                }
            }
        })

        /* Local Audio Mute 버튼 */
        var setMute_LA = false
        val btnMuteLAudio = this.findViewById(R.id.btn_local_amute) as ImageButton

        /* Local Audio Mute 처리시 로컬 음성 스트림은 상대방에게 전달이 되지 않는다. */
        btnMuteLAudio.setOnClickListener(object : View.OnClickListener {  //Button->view
            override fun onClick(v: View) {
                if (playRTCHandler != null) {
                    if (setMute_LA == false) {
                        setMute_LA = true
                    } else {
                        setMute_LA = false
                    }
                    playRTCHandler!!.setLocalAudioMute(setMute_LA)
                    if (setMute_LA == true) {
                        btnMuteLAudio.setImageResource(R.drawable.audio_on)
                    } else {
                        btnMuteLAudio.setImageResource(R.drawable.audio_off)
                    }
                }
            }
        })

        /* Remote Video Mute 버튼 */
        var setMute_RV = false
        val btnMuteRVideo = this.findViewById(R.id.btn_remote_vmute) as ImageButton

        /* Remote Video Mute 처리시 상대방의 영상 스트림은 수신되나 화면에는 출력이 되지 않는다. */
        btnMuteRVideo.setOnClickListener(object : View.OnClickListener {  //Button->view
            override fun onClick(v: View) {
                if (playRTCHandler != null) {
                    if (setMute_RV == false) {
                        setMute_RV = true
                    } else {
                        setMute_RV = false
                    }
                    playRTCHandler!!.setRemoteVideoPause(setMute_RV)
                    if (setMute_RV == true) {
                        btnMuteRVideo.setImageResource(R.drawable.video_on)
                    } else {
                        btnMuteRVideo.setImageResource(R.drawable.video_off)
                    }
                }
            }
        })

        /* Remote Audio Mute 버튼 */
        var setMute_RA = false
        val btnMuteRAudio = this.findViewById(R.id.btn_remote_amute) as ImageButton

        /* Remote Video Mute 처리시 상대방 영상 스트림은 수신되나 소리는 출력되지 않는다. */
        btnMuteRAudio.setOnClickListener(object : View.OnClickListener {  //Button->view
            override fun onClick(v: View) {
                if (playRTCHandler != null) {
                    if (setMute_RA == false) {
                        setMute_RA = true
                    } else {
                        setMute_RA = false
                    }
                    playRTCHandler!!.setRemoteAudioMute(setMute_RA)
                    if (setMute_RA == true) {
                        btnMuteRAudio.setImageResource(R.drawable.audio_on)
                    } else {
                        btnMuteRAudio.setImageResource(R.drawable.audio_off)
                    }
                }
            }
        })
    }

    //메뉴 버튼
    private fun initMenuControls() {
        val btnMenu = this.findViewById(R.id.btn_menu) as ImageButton

        btnMenu.setOnClickListener(object : View.OnClickListener {  //Button->view
            override fun onClick(v: View) {
                val layer = findViewById(R.id.btn_menu_layer) as RelativeLayout
                if (layer.isShown) {
                    layer.visibility = View.GONE
                } else {
                    hideFuntionUILayer()
                    layer.visibility = View.VISIBLE
                }
            }
        })

        val btnSticker = this.findViewById(R.id.btn_sticker) as ImageButton

        btnSticker.setOnClickListener(object : View.OnClickListener {  //Button->view
            override fun onClick(v: View) {
                val layer = findViewById(R.id.btn_sticker_layer) as RelativeLayout

                if (layer.isShown) {
                    layer.visibility = View.GONE
                } else {
                    hideFuntionUILayer()
                    layer.visibility = View.VISIBLE
                }
            }
        })

        val channelPopup = this.findViewById(R.id.btn_channel) as ImageButton

        channelPopup.setOnClickListener {
            if (channelInfoPopup!!.isShown) {
                channelInfoPopup!!.hide(0)
            } else {
                channelInfoPopup!!.showChannelList()
                channelInfoPopup!!.show(0)
            }
        }

        val flashBtn = this.findViewById(R.id.btn_switch_flash) as ImageButton

        flashBtn.setOnClickListener(object : View.OnClickListener {   //Button->viewv
            override fun onClick(v: View) {
                if (playRTCHandler == null) {
                    return
                }
                playRTCHandler!!.switchBackCameraFlash()
            }
        })

        /* 필터 서브 버튼 */
        val btnCameraWbalance = this.findViewById(R.id.btn_white_balance) as ImageButton

        btnCameraWbalance.setOnClickListener(object : View.OnClickListener {  //Button->view
            override fun onClick(v: View) {
                if (playRTCHandler == null) {
                    return
                }
                val layer = findViewById(R.id.btn_white_balance_layer) as RelativeLayout

                if (layer.isShown) {
                    layer.visibility = View.GONE
                } else {
                    val whiteBalance = playRTCHandler!!.cameraWhiteBalance
                    var labelText: String? = null

                    hideFuntionUILayer()

                    if (whiteBalance == PlayRTCWhiteBalance.Auto) {
                        labelText = "자동"
                    } else if (whiteBalance == PlayRTCWhiteBalance.Incandescent) {
                        labelText = "백열등"
                    } else if (whiteBalance == PlayRTCWhiteBalance.FluoreScent) {
                        labelText = "형광등"
                    } else if (whiteBalance == PlayRTCWhiteBalance.DayLight) {
                        labelText = "햇빛"
                    } else if (whiteBalance == PlayRTCWhiteBalance.CloudyDayLight) {
                        labelText = "흐림"
                    } else if (whiteBalance == PlayRTCWhiteBalance.TwiLight) {
                        labelText = "저녁빛"
                    } else if (whiteBalance == PlayRTCWhiteBalance.Shade) {
                        labelText = "그늘"
                    }
                    (findViewById(R.id.white_balance_label) as TextView).text = labelText
                    layer.visibility = View.VISIBLE
                }
            }
        })

        (this.findViewById(R.id.btn_white_balance_auto) as Button).setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {             //Button->view
                if (playRTCHandler!!.setCameraWhiteBalance(PlayRTCWhiteBalance.Auto)) {
                    val layer = findViewById(R.id.btn_white_balance_layer) as RelativeLayout

                    (findViewById(R.id.white_balance_label) as TextView).text = "자동"
                    layer.visibility = View.GONE
                }
            }
        })

        (this.findViewById(R.id.btn_white_balance_incandescent) as Button).setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {                  //Button->view
                if (playRTCHandler!!.isSupportedCameraWhiteBalance(PlayRTCWhiteBalance.Incandescent) == false) {
                    Utils.showToast(this@PlayRTCActivity, "단말기가 지원하지 않습니다.")
                    return
                }
                if (playRTCHandler!!.setCameraWhiteBalance(PlayRTCWhiteBalance.Incandescent)) {
                    val layer = findViewById(R.id.btn_white_balance_layer) as RelativeLayout

                    (findViewById(R.id.white_balance_label) as TextView).text = "백열등"
                    layer.visibility = View.GONE
                }
            }
        })

        (this.findViewById(R.id.btn_white_balance_fluoreScent) as Button).setOnClickListener(object : View.OnClickListener {    //Button->view
            override fun onClick(v: View) {
                if (playRTCHandler!!.isSupportedCameraWhiteBalance(PlayRTCWhiteBalance.FluoreScent) == false) {
                    Utils.showToast(this@PlayRTCActivity, "단말기가 지원하지 않습니다.")
                    return
                }
                if (playRTCHandler!!.setCameraWhiteBalance(PlayRTCWhiteBalance.FluoreScent)) {
                    val layer = findViewById(R.id.btn_white_balance_layer) as RelativeLayout

                    (findViewById(R.id.white_balance_label) as TextView).text = "형광등"
                    layer.visibility = View.GONE
                }
            }
        })

        (this.findViewById(R.id.btn_white_balance_daylight) as Button).setOnClickListener(object : View.OnClickListener {   //Button->view
            override fun onClick(v: View) {

                if (playRTCHandler!!.isSupportedCameraWhiteBalance(PlayRTCWhiteBalance.DayLight) == false) {
                    Utils.showToast(this@PlayRTCActivity, "단말기가 지원하지 않습니다.")
                    return
                }
                if (playRTCHandler!!.setCameraWhiteBalance(PlayRTCWhiteBalance.DayLight)) {
                    val layer = findViewById(R.id.btn_white_balance_layer) as RelativeLayout

                    (findViewById(R.id.white_balance_label) as TextView).text = "햇빛"
                    layer.visibility = View.GONE
                }
            }
        })

        (this.findViewById(R.id.btn_white_balance_cloudydaylight) as Button).setOnClickListener(object : View.OnClickListener {   //Button->view
            override fun onClick(v: View) {

                if (playRTCHandler!!.isSupportedCameraWhiteBalance(PlayRTCWhiteBalance.CloudyDayLight) == false) {
                    Utils.showToast(this@PlayRTCActivity, "단말기가 지원하지 않습니다.")
                    return
                }
                if (playRTCHandler!!.setCameraWhiteBalance(PlayRTCWhiteBalance.CloudyDayLight)) {
                    val layer = findViewById(R.id.btn_white_balance_layer) as RelativeLayout

                    (findViewById(R.id.white_balance_label) as TextView).text = "흐림"
                    layer.visibility = View.GONE
                }
            }
        })

        (this.findViewById(R.id.btn_white_balance_twilight) as Button).setOnClickListener(object : View.OnClickListener {    //Button->view
            override fun onClick(v: View) {

                if (playRTCHandler!!.isSupportedCameraWhiteBalance(PlayRTCWhiteBalance.TwiLight) == false) {
                    Utils.showToast(this@PlayRTCActivity, "단말기가 지원하지 않습니다.")
                    return
                }
                if (playRTCHandler!!.setCameraWhiteBalance(PlayRTCWhiteBalance.TwiLight)) {
                    val layer = findViewById(R.id.btn_white_balance_layer) as RelativeLayout

                    (findViewById(R.id.white_balance_label) as TextView).text = "저녁빛"
                    layer.visibility = View.GONE
                }
            }
        })

        (this.findViewById(R.id.btn_white_balance_shade) as Button).setOnClickListener(object : View.OnClickListener {   //Button->view
            override fun onClick(v: View) {
                if (playRTCHandler!!.isSupportedCameraWhiteBalance(PlayRTCWhiteBalance.Shade) == false) {
                    Utils.showToast(this@PlayRTCActivity, "단말기가 지원하지 않습니다.")
                    return
                }
                if (playRTCHandler!!.setCameraWhiteBalance(PlayRTCWhiteBalance.Shade)) {
                    val layer = findViewById(R.id.btn_white_balance_layer) as RelativeLayout

                    (findViewById(R.id.white_balance_label) as TextView).text = "그늘"
                    layer.visibility = View.GONE
                }
            }
        })

        /*zoom*/
        val btnCameraZoom = this.findViewById(R.id.btn_camera_zoom) as ImageButton
        zoomRangeBar = this.findViewById(R.id.seekbar_camera_zoom) as PlayRTCVerticalSeekBar

        btnCameraZoom.setOnClickListener(object : View.OnClickListener { //Button->view
            override fun onClick(v: View) {
                if (playRTCHandler == null) {
                    return
                }
                val layer = findViewById(R.id.btn_camera_zoom_layer) as RelativeLayout

                if (layer.isShown) {
                    layer.visibility = View.GONE
                } else {
                    val zoomRange = playRTCHandler!!.cameraZoomRange
                    val zoomLevel = playRTCHandler!!.currentCameraZoom

                    hideFuntionUILayer()
                    zoomRangeBar!!.maximum = zoomRange.maxValue
                    zoomRangeBar!!.setProgressAndThumb(zoomLevel)
                    (findViewById(R.id.lb_camera_zoom_max) as TextView).text = zoomRange.maxValue.toString() + ""
                    (findViewById(R.id.lb_camera_zoom_min) as TextView).text = zoomRange.minValue.toString() + ""
                    (findViewById(R.id.lb_camera_zoom) as TextView).text = "Zoom: " + zoomLevel
                    layer.visibility = View.VISIBLE
                }
            }
        })

        zoomRangeBar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser == false) {
                    return
                }
                zoomRangeBar!!.setProgressAndThumb(progress)
                (findViewById(R.id.lb_camera_zoom) as TextView).text = "Zoom: " + progress
                playRTCHandler!!.setCameraZoom(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })


        /* 로컬뷰 미러 모드 전환 버튼 */
        val btnMirror = this.findViewById(R.id.btn_mirror) as ImageButton

        btnMirror.setOnClickListener(object : View.OnClickListener {  //Button->view
            override fun onClick(v: View) {
                val layer = findViewById(R.id.btn_mirror_layer) as RelativeLayout

                if (layer.isShown) {
                    layer.visibility = View.GONE
                } else {
                    hideFuntionUILayer()
                    layer.visibility = View.VISIBLE
                }
            }
        })

        (this.findViewById(R.id.btn_mirror_on) as Button).setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {   //Button->view
                val layer = findViewById(R.id.btn_mirror_layer) as RelativeLayout
                val view = videoLayer!!.localView

                (findViewById(R.id.lb_btn_mirror) as TextView).text = "미러-On"
                view.isMirror = true
                layer.visibility = View.GONE
            }
        })

        (this.findViewById(R.id.btn_mirror_off) as Button).setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {   //Button->view
                val layer = findViewById(R.id.btn_mirror_layer) as RelativeLayout
                val view = videoLayer!!.localView

                (findViewById(R.id.lb_btn_mirror) as TextView).text = "미러-Off"
                view.isMirror = false
                layer.visibility = View.GONE
            }
        })

        /*채팅 기능*/
        val chat = findViewById(R.id.btn_chat) as ImageButton
        val edit = findViewById(R.id.editText) as EditText
        val text = findViewById(R.id.textView) as TextView
        val btn_server = findViewById(R.id.btn_server) as Button
        val btn_client = findViewById(R.id.btn_client) as Button
        val btn_send = findViewById(R.id.btn_send) as Button
        val btn_sticker = findViewById(R.id.btn_sticker) as ImageButton

        text.movementMethod = ScrollingMovementMethod()
        chat.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                if (edit.isShown) {
                    edit.setVisibility(View.GONE)
                    text.setVisibility(View.GONE)
                    btn_server.setVisibility(View.GONE)
                    btn_client.setVisibility(View.GONE)
                    btn_send.setVisibility(View.GONE)
                    btn_sticker.setVisibility(View.GONE)
                    Toast.makeText(this@PlayRTCActivity, "채팅 비활성화", Toast.LENGTH_SHORT).show()
                } else {
                    edit.setVisibility(View.VISIBLE)
                    text.setVisibility(View.VISIBLE)
                    btn_server.setVisibility(View.VISIBLE)
                    btn_client.setVisibility(View.VISIBLE)
                    btn_send.setVisibility(View.VISIBLE)
                    btn_sticker.setVisibility(View.VISIBLE)
                    Toast.makeText(this@PlayRTCActivity, "채팅 활성화", Toast.LENGTH_SHORT).show()
                }
            }
        })

        var msg = ""
        var msgs = SpannableString("")
        val send_t = SpannableString("나 : ")
        val reci_t = SpannableString("상대방 : ")

        fun changeEmoticon(text: String): SpannableString {
            var result = SpannableString(text)
            var drawable: Drawable

            when (text) {
                "(good)" -> {
                    drawable = resources.getDrawable(R.drawable.good)
                    drawable.setBounds(0, 0, 60, 60)
                    result.setSpan(ImageSpan(drawable, ImageSpan.ALIGN_BASELINE), 0, text.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                }
                "(ok)" -> {
                    System.out.println("OK")
                    drawable = resources.getDrawable(R.drawable.ok)
                    drawable.setBounds(0, 0, 60, 60)
                    result.setSpan(ImageSpan(drawable, ImageSpan.ALIGN_BASELINE), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                "(flower)" -> {
                    drawable = resources.getDrawable(R.drawable.flower)
                    drawable.setBounds(0, 0, 60, 60)
                    result.setSpan(ImageSpan(drawable, ImageSpan.ALIGN_BASELINE), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                "(heart)" -> {
                    drawable = resources.getDrawable(R.drawable.heart)
                    drawable.setBounds(0, 0, 60, 60)
                    result.setSpan(ImageSpan(drawable, ImageSpan.ALIGN_BASELINE), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                "(merong)" -> {
                    drawable = resources.getDrawable(R.drawable.merong)
                    drawable.setBounds(0, 0, 60, 60)
                    result.setSpan(ImageSpan(drawable, ImageSpan.ALIGN_BASELINE), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                "(cong)" -> {
                    drawable = resources.getDrawable(R.drawable.cong)
                    drawable.setBounds(0, 0, 60, 60)
                    result.setSpan(ImageSpan(drawable, ImageSpan.ALIGN_BASELINE), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                else -> {
                    result = SpannableString(text)
                }
            }
            return result
        }

        fun scrollBottom(textView: TextView) {
            var scrollY = textView.layout.getLineTop(textView.lineCount) - textView.height

            if (scrollY > 0) {
                textView.scrollTo(0, scrollY)
            } else {
                textView.scrollTo(0, 0)
            }
        }

        fun send(msg: String) {
            edit.text.clear()
            msgs = changeEmoticon(msg)
            text.append(send_t)
            text.append(msgs)
            text.append("\n")
            scrollBottom(text)
        }

        fun recieve() {
            text.append(reci_t)
            text.append(msgs)
            text.append("\n")
            scrollBottom(text)
        }

        btn_server.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                Toast.makeText(applicationContext, "기다려 주세요.", Toast.LENGTH_SHORT).show()
                server_stack = 1
                //Android API14버전이상 부터 네트워크 작업은 무조건 별도의 Thread에서 실행 해야함.
                Thread(object : Runnable {
                    override fun run() {
                        try {
                            //서버소켓 생성.
                            serversocket = ServerSocket(PORT)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }

                        try {
                            //서버에 접속하는 클라이언트 소켓 얻어오기(클라이언트가 접속하면 클라이언트 소켓 리턴)
                            socket = serversocket.accept() //서버는 클라이언트가 접속할 때까지 여기서 대기...
                            //여기 까지 왔다는 것은 클라이언트가 접속했다는 것을 의미하므로
                            //클라이언트와 데이터를 주고 받기 위한 통로구축..
                            inputStream = DataInputStream(socket.getInputStream()) //클라이언트로 부터 메세지를 받기 위한 통로
                            outputStream = DataOutputStream(socket.getOutputStream()) //클라이언트로 메세지를 보내기 위한 통로
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }

                        //클라이언트가 접속을 끊을 때까지 무한반복하면서 클라이언트의 메세지 수신
                        while (isConnected) {
                            try {
                                msg = inputStream.readUTF()
                                msgs = changeEmoticon(msg)
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                            //클라이언트로부터 읽어들인 메시지msg를 TextView에 출력..
                            //안드로이드는 오직 main Thread 만이 UI를 변경할 수 있기에
                            //네트워크 작업을 하는 이 Thread에서는 TextView의 글씨를 직접 변경할 수 없음.
                            //runOnUiThread()는 별도의 Thread가 main Thread에게 UI 작업을 요청하는 메소드임.
                            runOnUiThread(object : Runnable {
                                override fun run() {
                                    recieve()
                                }
                            })
                        }//while..
                    }//run method...
                }).start() //Thread 실행..
            }
        })

        btn_client.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                Toast.makeText(applicationContext, "IP를 입력해주세요", Toast.LENGTH_SHORT).show()
                client_stack = 1
            }
        })

        btn_send.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                if (client_stack == 1) {
                    if (send_stack != 0) {
                        val msg = edit.text.toString()
                        send(msg)

                        if (outputStream == null) return    //서버와 연결되어 있지 않다면 전송불가..
                        //네트워크 작업이므로 Thread 생성
                        Thread(Runnable {
                            //서버로 보낼 메세지 EditText로 부터 얻어오기
                            try {
                                outputStream!!.writeUTF(msg)  //서버로 메세지 보내기.UTF 방식으로(한글 전송가능...)
                                outputStream!!.flush()        //다음 메세지 전송을 위해 연결통로의 버퍼를 지워주는 메소드..
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }//run method..
                        ).start() //Thread 실행..
                    } else {
                        send_stack++
                        Thread(Runnable {
                            try {
                                ip = edit.text.toString()//IP 주소가 작성되어 있는 EditText에서 서버 IP 얻어오기
                                //서버와 연결하는 소켓 생성..
                                socket = Socket(InetAddress.getByName(ip), PORT)
                                runOnUiThread {
                                    edit.text.clear()
                                }
                                //여기까지 왔다는 것을 예외가 발생하지 않았다는 것이므로 소켓 연결 성공..
                                //서버와 메세지를 주고받을 통로 구축
                                inputStream = DataInputStream(socket.getInputStream())
                                outputStream = DataOutputStream(socket.getOutputStream())
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }

                            //서버와 접속이 끊길 때까지 무한반복하면서 서버의 메세지 수신
                            while (true) {
                                try {
                                    msg = inputStream.readUTF() //서버 부터 메세지가 전송되면 이를 UTF형식으로 읽어서 String 으로 리턴
                                    msgs = changeEmoticon(msg)
                                    //서버로부터 읽어들인 메시지msg를 TextView에 출력..
                                    //안드로이드는 오직 main Thread 만이 UI를 변경할 수 있기에
                                    //네트워크 작업을 하는 이 Thread에서는 TextView의 글씨를 직접 변경할 수 없음.
                                    //runOnUiThread()는 별도의 Thread가 main Thread에게 UI 작업을 요청하는 메소드임.
                                    runOnUiThread {
                                        recieve()
                                    }
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }
                            }//while
                        }//run method...
                        ).start()//Thread 실행..
                    }
                } else if (server_stack == 1) {
                    val msg = edit.getText().toString()
                    send(msg)

                    if (outputStream == null) return  //클라이언트와 연결되어 있지 않다면 전송불가..
                    //네트워크 작업이므로 Thread 생성
                    Thread(object : Runnable {
                        override fun run() {
                            try {
                                outputStream!!.writeUTF(msg) //클라이언트로 메세지 보내기.UTF 방식으로(한글 전송가능...)
                                outputStream!!.flush()   //다음 메세지 전송을 위해 연결통로의 버퍼를 지워주는 메소드..
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }
                    }).start() //Thread 실행..
                }
            }
        })

        val btn_good = findViewById(R.id.btn_good) as ImageButton
        val btn_ok = findViewById(R.id.btn_ok) as ImageButton
        val btn_flower = findViewById(R.id.btn_flower) as ImageButton
        val btn_heart = findViewById(R.id.btn_heart) as ImageButton
        val btn_cong = findViewById(R.id.btn_cong) as ImageButton
        val btn_merong = findViewById(R.id.btn_merong) as ImageButton

        btn_good.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                if (server_stack == 1 || (client_stack == 1 && send_stack != 0)) {
                    msg = "(good)"
                    send(msg)

                    if (outputStream == null) return    //서버와 연결되어 있지 않다면 전송불가..
                    //네트워크 작업이므로 Thread 생성
                    Thread(Runnable {
                        try {
                            outputStream!!.writeUTF(msg)  //서버로 메세지 보내기.UTF 방식으로(한글 전송가능...)
                            outputStream!!.flush()        //다음 메세지 전송을 위해 연결통로의 버퍼를 지워주는 메소드..
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    ).start() //Thread 실행..
                }
            }
        })

        btn_ok.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                if (server_stack == 1 || (client_stack == 1 && send_stack != 0)) {
                    msg = "(ok)"
                    send(msg)

                    if (outputStream == null) return    //서버와 연결되어 있지 않다면 전송불가..
                    //네트워크 작업이므로 Thread 생성
                    Thread(Runnable {
                        try {
                            outputStream!!.writeUTF(msg)  //서버로 메세지 보내기.UTF 방식으로(한글 전송가능...)
                            outputStream!!.flush()        //다음 메세지 전송을 위해 연결통로의 버퍼를 지워주는 메소드..
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    ).start() //Thread 실행..
                }
            }
        })

        btn_flower.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                if (server_stack == 1 || (client_stack == 1 && send_stack != 0)) {
                    msg = "(flower)"
                    send(msg)

                    if (outputStream == null) return    //서버와 연결되어 있지 않다면 전송불가..
                    //네트워크 작업이므로 Thread 생성
                    Thread(Runnable {
                        try {
                            outputStream!!.writeUTF(msg)  //서버로 메세지 보내기.UTF 방식으로(한글 전송가능...)
                            outputStream!!.flush()        //다음 메세지 전송을 위해 연결통로의 버퍼를 지워주는 메소드..
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    ).start() //Thread 실행..
                }
            }
        })

        btn_heart.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                if (server_stack == 1 || (client_stack == 1 && send_stack != 0)) {
                    msg = "(heart)"
                    send(msg)

                    if (outputStream == null) return    //서버와 연결되어 있지 않다면 전송불가..
                    //네트워크 작업이므로 Thread 생성
                    Thread(Runnable {
                        try {
                            outputStream!!.writeUTF(msg)  //서버로 메세지 보내기.UTF 방식으로(한글 전송가능...)
                            outputStream!!.flush()        //다음 메세지 전송을 위해 연결통로의 버퍼를 지워주는 메소드..
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    ).start() //Thread 실행..
                }
            }
        })

        btn_cong.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                if (server_stack == 1 || (client_stack == 1 && send_stack != 0)) {
                    msg = "(cong)"
                    send(msg)

                    if (outputStream == null) return    //서버와 연결되어 있지 않다면 전송불가..
                    //네트워크 작업이므로 Thread 생성
                    Thread(Runnable {
                        try {
                            outputStream!!.writeUTF(msg)  //서버로 메세지 보내기.UTF 방식으로(한글 전송가능...)
                            outputStream!!.flush()        //다음 메세지 전송을 위해 연결통로의 버퍼를 지워주는 메소드..
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    ).start() //Thread 실행..
                }
            }
        })

        btn_merong.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                if (server_stack == 1 || (client_stack == 1 && send_stack != 0)) {
                    msg = "(merong)"
                    send(msg)

                    if (outputStream == null) return    //서버와 연결되어 있지 않다면 전송불가..
                    //네트워크 작업이므로 Thread 생성
                    Thread(Runnable {
                        try {
                            outputStream!!.writeUTF(msg)  //서버로 메세지 보내기.UTF 방식으로(한글 전송가능...)
                            outputStream!!.flush()        //다음 메세지 전송을 위해 연결통로의 버퍼를 지워주는 메소드..
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    ).start() //Thread 실행..
                }
            }
        })
    }

    /* Video View ShowSnapshot 기능 버튼 */
    private fun initVideoViewShowSnapshotFunctionUIControls() {
        /* snapshot 레이어 보기 버튼 */
        val btnShowSnapshot = this.findViewById(R.id.btn_show_snapshot) as ImageButton

        btnShowSnapshot.setOnClickListener(object : View.OnClickListener {  //Button->view
            override fun onClick(v: View) {
                if (snapshotLayer!!.isShown == false) {
                    hideFuntionUILayer()
                    snapshotLayer!!.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun hideFuntionUILayer() {
        (findViewById(R.id.btn_mirror_layer) as RelativeLayout).visibility = View.GONE
        (findViewById(R.id.btn_camera_zoom_layer) as RelativeLayout).visibility = View.GONE
        (findViewById(R.id.btn_white_balance_layer) as RelativeLayout).visibility = View.GONE
    }

    private fun refreshGallery(file: File) {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        mediaScanIntent.data = Uri.fromFile(file)
        sendBroadcast(mediaScanIntent)
    }

    /*스냅샷*/
    private fun initSnapshotControlls() {
        if (snapshotLayer != null && videoLayer != null) {
            // Snapshot 버튼과 이미지 배치등의 자식 요소를 동적으로 생성하여 Layout 구성
            // Snapshot 요청과 이미지를 전달하기 위한 인터페이스 등록
            snapshotLayer!!.createControls { local ->
                if (local && videoLayer!!.localView != null) {
                    /*
                     * Snapshot 이미지 요청
                     */
                    videoLayer!!.localView.snapshot { image ->
                        val w = image.width
                        val h = image.height
                        Log.e("SNAP-SHOT", "snapshot Bitmap[" + w + "x" + h + "].....")
                        /*
                         * Snapshot 이미지 출력
                         */
                        snapshotLayer!!.setSnapshotImage(image)

                        try {
                            val sdCard = Environment.getExternalStorageDirectory()   //저장소 디렉토리 받아오기
                            val dir = File(sdCard.absolutePath + "/test")     //위치+하위디렉토리에 빈파일 만들기
                            val fileName = System.currentTimeMillis().toString() + ".png" //현재시간으로 사진 이름 저장
                            val outFile = File(dir, fileName)   //dir 위치에다가 fileName이라는 파일을 만든다
                            var out = FileOutputStream(outFile)

                            dir.mkdirs()
                            image.compress(Bitmap.CompressFormat.PNG, 100, out)
                            out.close()
                            refreshGallery(outFile) //갤러리 갱신
                        } catch (e: Exception) {
                            Toast.makeText(this, "error", Toast.LENGTH_SHORT).show()
                            e.printStackTrace()
                        }
                    }
                } else if (local == false && videoLayer!!.remoteView != null) {
                    /*
                     * Snapshot 이미지 요청
                     */
                    videoLayer!!.remoteView.snapshot { image ->
                        val w = image.width
                        val h = image.height
                        Log.e("SNAP-SHOT", "snapshot Bitmap[" + w + "x" + h + "].....")

                        /*
                         * Snapshot 이미지 출력
                         */
                        snapshotLayer!!.setSnapshotImage(image)
                        try {
                            val sdCard = Environment.getExternalStorageDirectory()
                            val dir = File(sdCard.absolutePath + "/test")
                            val fileName = System.currentTimeMillis().toString() + ".png"
                            val outFile = File(dir, fileName)
                            var out = FileOutputStream(outFile)

                            dir.mkdirs()
                            image.compress(Bitmap.CompressFormat.PNG, 100, out)
                            out.close()
                            refreshGallery(outFile)
                        } catch (e: Exception) {
                            Toast.makeText(this, "error", Toast.LENGTH_SHORT).show()
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val LOG_TAG = "PlayRTCActivity"
        val PORT: Int = 10001
    }
}