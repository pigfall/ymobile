package com.example.ymobile

import android.content.Intent
import android.content.res.Configuration
import android.net.VpnService
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
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
                "disconnect"->{
                    Log.i("","discconect from Android");
                    stopService(Intent(this,YingTunnelService::class.java))
                    Log.i("","discconected from Android");
                    YingTunnelService.curConnection?.disconnect()
                    result.success("");
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
                    // user reject privilege to tunnel
                    YingTunnelService.onConnDisconnect("User reject privilege to app")
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
    var connSocket:DatagramSocket?=null;
    var tunIfce:ParcelFileDescriptor?=null;
    fun disconnect(){
        cancel("")

    }


    fun cancel(reason:String){
        Log.i("","disconnect tunnelConn, job canel, thread interrupt")
        connSocket?.close()
        jobTunIfceRead!!.cancel();
        jobConnRead!!.cancel();
        Log.i("","waiting job stop")
        runBlocking {
            jobTunIfceRead!!.join()
            jobConnRead!!.join()
            Log.i("","job stop in runBlocking")
        }
        YingTunnelService.onConnDisconnect(reason);
        Log.i("","diconnected")
    }

    fun serveInNewThread(tunnelSvc:YingTunnelService){
        var tunConn = this
        Log.i("","serveInNewThread")
        thread {
            runBlocking{
                // tmp(tunnelSvc);
                 coroutineScope {
                    try{
                        // var tunnel:DatagramChannel = DatagramChannel.open()
                        var socket = DatagramSocket()
                        connSocket = socket
                        tunnelSvc.protect(socket)
                        var remoteAddr = "107.155.15.21"
                        Log.i("","connect to address ${remoteAddr}")
                        socket.connect(InetSocketAddress(remoteAddr,10101))
                        // >
                        Log.i("","suc connect address ${remoteAddr}")
                        var clientTunnelIpNet:String = ""
                        var apiIns = api(socket)
                        jobTunIfceRead = launch (Dispatchers.Default){
                            while(isActive){
                                delay(1000L)
                                if (clientTunnelIpNet.length > 0) {
                                    break
                                }
                            }
                            if (isActive){
                                try{
                                    Log.i("","start svc")
                                    var builder  = tunnelSvc.Builder()
                                    val localTunnel = builder
                                        .addAddress(clientTunnelIpNet, 24)
                                        .addRoute("0.0.0.0", 0)
                                        .addDnsServer("8.8.8.8")
                                        .establish()
                                    if (localTunnel != null){
                                        tunIfce = localTunnel
                                        launch (Dispatchers.Main){
                                            YingTunnelService.onConnected("")
                                        }
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
                                            packetBuffer.clear()
                                            Thread.sleep(1000)
                                        }
                                        Log.i("","while over")
                                    }else{
                                        var msg = "create tun dev failed"
                                        Log.e("",msg)
                                        cancel()
                                        YingTunnelService.onConnDisconnect(msg);
                                    }
                                }finally {
                                    Log.d("","jobTunIfceRead over");
                                    this.cancel()
                                }
                            }
                        }
                        launch(Dispatchers.Default){
                            while(isActive){
                                Log.i("","querying ip")
                                apiIns.queryIp()
                                delay(1000L)
                                if (clientTunnelIpNet.length >0){
                                    break
                                }
                            }
                        }
                        jobConnRead = launch(Dispatchers.Default){
                            var e :Exception = Exception("")
                            try{
                                var bufLen = 1024*10
                                var buf = ByteArray(bufLen);
                                var p = DatagramPacket(buf,0,bufLen)
                                Log.d("","recving connection packet")
                                while(isActive){
                                    socket.receive(p);
                                    Log.d("","rcvd connect packet")
                                    if (p.data[0] ==(0).toByte() && tunIfce != null){
                                        Log.d("","write to tunifce ${p.data}")
                                        var packetWriteToIfce = FileOutputStream(tunIfce?.fileDescriptor)
                                        packetWriteToIfce.write(p.data,1,p.length-1)
                                    }else{
                                        Log.d("","recv app msg")
                                        // TODO handle custom proto
                                    }
                                }
                            }catch(ex:Exception){
                                Log.e("","jobConnRead Exception : ${ex.toString()}")
                                e=ex
                            }
                            finally {
                                Log.d("","jobConnRead over")
                                if (e == null){
                                    e =Exception("")
                                }
                                tunConn.cancel("jobConnRead over ${e.toString()}")
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
                    try{
                        tunIfce?.close()
                    }catch(e:Exception){
                        Log.e("",e.toString())
                    }
                    Log.d("","jobs over ")
                }
                Log.d(""," serveInNewThread over")
            }
        }
    }
}


class YingTunnelService:VpnService(){
    companion object{
        var instance:YingTunnelService? =null;
        fun onConnected(errInfo:String) {
            try{
                Log.i("","onConnected from Android");
                MethodChannel(MainActivity.binaryMsg,"ying").invokeMethod("onConnected",errInfo,object:MethodChannel.Result{
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
            }catch (e:Exception){
                Log.i("","${e.toString()}")
            }finally {
                Log.i("","onConnected over from Android")
            }
        }

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
        instance = this
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
        Log.i("","Service onDestroy")
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
