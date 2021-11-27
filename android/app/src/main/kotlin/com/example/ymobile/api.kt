package com.example.ymobile

import kotlinx.serialization.DeserializationStrategy
import android.util.Log
import androidx.annotation.Keep
import kotlinx.serialization.Serializable
import java.net.DatagramSocket
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.*
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.util.*
import kotlin.collections.HashMap


class api(socket:DatagramSocket,remoteAddr:InetSocketAddress) {
    var socket = socket
    var remoteAddr = remoteAddr
    fun queryIp() {
        Log.i("","querying ip")
        sendRequest(ApiId.C2S_QUERY_IP,null)
    }
    fun sendHeartbeat() {
        Log.d("", "sendHeartbeat")
        sendRequest(ApiId.C2S_HEARTBEAT, null)
    }

    fun sendRequest(msgId:Int,bodyMsg:Serializable?) {
        Log.i("","sendRequest")
        var bodyBytes:String = ""
        if (bodyMsg !=null) {
            bodyBytes  = Json.encodeToString(bodyMsg)
        }
        var req = ApiReq(msgId,bodyBytes?.toByteArray())
        Log.d("","json encoding")
        var msgBytes  = Json.encodeToString(ApiReq.serializer(),req)
        Log.d("","json encoded")
        Log.d("",msgBytes.toString())
        var bytes = ByteArray(msgBytes.length+1)
        bytes[0] = 1
        bytes = msgBytes.toByteArray().copyInto(bytes,1,0,msgBytes.length)
        Log.i("","sending app req msg ${remoteAddr}")
        socket.send(DatagramPacket(bytes,msgBytes.length+1,remoteAddr))
        Log.i("","sent app req msg")
    }

    fun decodeRes( resMsgStr:String) :Res {
        Log.d("",resMsgStr)
        return Json.decodeFromString<Res>(Res.serializer(),resMsgStr)
    }

}

@Keep
@Serializable
class reqQueryIp{

}

@Keep
@Serializable
data class Res(val id:Int,val err_reason:String,val body:String) {

}

@Keep
@Serializable
data class resQueryIp(val ip_net:String){

}

@Keep
@Serializable
data class ApiReq(val id:Int,val body:ByteArray?){

}


class ApiId(){
    companion object{
        var C2S_QUERY_IP=1
        var S2C_QUERY_IP=2
        var C2S_HEARTBEAT =3
        var S2C_HEARTBEAT =4
    }
}