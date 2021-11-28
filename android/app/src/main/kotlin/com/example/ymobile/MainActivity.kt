package com.example.ymobile

import android.content.Intent
import android.net.VpnService
import android.os.*
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Exception
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.*
import java.nio.ByteBuffer
import kotlin.concurrent.thread
import android.os.Looper
import java.time.Duration
import java.time.LocalDateTime


class MainActivity: FlutterActivity(){
    companion object {
        var binaryMsg :BinaryMessenger? =null;
        var clientCfgToUse :EntityClientCfg?=null;
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
                    Log.i("",call.arguments())
                    var clientCfg = Json.decodeFromString<EntityClientCfg>(EntityClientCfg.serializer(),call.arguments())
                    MainActivity.clientCfgToUse= clientCfg
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
        Log.i(" startTunnelService","starting tunnel service")
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



class TunnelSvc(vpnSvc:YingTunnelService){
    var th :Thread? = null
    var tunIfce:ParcelFileDescriptor?=null;
    var vpnSvc = vpnSvc
    var socket :DatagramSocket? = null
    var isOver:Boolean = false
    var jobHeartbeat:Job? = null
    var errReason :String=""
    fun setFirstReason(errReason:String){
        if (this.errReason.length == 0){
            this.errReason = errReason
        }
    }
    fun disconnect(){
        this.cancelSvc()
        this.th?.join()
    }

    fun onSvcOver(){
        Log.i(""," onSvcOver")
        YingTunnelService.onConnDisconnect(errReason.toString())
    }

    fun cancelSvc(){
        this.isOver = true
        this.socket?.close()
        this.tunIfce?.close()
        this.jobHeartbeat?.cancel()
    }
    fun connect(clientCfg: EntityClientCfg?) {
        var ins = this
        val mainHandler = Handler(Looper.getMainLooper())
        var stop = false
        try{
            var remoteAddr = InetSocketAddress(clientCfg!!.serverHost,clientCfg!!.serverPort.toInt())
        }catch (e:Exception){
            onSvcOver()
            stop = true
        }
        if (stop) {
            return
        }

        this.th = thread {
            var ins = this
            runBlocking {
                coroutineScope {
                    try{
                        var remoteAddr = InetSocketAddress(clientCfg!!.serverHost,clientCfg!!.serverPort.toInt())
                        var socket = DatagramSocket()
                        Log.d("","tmp")
                        ins.socket = socket
                        var heartbeat =LocalDateTime.now()
                        var clientTunnelIpNet = ""
                        // < readySocket
                        vpnSvc.protect(socket)
                        // socket?.connect(remoteAddr)
                        // >
                        var apiIns = api(socket,remoteAddr)
                        launch(Dispatchers.Default){
                            while(!isOver){
                                Log.i("","querying ip")
                                try{
                                    apiIns.queryIp()
                                    delay(1000L)
                                    if (clientTunnelIpNet.length >0){
                                        break
                                    }
                                }catch (e:Exception){
                                    setFirstReason(e.toString())
                                    Log.e("",e.toString())
                                    cancelSvc()
                                }
                            }
                        }
                        ins.jobHeartbeat = launch (Dispatchers.Default) {
                            try{
                                while (!isOver) {
                                    var now = LocalDateTime.now()
                                    var period = Duration.between(now, heartbeat)
                                    if (!period.abs().minus(Duration.ofSeconds(20)).isNegative) {
                                        Log.i("","heartbeat timeout")
                                        cancelSvc()
                                        break
                                    }
                                    delay(6000)
                                    apiIns.sendHeartbeat()
                                }
                            }catch (e:Exception){
                                setFirstReason(e.toString())
                                Log.e("",e.toString())
                            }finally {
                                cancelSvc()
                            }
                        }


                        var jobConRead = launch (Dispatchers.Default){
                            try{
                                    var bufLen = 1024*10
                                    var buf = ByteArray(bufLen);
                                    var p = DatagramPacket(buf,0,bufLen)
                                    Log.d("","recving connection packet")
                                    while(!isOver){
                                        Log.d("","recving connection packet")
                                        socket.receive(p);
                                        Log.d("","rcvd connect packet")
                                        if (p.data[0] ==(0).toByte() && tunIfce != null){
                                            Log.d("","write to tunifce ${p.data}")
                                            var packetWriteToIfce = FileOutputStream(tunIfce?.fileDescriptor)
                                            packetWriteToIfce.write(p.data,1,p.length-1)
                                        }else{
                                            var resBytes = p.data.sliceArray(1..(p.length-1))
                                            Log.d("","recv app msg ${p.data.sliceArray(1..(p.length-1)).decodeToString()}")
                                            var msg = apiIns.decodeRes(resBytes.decodeToString())
                                            when(msg.id){
                                                (ApiId.S2C_QUERY_IP)->{
                                                    Log.i("","get clientTunnel ip ")
                                                    var clientTunnelIp = Json.decodeFromString<resQueryIp>(resQueryIp.serializer(),msg.body)
                                                    Log.i("","get clientTunnel ip $clientTunnelIp")
                                                    clientTunnelIpNet = clientTunnelIp.ip_net
                                                }
                                                (ApiId.S2C_HEARTBEAT)->{
                                                    Log.i("","get hearbeat response")
                                                    heartbeat = LocalDateTime.now()
                                                }
                                            }
                                            // TODO handle custom proto
                                        }
                                    }
                            }catch (e:Exception){
                                setFirstReason(e.toString())
                                Log.e("",e.toString())
                            }
                            finally {
                                cancelSvc()
                            }
                        }
                        var jobTunRead = launch (Dispatchers.Default){
                            try{
                                Log.d("","wating clientTunnel ip")
                                while(!isOver){
                                    delay(1000)
                                    Log.d("","dealy over")
                                    if (clientTunnelIpNet.length > 0) {
                                        Log.i("","get clientTunnelIpNet")
                                        break
                                    }
                                }
                                if (!isOver){
                                    mainHandler.post {
                                        YingTunnelService.onConnected("")
                                    }
                                    try{
                                        Log.i("","start create tun interface")
                                        var builder  =vpnSvc.Builder()

                                        val localTunnel = builder
                                            .addAddress(clientTunnelIpNet.slice(0..(clientTunnelIpNet.indexOfFirst {c ->  c=='/'}-1)), 24)
                                            .addRoute("0.0.0.0", 0)
                                            .addDnsServer("8.8.8.8").setBlocking(true)
                                            .establish()
                                        if (localTunnel != null){
                                            tunIfce = localTunnel
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
                                                    socket.send(DatagramPacket(bytesSendToServer,0,length+1,remoteAddr))
                                                    Log.d("","Sended packet to connect")
                                                }else{
                                                    Log.i("read byte"," length is 0")
                                                }
                                                packetBuffer.clear()
                                                // Thread.sleep(1000)
                                            }
                                            Log.i("","while over")
                                        }else{
                                            var msg = "create tun dev failed"
                                            Log.e("",msg)
                                        }
                                    }finally {
                                        Log.d("","jobTunIfceRead over");
                                        cancelSvc()
                                    }
                                }
                            }catch (e:Exception){
                                setFirstReason(e.toString())
                                Log.e("",e.toString())
                            }
                            finally {
                                cancelSvc()
                            }
                        }
                        Log.i("","wating jobs over")
                        jobConRead.join()
                        jobTunRead.join()
                        Log.i("","jobs over")
                    }catch (e:Exception){
                        setFirstReason(e.toString())
                        Log.e("","mainBlock exception ${e.toString()}")
                    }finally {
                        Log.e("","mainBlock quit")
                        cancelSvc()
                    }
                }
                mainHandler.post {
                    onSvcOver()
                }
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
            Log.i(""," onConnDisconnect")
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
        }
        var curConnection: TunnelSvc? =null;
    }


    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i("","Tunnel service onCreate")
    }

    fun startConnection(){
        curConnection?.disconnect()
        Log.i("","new tunnel conn")
        curConnection = TunnelSvc(this);
        curConnection?.connect(MainActivity.clientCfgToUse);
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
