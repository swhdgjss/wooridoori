package com.example.l.myapplication

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
    //Button을 클릭했을 때 호출되는 callback 메소드

    fun mOnClick(v: View) {
        val i: Intent

        when (v.id) {
            R.id.btn_server //서버 화면
            -> {
                i = Intent(this, Server::class.java)
                startActivity(i)
            }
            R.id.btn_client //클라이언트 화면
            -> {
                i = Intent(this, Client::class.java)
                startActivity(i)
            }
        }
    }
}
