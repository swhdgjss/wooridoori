package com.example.l.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    //Button을 클릭했을 때 호출되는 callback 메소드

    public void mOnClick(View v){
        Intent i;

        switch(v.getId()){
            case R.id.btn_server: //서버 화면
                i= new Intent(this, Server.class);
                startActivity(i);
                break;
            case R.id.btn_client: //클라이언트 화면
                i= new Intent(this, Client.class);
                startActivity(i);
                break;
        }
    }
}
