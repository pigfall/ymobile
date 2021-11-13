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
import java.net.DatagramPacket
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.SocketException
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
    var job : Job? =null;
    fun disconnect(){
        Log.i("","disconnect tunnelConn, job canel, thread interrupt")
        job!!.cancel();
        Log.i("","waiting job stop")
        runBlocking {
            job!!.join()
            Log.i("","job stop in runBlocking")
        }
        Log.i("","diconnected")
    }


    fun serveInNewThread(tunnelSvc:YingTunnelService){
         thread {
             runBlocking{
                job = launch {
                    Log.i("","start svc")
                    try {
                        // TODO
                        var tunnel:DatagramChannel = DatagramChannel.open()
                        var socket = tunnel.socket()
                        tunnelSvc.protect(socket)
                        socket.connect(InetSocketAddress("107.155.15.21",10101))
                        Log.i("","builder")
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
                    }catch(e:SocketException){
                        Log.e("",e.toString())
                    }finally {
                    }
                };
                job!!.join();
            }
        }
    }
}


class YingTunnelService:VpnService(){
    companion object{
        var curConnection: TunnelConn? =null;
    }
    override fun onCreate() {
        super.onCreate()
        Log.i("","Tunnel service onCreate")
    }

    override fun stopService(name: Intent?): Boolean {
        return super.stopService(name)
    }

    fun startConnection(){
        curConnection?.disconnect()
        Log.i("","new tunnel conn")
        curConnection = TunnelConn();
        curConnection?.serveInNewThread(this);
    }

    override fun onDestroy() {
        super.onDestroy()
    }


    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            try{
                MethodChannel(MainActivity.binaryMsg,"ying").invokeMethod("onConnectFailed","",object:MethodChannel.Result{
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
                Log.i("","call method channel")
                // TODO
                // startConnection();
            }catch(e:Exception){
                // MethodChannel(MainActivity.binaryMsg,"onConnectFailed")
                Log.i("",e.toString());
            }
            return VpnService.START_NOT_STICKY
    }
}

class TunnelRun():Thread(){
    public override fun run() {

    }

}