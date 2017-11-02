package com.example.lg_pc.chatting

/**
 * Created by LG_PC on 2017-11-02.
 */

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

class TCPServer: Runnable {
    override fun run() {
        try {
            println("S: Connecting...")
            val serverSocket=ServerSocket(ServerPort)
            while (true) {
                val client=serverSocket.accept()
                println("S: Receiving...")
                try {
                    val `in`=BufferedReader(InputStreamReader(client.getInputStream()))
                    val str=`in`.readLine()
                    println("S: Received: '$str'")
                    val out=PrintWriter(BufferedWriter(OutputStreamWriter(client.getOutputStream())), true)
                    out.println("Server Received " + str)
                } catch (e: Exception) {
                    println("S: Error")
                    e.printStackTrace()
                } finally {
                    client.close()
                    println("S: Done.")
                }
            }
        } catch (e: Exception) {
            println("S: Error")
            e.printStackTrace()
        }
    }

    companion object {
        val ServerPort=8080
        val ServerIP="223.62.22.45"

        @JvmStatic
        fun main(args: Array<String>) {
            val desktopServerThread=Thread(TCPServer())
            desktopServerThread.start()
        }
    }


}
