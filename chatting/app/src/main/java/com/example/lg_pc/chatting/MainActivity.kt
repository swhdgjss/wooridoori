import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import java.net.URL
import java.net.URLConnection
//import org.apache.http.util.ByteArrayBuffer
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
import com.example.lg_pc.chatting.R
import kotlinx.android.synthetic.main.activity_main.view.*


class MainActivity : Activity() {
    private var html=""
    private var mHandler: Handler?=null
    private var socket: Socket?=null
    private var networkReader: BufferedReader?=null
    private var networkWriter: BufferedWriter?=null
    private val ip="223.62.22.45" // IP
    private val port=8080 // PORT번호


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

    private val showUpdate=Runnable { Toast.makeText(this@MainActivity, "Coming word: " + html, Toast.LENGTH_SHORT).show() }

    override fun onStop() {
        super.onStop()
        try {
            socket!!.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mHandler=Handler()

        try {
            setSocket(ip, port)
        } catch (e1: IOException) {
            e1.printStackTrace()
        }

        checkUpdate.start()

        val et=findViewById<EditText>(R.id.editText) as EditText
        val btn=findViewById<Button>(R.id.btn_chat) as Button
        val tv=findViewById<TextView>(R.id.textView) as TextView

        btn.setOnClickListener {
            if (et.text.toString() != null || et.text.toString() != "") {
                val out=PrintWriter(networkWriter!!, true)
                val return_msg=et.text.toString()
                out.println(return_msg)
            }
        }

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
