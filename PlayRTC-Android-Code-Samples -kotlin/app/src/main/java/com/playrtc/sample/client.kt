package com.playrtc.sample

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
//import org.apache.http.util.ByteArrayBuffer;
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class client : Activity() {
    private var html=""
    private var mHandler: Handler?=null
    private var socket: Socket?=null
    private val name: String?=null
    private var networkReader: BufferedReader?=null
    private var networkWriter: BufferedWriter?=null
    private val ip="xxx.xxx.xxx.xxx" // IP
    private val port=9999 // PORT번호
    override fun onStop() {
        // TODO Auto-generated method stub
        super.onStop()
        try {
            socket!!.close()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rtc)
        mHandler=Handler()

        try {
            setSocket(ip, port)             //소켓 연결
        } catch (e1: IOException) {
            // TODO Auto-generated catch block
            e1.printStackTrace()
        }

        checkUpdate.start()
        val et=findViewById(R.id.editText) as EditText
        val btn=findViewById(R.id.btn_chat) as Button
        var tv=findViewById(R.id.textView) as TextView

        btn.setOnClickListener {
            if (et.text.toString() != null || et.text.toString() != "") {
                val out=PrintWriter(networkWriter!!, true)
                val return_msg=et.text.toString()
               // tv.println(return_msg);
                out.println(return_msg)
            }
        }
    }


    private val checkUpdate=object : Thread() {
        override fun run() {
            try {
                var line: String
                Log.w("ChattingStart", "Start Thread")
                while (true) {
                    Log.w("Chatting is running", "chatting is running")
                    line=networkReader!!.readLine()
                    html=line
                    mHandler!!.post(showUpdate)
                }
            } catch (e: Exception) {
            }

        }
    }

    private val showUpdate=Runnable {
        Toast.makeText(this@client, "Coming word: " + html,
                Toast.LENGTH_SHORT).show()
    }

    @Throws(IOException::class)
    fun setSocket(ip: String, port: Int) {
        try {
            socket=Socket(ip, port)
            networkWriter=BufferedWriter(OutputStreamWriter(socket!!.getOutputStream()))
            networkReader=BufferedReader(InputStreamReader(socket!!.getInputStream()))
        } catch (e: IOException) {
            println(e)
            e.printStackTrace()
        }

    }
}