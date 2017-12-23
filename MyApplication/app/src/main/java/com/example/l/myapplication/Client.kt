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
import java.net.InetAddress
import java.net.Socket

class Client : Activity() {
    internal var ip = "192.168.193.140" //서버 단말기의 IP주소..
    //본 예제는 Genymotion 에뮬레이터 2대로 테스한 예제입니다.
    //Genymotion을 실행하면 각 에뮬레이터의 IP를 확인할 수 있습니다.
    internal lateinit var socket: Socket     //클라이언트의 소켓
    internal lateinit var inputStream: DataInputStream
    internal var outputStream: DataOutputStream? = null
    internal lateinit var text_msg: TextView  //서버로 부터 받은 메세지를 보여주는 TextView
    internal lateinit var edit_msg: EditText  //서버로 전송할 메세지를 작성하는 EditText
    internal lateinit var edit_ip: EditText   //서버의 IP를 작성할 수 있는 EditText
    internal var msg = ""
    var msgs : SpannableString = SpannableString("")
    val send : SpannableString = SpannableString("나 : ")
    val reci : SpannableString = SpannableString("상대방 : ")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client)
        text_msg = findViewById<View>(R.id.text_massage_from_server) as TextView
        text_msg.movementMethod = ScrollingMovementMethod();
        edit_msg = findViewById<View>(R.id.edit_message_to_server) as EditText
        edit_ip = findViewById<View>(R.id.edit_addressofserver) as EditText
        edit_ip.setText(ip)
    }

    //Button 클릭시 자동으로 호출되는 callback 메소드
    fun mOnClick(v: View) {
        when (v.id) {
            R.id.btn_connectserver//서버에 접속하고 서버로 부터 메세지 수신하기
            ->
                //Android API14버전이상 부터 네트워크 작업은 무조건 별도의 Thread에서 실행 해야함.
                Thread(Runnable {
                    // TODO Auto-generated method stub
                    try {
                        ip = edit_ip.text.toString()//IP 주소가 작성되어 있는 EditText에서 서버 IP 얻어오기
                        //서버와 연결하는 소켓 생성..
                        socket = Socket(InetAddress.getByName(ip), PORT)
                        //여기까지 왔다는 것을 예외가 발생하지 않았다는 것이므로 소켓 연결 성공..
                        //서버와 메세지를 주고받을 통로 구축
                        inputStream = DataInputStream(socket.getInputStream())
                        outputStream = DataOutputStream(socket.getOutputStream())
                    } catch (e: IOException) {
                        // TODO Auto-generated catch block
                        e.printStackTrace()
                    }

                    //서버와 접속이 끊길 때까지 무한반복하면서 서버의 메세지 수신
                    while (true) {
                        try {
                            msg = inputStream.readUTF() //서버 부터 메세지가 전송되면 이를 UTF형식으로 읽어서 String 으로 리턴
                            msgs = changeEmoticon(msg)
                            //var text = SpannableString(text_msg.text)
                            //text = TextUtils.concat(text, msgs) as SpannableString
                            //서버로부터 읽어들인 메시지msg를 TextView에 출력..
                            //안드로이드는 오직 main Thread 만이 UI를 변경할 수 있기에
                            //네트워크 작업을 하는 이 Thread에서는 TextView의 글씨를 직접 변경할 수 없음.
                            //runOnUiThread()는 별도의 Thread가 main Thread에게 UI 작업을 요청하는 메소드임.
                            runOnUiThread {
                                // TODO Auto-generated method stub
                                recieve()
                            }
                            //////////////////////////////////////////////////////////////////////////
                        } catch (e: IOException) {
                            // TODO Auto-generated catch block
                            e.printStackTrace()
                        }

                    }//while
                }//run method...
                ).start()//Thread 실행..
            R.id.btn_send_client //서버로 메세지 전송하기...
            -> {
                val msg = edit_msg.text.toString()
                send(msg)

                if (outputStream == null) return    //서버와 연결되어 있지 않다면 전송불가..
                //네트워크 작업이므로 Thread 생성
                Thread(Runnable {
                    // TODO Auto-generated method stub
                    //서버로 보낼 메세지 EditText로 부터 얻어오기

                    try {
                        outputStream!!.writeUTF(msg)  //서버로 메세지 보내기.UTF 방식으로(한글 전송가능...)
                        outputStream!!.flush()        //다음 메세지 전송을 위해 연결통로의 버퍼를 지워주는 메소드..
                    } catch (e: IOException) {
                        // TODO Auto-generated catch block
                        e.printStackTrace()
                    }
                }//run method..
                ).start() //Thread 실행..
            }
        }
    }

    fun changeEmoticon(text : String) : SpannableString{
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
        private val PORT = 10001 //서버에서 설정한 PORT 번호
    }
}