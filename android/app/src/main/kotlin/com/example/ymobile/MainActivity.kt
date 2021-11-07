package com.example.ymobile

import android.content.Intent
import android.content.res.Configuration
import android.net.VpnService
import android.os.Bundle
import android.os.IBinder
import android.os.PersistableBundle
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Exception
import java.lang.reflect.Executable
import java.net.DatagramPacket
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.SocketException
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import kotlin.concurrent.thread

class MainActivity: FlutterActivity() {
    var yingTunnelSvc:YingTunnelService = YingTunnelService()
    var  REQ_CODE_QUERY_PERMISSION_TO_CREATE_TUNNEL_DEV= 0
    var hasPermissionToCreateTunnelDev = false
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
    }
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        // this.requestPermission()
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger,"yingying").setMethodCallHandler{
            call, result ->
            Log.i("","methodChannel call")
            when(call.method){
                "connectTunnel"-> {
                    stopService(Intent(this,YingTunnelService::class.java))
                    Log.i("","req permission for create tun device")
                    var requestTunnelPermissionIntent:Intent? = VpnService.prepare(applicationContext)
                    if (requestTunnelPermissionIntent != null)  {
                        startActivityForResult(requestTunnelPermissionIntent,REQ_CODE_QUERY_PERMISSION_TO_CREATE_TUNNEL_DEV)
                    }else{
                        startTunnelService()
                    }
                }
                else->{
                    Log.e("",String.format("undefined method {}",call.method))
                }
            }
        }
    }
    private fun startTunnelService(){
        Log.i("","starting tunnel service")
        (Intent(this,YingTunnelService::class.java)).also {intent -> startService(intent) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.i("",String.format("onActivity %d, %d",requestCode,resultCode))
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode){
            (this.REQ_CODE_QUERY_PERMISSION_TO_CREATE_TUNNEL_DEV)->{
                if (resultCode == RESULT_OK){
                    this.startTunnelService()
                }else{
                }
            }
            else -> {
                var msg = "Undefined activity request code"
                Log.e("",msg)
            }
        }
    }
}

class YingTunnelService:VpnService(){
    override fun onCreate() {
        super.onCreate()
        Log.i("","Tunnel service onCreate")
    }

    override fun onDestroy() {
        super.onDestroy()
    }


    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            Log.i("","onStartComamnd called")
            thread{
                try {
                    var tunnel:DatagramChannel = DatagramChannel.open()
                    var socket = tunnel.socket()
                    this.protect(socket)
                    // TODO
                    socket.connect(InetSocketAddress("107.155.15.21",10101))
                    Log.i("","builder")
                    var builder  = Builder()
                    val localTunnel = builder
                        .addAddress("10.8.1.20", 24)
                        .addRoute("0.0.0.0", 0)
                        .addDnsServer("8.8.8.8")
                        .establish()
                    if (localTunnel != null){
                        // Allocate the buffer for a single packet.
                        var packetBuffer     = ByteBuffer.allocate(1024*4)
                        var  packetReadFromIfce = FileInputStream(localTunnel.fileDescriptor)
                        var packetWriteToIfce = FileOutputStream(localTunnel.fileDescriptor)
                        while (true){
                            Log.i("","reading packet")
                            var length = packetReadFromIfce.read(packetBuffer.array())
                            if (length >0){
                                Log.i("read byte",packetBuffer.array().toString())
                                socket.send(DatagramPacket(packetBuffer.array(),0,length))
                            }else{
                                Log.i("read byte"," length is 0")
                            }
                            Thread.sleep(1000)
                            packetBuffer.clear()
                        }
                    }else{
                        Log.e("","create tun dev failed")
                    }
                }catch(e:SocketException){
                    Log.e("",e.toString())
                }finally {
                    // TODO
                    stopSelf()
                }
            }
            return VpnService.START_NOT_STICKY
    }
}

class TunnelRun():Thread(){
    public override fun run() {

    }

}