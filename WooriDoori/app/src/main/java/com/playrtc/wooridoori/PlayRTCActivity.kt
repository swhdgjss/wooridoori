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
 * - PlayRTCLogView logView
 *     PlayRTC 로그를 출력하기위해 TextView를 확장한 Class
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
    var channelInfoPopup: PlayRTCChannelView?=null
        private set

    /*
     * PlayRTC-Handler Class
     * PlayRTC 메소드 , PlayRTC객체의 이벤트 처리
     */
    /*
     * PlayRTCHandler 인스턴스를 반환한다.
     * @return PlayRTCHandler
     */
    var playRTCHandler: PlayRTCHandler?=null
        private set

    /*
     * PlayRTCVideoView를 위한 부모 뷰 그룹
     */
    var videoLayer: PlayRTCVideoViewGroup?=null
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
    var rtcDataHandler: PlayRTCDataChannelHandler?=null
        private set


    /*
     * 로그 출력 TextView
     *
     * @see com.playrtc.sample.view.PlayRTCLogView
     */
    private var logView: PlayRTCLogView?=null

    /*
     * PlayRTC P2P Status report 출력 TextView
     */
    private var txtStatReport: TextView?=null


    /*
     * 영상 뷰 Snapshot 이미지 요청 및 이미지 출력을 위한 뷰 그룹
     */
    private var snapshotLayer: PlayRTCSnapshotView?=null

    /*
     * isCloesActivity가 false이면 Dialog를 통해 사용자의 종료 의사를 확인하고<br>
     * Activity를 종료 처리. 만약 채널에 입장한 상태이면 먼저 채널을 종료한다.
     */
    private var isCloesActivity=false

    /*
     * 영상 뷰를 사용하지 않는 경우 로그 뷰를 화면 중앙에 1회 위치 시키기 위한 변수
     * onWindowFocusChanged에서 로그뷰 Layout을 조정 하므로 필요함.
     */
    private var isResetLogViewArea=false

    private var zoomRangeBar: PlayRTCVerticalSeekBar?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rtc)


        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)

        val intent=intent

        val channelRing=intent.getStringExtra("channelRing")
        val videoCodec=intent.getStringExtra("videoCodec")
        val audioCodec=intent.getStringExtra("audioCodec")

        // UI 인스턴스 변수 처리
        initUIControls()

        initSnapshotControlls()

        chatting()

        playRTCHandler=PlayRTCHandler(this)
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
        this.rtcDataHandler=PlayRTCDataChannelHandler(this)

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

        /*
        // Layout XML을 사용하지 않고 소스 코드에서 직접 샹성하는 경우
        if (hasFocus && videoLayer.isCreatedVideoView() == false) {

            // 4. 영상 스트림 출력을 위한 PlayRTCVideoView 동적 생성
            videoLayer.createVideoView();
        }
        */

        // Layout XML에 VideoView를 기술한 경우. v2.2.6
        if (hasFocus && videoLayer!!.isInitVideoView == false) {
            // 4. 영상 스트림 출력을 위한 PlayRTCVideoView 초기화
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
            playRTCHandler=null
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
        // 팝업 창을 닫기 만 한다.
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
            val alert=AlertDialog.Builder(this)
            alert.setTitle("PlayRTC")
            alert.setMessage("PlayRTC를 종료하겠습니까?")

            alert.setPositiveButton("종료") { dialog, which ->
                dialog.dismiss()
                // 채널에 입장한 상태라면 채널을 먼저 종료한다.
                // 종료 이벤트에서 isCloesActivity를 true로 설정하고 onBackPressed()를 호출하여
                // Activity를 종료 처리
                if (playRTCHandler!!.isChannelConnected == true) {
                    isCloesActivity=false
                    // PlayRTC 플랫폼 채널을 종료한다.
                    playRTCHandler!!.disconnectChannel()

                } else {
                    isCloesActivity=true
                    onBackPressed()
                }// 채널에 입장한 상태가 아니라면 바로 종료 처리
            }
            alert.setNegativeButton("취소") { dialog, whichButton ->
                dialog.dismiss()
                isCloesActivity=false
            }
            alert.show()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        when (this.resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
            }
        }
        super.onConfigurationChanged(newConfig)
    }

    /*
     * 로컬 영상 PlayRTCVideoView 인스턴스를 반환한다.
     * @return PlayRTCVideoView
     */
    val localVideoView: PlayRTCVideoView
        get()=videoLayer!!.localView

    /*
     * 상대방 영상 PlayRTCVideoView 인스턴스를 반환한다.
     * @return PlayRTCVideoView
     */
    val remoteVideoView: PlayRTCVideoView
        get()=videoLayer!!.remoteView

    /*
     * PlayRTCActivity를 종료한다.
     * PlayRTCHandler에서 채널이 종료 할 때 호출한다.
     * @param isClose boolean, 종료 처리 시 사용자의 종료 으의사를 묻는 여부
     */
    fun setOnBackPressed(isClose: Boolean) {
        if (channelInfoPopup!!.isShown) {
            channelInfoPopup!!.hide(0)
        }
        isCloesActivity=isClose
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
        txtStatReport!!.post { txtStatReport!!.text=resport }
    }

    /*
     * Layout 관련 인스턴스 설정 및 이벤트 정의
     */
    private fun initUIControls() {
        /* 채널 팝업 뷰 */
        channelInfoPopup=findViewById(R.id.channel_info) as PlayRTCChannelView

        /*video 스트림 출력을 위한 PlayRTCVideoView의 부모 ViewGroup */
        videoLayer=findViewById(R.id.videoarea) as PlayRTCVideoViewGroup

        /*video 스트림 출력을 위한 PlayRTCVideoView의 부모 ViewGroup */
        videoLayer=findViewById(R.id.videoarea) as PlayRTCVideoViewGroup

        /* 로그 출력 TextView */
        logView=this.findViewById(R.id.logtext) as PlayRTCLogView

        snapshotLayer=this.findViewById(R.id.snapshot_area) as PlayRTCSnapshotView

        /* PlayRTC P2P Status report 출력 TextView */
        txtStatReport=this.findViewById(R.id.txt_stat_report) as TextView
        val text="Local\n ICE:none\n Frame:0x0x0\n Bandwidth[0bps]\n RTT[0]\n eModel[-]\n VFLost[0]\n AFLost[0]\n\nRemote\n ICE:none\n Frame:0x0x0\n Bandwidth[0bps]\n VFLost[0]\n AFLost[0]"
        txtStatReport!!.text=text

        /* 채널 팝업 버튼 */
        val channelPopup=this.findViewById(R.id.btn_channel) as Button
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

        /* 후방 카메라 사용 시 플래쉬 On/Off 전환 버튼 */
        initSwitchVideoCameraFlashFunctionUIControls()

        /* 로그뷰  토글 버튼 */
        initLogViewFunctionUIControls()

        /* Peer 채널 퇴장/종료 버튼 */
        initChannelCloseFunctionUIControls()

        /* 미디어 스트림 Mute 버튼 */
        initMediaMuteFunctionUIControls()

        /* 로컬뷰 미러 모드 전환 버튼 */
        initVideoViewMirrorFunctionUIControls()

        /* 카메라 영상 추가 회전 각 버튼 */
        initCameraDegreeFunctionUIControls()

        /* 카메라 영상 Zoom 기능 버튼 */
        initCameraZoomFunctionUIControls()

        /* 카메라 Whitebalance 기능 버튼 */
        initCameraWhitebalanceFunctionUIControls()

        /* Video View ShowSnapshot 기능 버튼 */
        initVideoViewShowSnapshotFunctionUIControls()

        /* Menu 기능 버튼*/
        initMenuControls()
    }

    /* 카메라 전/후방 전환 버튼 */
    private fun initSwitchVideoCameraFunctionUIControls() {
        val cameraBtn=this.findViewById(R.id.btn_switch_camera) as Button

        cameraBtn.setOnClickListener(object : View.OnClickListener {  //Button->view
            override fun onClick(v: View) {
                playRTCHandler!!.switchVideoCamera()
            }
        })
    }

    /* 후방 카메라 사용 시 플래쉬 On/Off 전환 버튼 */
    private fun initSwitchVideoCameraFlashFunctionUIControls() {
        val flashBtn=this.findViewById(R.id.btn_switch_flash) as Button
        /* 후방 카메라 플래쉬 On/Off, 후방 카메라 사용 시 작동  */

        flashBtn.setOnClickListener(object : View.OnClickListener {   //Button->viewv
            override fun onClick(v: View) {
                if (playRTCHandler == null) {
                    return
                }
                playRTCHandler!!.switchBackCameraFlash()
            }
        })
    }


    private fun initLogViewFunctionUIControls() {
        /* 로그뷰  토글 버튼 이벤트 처리 */
        val btnLog=this.findViewById(R.id.btn_log) as Button

        btnLog.setOnClickListener(object : View.OnClickListener {
            //Button->view
            override fun onClick(v: View) {
                if (logView!!.isShown == false) {
                    logView!!.show()
                    (v as Button).text="로그닫기"
                } else {
                    logView!!.hide()
                    (v as Button).text="로그보기"
                }
            }
        })
    }

    /* Peer 채널 종료 버튼 */
    private fun initChannelCloseFunctionUIControls() {
        val btnCloseChannel=this.findViewById(R.id.btn_chClose) as ImageButton
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
        val btnMuteLVideo=this.findViewById(R.id.btn_local_vmute) as Button
        /* Local Video Mute 처리시 로컬 영상 스트림은 화면에 출력이 안되며 상대방에게 전달이 되지 않는다. */
        btnMuteLVideo.setOnClickListener(object : View.OnClickListener {  //Button->view
            override fun onClick(v: View) {
                val b=v as Button
                if (playRTCHandler != null) {
                    val text=b.text as String
                    val setMute=text.endsWith("-OFF")
                    playRTCHandler!!.setLocalVideoPause(setMute)
                    b.text=if (setMute == true) "VIDEO-ON" else "VIDEO-OFF"
                }
            }
        })

        /* Local Audio Mute 버튼 */
        val btnMuteLAudio=this.findViewById(R.id.btn_local_amute) as Button
        /* Local Audio Mute 처리시 로컬 음성 스트림은 상대방에게 전달이 되지 않는다. */
        btnMuteLAudio.setOnClickListener(object : View.OnClickListener {  //Button->view
            override fun onClick(v: View) {
                val b=v as Button
                if (playRTCHandler != null) {
                    val text=b.text as String
                    val setMute=text.endsWith("-OFF")
                    playRTCHandler!!.setLocalAudioMute(setMute)
                    b.text=if (setMute == true) "AUDIO-ON" else "AUDIO-OFF"
                }
            }
        })

        /* Remote Video Mute 버튼 */
        val btnMuteRVideo=this.findViewById(R.id.btn_remote_vmute) as Button
        /* Remote Video Mute 처리시 상대방의 영상 스트림은 수신되나 화면에는 출력이 되지 않는다. */
        btnMuteRVideo.setOnClickListener(object : View.OnClickListener {  //Button->view
            override fun onClick(v: View) {
                val b=v as Button
                if (playRTCHandler != null) {
                    val text=b.text as String
                    val setMute=text.endsWith("-OFF")
                    playRTCHandler!!.setRemoteVideoPause(setMute)
                    b.text=if (setMute == true) "VIDEO-ON" else "VIDEO-OFF"
                }
            }
        })

        /* Remote Audio Mute 버튼 */
        val btnMuteRAudio=this.findViewById(R.id.btn_remote_amute) as Button
        /* Remote Video Mute 처리시 상대방 영상 스트림은 수신되나 소리는 출력되지 않는다. */
        btnMuteRAudio.setOnClickListener(object : View.OnClickListener {  //Button->view
            override fun onClick(v: View) {
                val b=v as Button
                if (playRTCHandler != null) {
                    val text=b.text as String
                    val setMute=text.endsWith("-OFF")
                    playRTCHandler!!.setRemoteAudioMute(setMute)
                    b.text=if (setMute == true) "AUDIO-ON" else "AUDIO-OFF"
                }
            }
        })

    }

    /* 로컬뷰 미러 모드 전환 버튼 */
    private fun initVideoViewMirrorFunctionUIControls() {
        val btnMirror=this.findViewById(R.id.btn_mirror) as Button

        btnMirror.setOnClickListener(object : View.OnClickListener {  //Button->view
            override fun onClick(v: View) {
                val layer=findViewById(R.id.btn_mirror_layer) as RelativeLayout
                if (layer.isShown) {
                    layer.visibility=View.GONE
                } else {
                    hideFuntionUILayer()
                    layer.visibility=View.VISIBLE
                }
            }
        })
        (this.findViewById(R.id.btn_mirror_on) as Button).setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {   //Button->view
                (findViewById(R.id.lb_btn_mirror) as TextView).text="미러-On"
                val view=videoLayer!!.localView
                view.isMirror=true
            }
        })
        (this.findViewById(R.id.btn_mirror_off) as Button).setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {   //Button->view
                (findViewById(R.id.lb_btn_mirror) as TextView).text="미러-Off"
                val view=videoLayer!!.localView
                view.isMirror=false
            }
        })
    }

    //메뉴~
    private fun initMenuControls() {
        val btnMenu=this.findViewById(R.id.btn_menu) as ImageButton
        btnMenu.setOnClickListener(object : View.OnClickListener {  //Button->view
            override fun onClick(v: View) {
                val layer=findViewById(R.id.btn_menu_layer) as RelativeLayout
                if (layer.isShown) {
                    layer.visibility=View.GONE
                } else {
                    hideFuntionUILayer()
                    layer.visibility=View.VISIBLE
                }
            }
        })
        val log=findViewById(R.id.btn_log) as Button
        log.setOnClickListener(object : View.OnClickListener {  //Button->view
            override fun onClick(v: View) {
                if (logView!!.isShown == false) {
                    logView!!.show()
                    (v as Button).text="로그닫기"
                } else {
                    logView!!.hide()
                    (v as Button).text="로그보기"
                }
            }

        })
        val channelPopup=this.findViewById(R.id.btn_channel) as Button
        channelPopup.setOnClickListener {
            if (channelInfoPopup!!.isShown) {
                channelInfoPopup!!.hide(0)
            } else {
                channelInfoPopup!!.showChannelList()
                channelInfoPopup!!.show(0)
            }
        }
    }

    /* 카메라 영상 추가 회전 각 버튼 v2.2.9 */
    private fun initCameraDegreeFunctionUIControls() {
        val btnCameraDegree=this.findViewById(R.id.btn_camera_degree) as Button
        btnCameraDegree.setOnClickListener(object : View.OnClickListener {  //Button->view
            override fun onClick(v: View) {
                val layer=findViewById(R.id.btn_camera_degree_layer) as RelativeLayout
                if (layer.isShown) {
                    layer.visibility=View.GONE
                } else {
                    hideFuntionUILayer()
                    layer.visibility=View.VISIBLE
                }
            }
        })
        val cameraRotation0=findViewById(R.id.btn_camera_0) as Button
        cameraRotation0.setOnClickListener(object : View.OnClickListener {  //Button->view
            override fun onClick(v: View) {
                setCameraRotation(0)
            }
        })
        val cameraRotation90=this.findViewById(R.id.btn_camera_90) as Button
        cameraRotation90.setOnClickListener(object : View.OnClickListener {   //Button->view
            override fun onClick(v: View) {
                setCameraRotation(90)
            }
        })
        val cameraRotation180=this.findViewById(R.id.btn_camera_180) as Button
        cameraRotation180.setOnClickListener(object : View.OnClickListener {   //Button->view
            override fun onClick(v: View) {
                setCameraRotation(180)
            }
        })
        val cameraRotation270=this.findViewById(R.id.btn_camera_270) as Button
        cameraRotation270.setOnClickListener(object : View.OnClickListener {   //Button->view
            override fun onClick(v: View) {
                setCameraRotation(270)
            }
        })
    }

    /* 카메라 영상 Zoom 기능 버튼 v2.3.0 */
    private fun initCameraZoomFunctionUIControls() {
        val btnCameraZoom=this.findViewById(R.id.btn_camera_zoom) as ImageButton
        zoomRangeBar=this.findViewById(R.id.seekbar_camera_zoom) as PlayRTCVerticalSeekBar

        btnCameraZoom.setOnClickListener(object : View.OnClickListener { //Button->view
            override fun onClick(v: View) {

                if (playRTCHandler == null) {
                    return
                }
                val layer=findViewById(R.id.btn_camera_zoom_layer) as RelativeLayout
                if (layer.isShown) {
                    layer.visibility=View.GONE
                } else {

                    hideFuntionUILayer()

                    val zoomRange=playRTCHandler!!.cameraZoomRange
                    val zoomLevel=playRTCHandler!!.currentCameraZoom
                    zoomRangeBar!!.maximum=zoomRange.maxValue
                    zoomRangeBar!!.setProgressAndThumb(zoomLevel)
                    (findViewById(R.id.lb_camera_zoom_max) as TextView).text=zoomRange.maxValue.toString() + ""
                    (findViewById(R.id.lb_camera_zoom_min) as TextView).text=zoomRange.minValue.toString() + ""
                    (findViewById(R.id.lb_camera_zoom) as TextView).text="Zoom: " + zoomLevel


                    layer.visibility=View.VISIBLE
                }
            }
        })

        zoomRangeBar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser == false) {
                    return
                }
                zoomRangeBar!!.setProgressAndThumb(progress)
                (findViewById(R.id.lb_camera_zoom) as TextView).text="Zoom: " + progress
                playRTCHandler!!.setCameraZoom(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}

        })
    }

    /*채팅 버튼*/
    private fun chatting() {
        val text=findViewById(R.id.btn_chat) as Button
        val edit=findViewById(R.id.editText) as EditText
        val txt=findViewById(R.id.textView) as TextView
        txt.movementMethod = ScrollingMovementMethod()
        text.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                edit.setVisibility(View.VISIBLE)
                txt.setVisibility(View.VISIBLE)
                Toast.makeText(this@PlayRTCActivity, "메시지", Toast.LENGTH_SHORT).show()
            }
        });
    }

    /* 카메라 Whitebalance 기능 버튼 v2.3.0 */
    private fun initCameraWhitebalanceFunctionUIControls() {
        val btnCameraWbalance=this.findViewById(R.id.btn_white_balance) as Button

        btnCameraWbalance.setOnClickListener(object : View.OnClickListener {  //Button->view
            override fun onClick(v: View) {

                if (playRTCHandler == null) {
                    return
                }
                val layer=findViewById(R.id.btn_white_balance_layer) as RelativeLayout
                if (layer.isShown) {
                    layer.visibility=View.GONE
                } else {

                    hideFuntionUILayer()

                    val whiteBalance=playRTCHandler!!.cameraWhiteBalance
                    var labelText: String?=null

                    if (whiteBalance == PlayRTCWhiteBalance.Auto) {
                        labelText="자동"
                    } else if (whiteBalance == PlayRTCWhiteBalance.Incandescent) {
                        labelText="백열등"
                    } else if (whiteBalance == PlayRTCWhiteBalance.FluoreScent) {
                        labelText="형광등"
                    } else if (whiteBalance == PlayRTCWhiteBalance.DayLight) {
                        labelText="햇빛"
                    } else if (whiteBalance == PlayRTCWhiteBalance.CloudyDayLight) {
                        labelText="흐림"
                    } else if (whiteBalance == PlayRTCWhiteBalance.TwiLight) {
                        labelText="저녁빛"
                    } else if (whiteBalance == PlayRTCWhiteBalance.Shade) {
                        labelText="그늘"
                    }
                    (findViewById(R.id.white_balance_label) as TextView).text=labelText
                    layer.visibility=View.VISIBLE
                }
            }
        })
        (this.findViewById(R.id.btn_white_balance_auto) as Button).setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {             //Button->view
                if (playRTCHandler!!.setCameraWhiteBalance(PlayRTCWhiteBalance.Auto)) {
                    (findViewById(R.id.white_balance_label) as TextView).text="자동"
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
                    (findViewById(R.id.white_balance_label) as TextView).text="백열등"
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
                    (findViewById(R.id.white_balance_label) as TextView).text="형광등"
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
                    (findViewById(R.id.white_balance_label) as TextView).text="햇빛"
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
                    (findViewById(R.id.white_balance_label) as TextView).text="흐림"
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
                    (findViewById(R.id.white_balance_label) as TextView).text="저녁빛"
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
                    (findViewById(R.id.white_balance_label) as TextView).text="그늘"
                }
            }
        })
    }

    /* Video View ShowSnapshot 기능 버튼 */
    private fun initVideoViewShowSnapshotFunctionUIControls() {
        /* snapshot 레이어 보기 버튼 */
        val btnShowSnapshot=this.findViewById(R.id.btn_show_snapshot) as Button

        btnShowSnapshot.setOnClickListener(object : View.OnClickListener {  //Button->view
            override fun onClick(v: View) {
                if (snapshotLayer!!.isShown == false) {
                    hideFuntionUILayer()

                    snapshotLayer!!.visibility=View.VISIBLE
                }
            }
        })
    }

    private fun hideFuntionUILayer() {
        (findViewById(R.id.btn_mirror_layer) as RelativeLayout).visibility=View.GONE
        (findViewById(R.id.btn_camera_degree_layer) as RelativeLayout).visibility=View.GONE
        (findViewById(R.id.btn_camera_zoom_layer) as RelativeLayout).visibility=View.GONE
        (findViewById(R.id.btn_white_balance_layer) as RelativeLayout).visibility=View.GONE
        (findViewById(R.id.btn_exposure_compensation_layer) as RelativeLayout).visibility=View.GONE
        (findViewById(R.id.btn_menu_layer) as RelativeLayout).visibility=View.GONE
    }

    /**
     * 카메라 영상 회전 기능. v2.2.9
     * @param degree int 0 , 90, 180, 270
     */
    private fun setCameraRotation(degree: Int) {
        if (playRTCHandler != null) {

            playRTCHandler!!.setCameraRotation(degree)
            val text=findViewById(R.id.lb_camera_degree) as TextView
            text.text=degree.toString() + "도"
        }
    }

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
                        val w=image.width
                        val h=image.height
                        Log.e("SNAP-SHOT", "snapshot Bitmap[" + w + "x" + h + "].....")

                        /*
                                 * Snapshot 이미지 출력
                                 */
                        snapshotLayer!!.setSnapshotImage(image)
                    }
                } else if (local == false && videoLayer!!.remoteView != null) {
                    /*
                         * Snapshot 이미지 요청
                         */
                    videoLayer!!.remoteView.snapshot { image ->
                        val w=image.width
                        val h=image.height
                        Log.e("SNAP-SHOT", "snapshot Bitmap[" + w + "x" + h + "].....")

                        /*
                                 * Snapshot 이미지 출력
                                 */
                        snapshotLayer!!.setSnapshotImage(image)
                    }
                }
            }
        }
    }

    private fun resetLogViewArea() {
        if (isResetLogViewArea == true) {
            return
        }

        val screenDimensions=Point()
        val height=videoLayer!!.height

        // ViewGroup의 사이즈 재조정, 높이 기준으로 4(폭):3(높이)으로 재 조정
        // 4:3 = width:height ,  width = ( 4 * height) / 3
        val width=4.0f * height / 3.0f


        val logLayoutparam=RelativeLayout.LayoutParams(width.toInt(), height.toInt())
        logLayoutparam.addRule(RelativeLayout.CENTER_VERTICAL)
        logLayoutparam.addRule(RelativeLayout.CENTER_HORIZONTAL)
        logView!!.layoutParams=logLayoutparam

        val videoLayoutparam=RelativeLayout.LayoutParams(width.toInt(), height.toInt())
        videoLayoutparam.addRule(RelativeLayout.CENTER_VERTICAL)
        videoLayoutparam.addRule(RelativeLayout.CENTER_HORIZONTAL)
        videoLayer!!.layoutParams=videoLayoutparam

        isResetLogViewArea=true

    }

    companion object {
        private val LOG_TAG="PlayRTCActivity"
    }
}