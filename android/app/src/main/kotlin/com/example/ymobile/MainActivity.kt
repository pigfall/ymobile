package com.example.ymobile

import android.content.Intent
import android.content.res.Configuration
import android.net.VpnService
import android.os.Bundle
import android.os.IBinder
import android.os.PersistableBundle
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.ViewModel
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Exception
import java.lang.reflect.Executable
import java.lang.reflect.Method
import java.net.*
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext

class MainActivity: FlutterActivity(){
    companion object {
        var binaryMsg :BinaryMessenger? =null;
    }
    var yingTunnelSvc:YingTunnelService = YingTunnelService()
    var  REQ_CODE_QUERY_PERMISSION_TO_CREATE_TUNNEL_DEV= 0
    var hasPermissionToCreateTunnelDev = false
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
    }
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        binaryMsg = flutterEngine.dartExecutor.binaryMessenger;
        // this.requestPermission()
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger,"ying").setMethodCallHandler{
                call, result ->
            Log.i("","methodChannel call")
            when(call.method){
                "connect"-> {
                    // result.success("");
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

class TzzViewMode:ViewModel(){
    suspend fun test(){

    }

}


class TunnelConn {
    var jobTunIfceRead : Job? =null;
    var jobConnRead: Job? =null;
    fun disconnect(){
        Log.i("","disconnect tunnelConn, job canel, thread interrupt")
        jobTunIfceRead!!.cancel();
        jobConnRead!!.cancel();
        Log.i("","waiting job stop")
        runBlocking {
            jobTunIfceRead!!.join()
            jobConnRead!!.join()
            Log.i("","job stop in runBlocking")
        }
        Log.i("","diconnected")
    }

    suspend fun tmp(tunnelSvc: YingTunnelService){
        coroutineScope {
            try{
                var tunnel:DatagramChannel = DatagramChannel.open()
                var socket = tunnel.socket()
                tunnelSvc.protect(socket)
                var remoteAddr = "107.155.15.21"
                Log.i("","connect to address ${remoteAddr}")
                socket.connect(InetSocketAddress(remoteAddr,10101))
                Log.i("","suc connect address ${remoteAddr}")
                jobTunIfceRead = launch {
                    Log.i("","start svc")
                    var builder  = tunnelSvc.Builder()
                    val localTunnel = builder
                        .addAddress("10.8.1.20", 24)
                        .addRoute("0.0.0.0", 0)
                        .addDnsServer("8.8.8.8")
                        .establish()
                    if (localTunnel != null){
                        // Allocate the buffer for a single packet.
                        var packetBuffer     = ByteBuffer.allocate(65535)
                        var bytesSendToServer = ByteArray(65536);
                        var  packetReadFromIfce = FileInputStream(localTunnel.fileDescriptor)
                        var packetWriteToIfce = FileOutputStream(localTunnel.fileDescriptor)
                        while (isActive){
                            Log.i("","reading packet")
                            var length = packetReadFromIfce.read(packetBuffer.array())
                            if (length >0){
                                Log.i("read byte",packetBuffer.array().toString())
                                bytesSendToServer[0] = 0
                                var  bytesIpPacket = packetBuffer.array()
                                bytesIpPacket.copyInto(bytesSendToServer,1,0,length)
                                socket.send(DatagramPacket(bytesSendToServer,0,length+1))
                            }else{
                                Log.i("read byte"," length is 0")
                            }
                            Thread.sleep(1000)
                            packetBuffer.clear()
                        }
                        Log.i("","while over")
                    }else{
                        Log.e("","create tun dev failed")
                    }
                }
                jobConnRead = launch{
                    var bufLen = 1024*10
                    var buf = ByteArray(bufLen);
                    var p = DatagramPacket(buf,0,bufLen)
                    Log.d("","recving connection packet")
                    socket.receive(p);
                    Log.d("","recved connection packet ${p}")
                }

            }catch(e:Exception){
                Log.i("","onDisconnect from android")
                YingTunnelService.onConnDisconnect(e.toString());
                jobTunIfceRead!!.cancel();
                jobConnRead!!.cancel();
            }
            jobTunIfceRead!!.join();
            jobConnRead!!.join();
        }
    }

    fun serveInNewThread(tunnelSvc:YingTunnelService){
        Log.i("","serveInNewThread")
        thread {
            runBlocking{
                // tmp(tunnelSvc);
                coroutineScope {
                    try{
                        // var tunnel:DatagramChannel = DatagramChannel.open()
                        var socket = DatagramSocket()
                        tunnelSvc.protect(socket)
                        var remoteAddr = "107.155.15.21"
                        Log.i("","connect to address ${remoteAddr}")
                        socket.connect(InetSocketAddress(remoteAddr,10101))
                        Log.i("","suc connect address ${remoteAddr}")
                        jobTunIfceRead = launch (Dispatchers.Default){
                            try{
                                Log.i("","start svc")
                                var builder  = tunnelSvc.Builder()
                                val localTunnel = builder
                                    .addAddress("10.8.1.20", 24)
                                    .addRoute("0.0.0.0", 0)
                                    .addDnsServer("8.8.8.8")
                                    .establish()
                                if (localTunnel != null){
                                    // Allocate the buffer for a single packet.
                                    var packetBuffer     = ByteBuffer.allocate(65535)
                                    var bytesSendToServer = ByteArray(65536);
                                    var  packetReadFromIfce = FileInputStream(localTunnel.fileDescriptor)
                                    var packetWriteToIfce = FileOutputStream(localTunnel.fileDescriptor)
                                    while (isActive){
                                        Log.i("","reading packet")
                                        var length = packetReadFromIfce.read(packetBuffer.array())
                                        if (length >0){
                                            Log.i("read byte",packetBuffer.array().toString())
                                            bytesSendToServer[0] = 0
                                            var  bytesIpPacket = packetBuffer.array()
                                            bytesIpPacket.copyInto(bytesSendToServer,1,0,length)
                                            Log.d("","sending packet to connect")
                                            try{
                                                socket.send(DatagramPacket(bytesSendToServer,0,length+1))
                                            }catch (e:Exception){
                                                Log.e("",e.toString())
                                            }
                                            Log.d("","Sended packet to connect")
                                        }else{
                                            Log.i("read byte"," length is 0")
                                        }
                                        Thread.sleep(1000)
                                        packetBuffer.clear()
                                    }
                                    Log.i("","while over")
                                }else{
                                    Log.e("","create tun dev failed")
                                }
                            }finally {
                                Log.d("","jobTunIfceRead over");
                            }
                        }
                        jobConnRead = launch(Dispatchers.Default){
                            try{
                                var bufLen = 1024*10
                                var buf = ByteArray(bufLen);
                                var p = DatagramPacket(buf,0,bufLen)
                                Log.d("","recving connection packet")
                                socket.receive(p);
                                Log.d("","recved connection packet ${p}")
                            }finally {
                                Log.d("","jobConnRead over")
                            }
                        }
                    }catch(e:Exception){
                        Log.i("","onDisconnect from android")
                        YingTunnelService.onConnDisconnect(e.toString());
                        jobTunIfceRead!!.cancel();
                        jobConnRead!!.cancel();
                    }
                    Log.d("","waiting jobs ")
                    jobTunIfceRead!!.join();
                    jobConnRead!!.join();
                    Log.d("","jobs over ")
                }
                Log.d(""," serveInNewThread over")
            }
        }
    }
}


class YingTunnelService:VpnService(){
    companion object{
        fun onConnDisconnect(errInfo:String){
            MethodChannel(MainActivity.binaryMsg,"ying").invokeMethod("onConnectFailed",errInfo,object:MethodChannel.Result{
                override fun success(result: Any?) {
                    Log.d("Android", "result = $result")
                }
                override fun error(errorCode: String?, errorMessage: String?, errorDetails: Any?) {
                    Log.d("Android", "$errorCode, $errorMessage, $errorDetails")
                }
                override fun notImplemented() {
                    Log.d("Android", "notImplemented")
                }
            })
        }    var curConnection: TunnelConn? =null;
    }


    override fun onCreate() {
        super.onCreate()
        Log.i("","Tunnel service onCreate")
    }

    fun startConnection(){
        curConnection?.disconnect()
        Log.i("","new tunnel conn")
        curConnection = TunnelConn();
        curConnection?.serveInNewThread(this);
    }

    override fun onDestroy() {
        super.onDestroy()
        curConnection?.disconnect();
    }


    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try{
            startConnection();
        }catch(e:Exception){
            onConnDisconnect(e.toString())
        }
        return VpnService.START_NOT_STICKY
    }
}
