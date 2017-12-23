package com.example.l.myapplication

import android.app.Activity
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.ScrollingMovementMethod
import android.text.style.ImageSpan
import android.view.View
import android.widget.EditText
import android.widget.TextView

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

class Server:Activity() {
    internal lateinit var serversocket:ServerSocket
    internal lateinit var socket:Socket
    internal lateinit var inputStream:DataInputStream
    internal var outputStream:DataOutputStream? = null
    internal lateinit var text_msg:TextView //클라이언트로부터 받을 메세지를 표시하는 TextView
    internal lateinit var edit_msg:EditText //클라이언트로 전송할 메세지를 작성하는 EditText
    internal var msg = ""
    internal var isConnected = true
    var msgs : SpannableString = SpannableString("")
    val send : SpannableString = SpannableString("나 : ")
    val reci : SpannableString = SpannableString("상대방 : ")

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)
        text_msg = findViewById<View>(R.id.text_massage_from_client) as TextView
        edit_msg = findViewById<View>(R.id.edit_message_to_client) as EditText
        text_msg.movementMethod = ScrollingMovementMethod();
    }

    //Button 클릭시 자동으로 호출되는 callback 메소드
    fun mOnClick(v:View) {
        when (v.getId()) {
            R.id.btn_start_server //채팅 서버 구축 및 클라이언트로 부터 메세지 받기
            ->
                //Android API14버전이상 부터 네트워크 작업은 무조건 별도의 Thread에서 실행 해야함.
                Thread(object:Runnable {
                    override fun run() {
                        // TODO Auto-generated method stub
                        try {
                            //서버소켓 생성.
                            serversocket = ServerSocket(PORT)
                        } catch (e:IOException) {
                            // TODO Auto-generated catch block
                            e.printStackTrace()
                        }

                        try {
                            //서버에 접속하는 클라이언트 소켓 얻어오기(클라이언트가 접속하면 클라이언트 소켓 리턴)
                            socket = serversocket.accept() //서버는 클라이언트가 접속할 때까지 여기서 대기...

                            //여기 까지 왔다는 것은 클라이언트가 접속했다는 것을 의미하므로
                            //클라이언트와 데이터를 주고 받기 위한 통로구축..
                            inputStream = DataInputStream(socket.getInputStream()) //클라이언트로 부터 메세지를 받기 위한 통로
                            outputStream = DataOutputStream(socket.getOutputStream()) //클라이언트로 메세지를 보내기 위한 통로
                        } catch (e:IOException) {
                            // TODO Auto-generated catch block
                            e.printStackTrace()
                        }

                        //클라이언트가 접속을 끊을 때까지 무한반복하면서 클라이언트의 메세지 수신
                        while (isConnected) {
                            try {
                                msg = inputStream.readUTF()
                                msgs = changeEmoticon(msg)
                            } catch (e:IOException) {
                                // TODO Auto-generated catch block
                                e.printStackTrace()
                            }

                            //클라이언트로부터 읽어들인 메시지msg를 TextView에 출력..
                            //안드로이드는 오직 main Thread 만이 UI를 변경할 수 있기에
                            //네트워크 작업을 하는 이 Thread에서는 TextView의 글씨를 직접 변경할 수 없음.
                            //runOnUiThread()는 별도의 Thread가 main Thread에게 UI 작업을 요청하는 메소드임.
                            runOnUiThread(object:Runnable {
                                public override fun run() {
                                    // TODO Auto-generated method stub
                                    recieve()
                                }
                            })
                            /////////////////////////////////////////////////////////////////////////////
                        }//while..
                    }//run method...
                }).start() //Thread 실행..
            R.id.btn_send_server // 클라이언트로 메세지 전송하기.
            -> {
                val msg = edit_msg.getText().toString()
                send(msg)

                if (outputStream == null) return  //클라이언트와 연결되어 있지 않다면 전송불가..
                //네트워크 작업이므로 Thread 생성
                Thread(object:Runnable {
                    public override fun run() {
                        // TODO Auto-generated method stub
                        //클라이언트로 보낼 메세지 EditText로 부터 얻어오기

                        try {
                            outputStream!!.writeUTF(msg) //클라이언트로 메세지 보내기.UTF 방식으로(한글 전송가능...)
                            outputStream!!.flush()   //다음 메세지 전송을 위해 연결통로의 버퍼를 지워주는 메소드..
                        } catch (e:IOException) {
                            // TODO Auto-generated catch block
                            e.printStackTrace()
                        }
                    }
                }).start() //Thread 실행..
            }
        }
    }

    fun changeEmoticon(text : String) : SpannableString {
        var result = SpannableString(text)
        var drawable : Drawable

        when(text) {
            "(good)" ->  {
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

    fun send(msg : String) {
        edit_msg.text.clear()
        msgs = changeEmoticon(msg)
        text_msg.append(send)
        text_msg.append(msgs)
        text_msg.append("\n")
    }

    fun recieve() {
        text_msg.append(reci)
        text_msg.append(msgs)
        text_msg.append("\n")
    }

    companion object {
        internal val PORT = 10001
    }
}