package com.example.ymobile

import kotlinx.serialization.DeserializationStrategy
import android.util.Log
import kotlinx.serialization.Serializable
import java.net.DatagramSocket
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.net.DatagramPacket


class api(socket:DatagramSocket) {
    var socket = socket
    fun queryIp() {
        Log.i("","querying ip")
         sendRequest(ApiId.C2S_QUERY_IP,null)
    }

    fun sendRequest(msgId:Int,bodyMsg:Serializable?) {
        try{
            Log.i("","sendRequest")
            var bodyBytes:String = ""
            if (bodyMsg !=null) {
                bodyBytes  = Json.encodeToString(bodyMsg)
            }
            var req = ApiReq(msgId,bodyBytes?.toByteArray())
            Log.d("","json encoding")
            var msgBytes  = Json.encodeToString(req)
            Log.d("","json encoded")
            Log.d("",msgBytes.toString())
            var bytes = ByteArray(msgBytes.length+1)
            bytes[0] = 1
            bytes = msgBytes.toByteArray().copyInto(bytes,1,0,msgBytes.length)
            Log.i("","sending app req msg")
            socket.send(DatagramPacket(bytes,msgBytes.length+1))
            Log.i("","sent app req msg")
        }catch (e:Exception){
            Log.e("",e.toString())
        }
    }

    fun <T>decodeRes(resMsg: DeserializationStrategy<Res>, resMsgBytes:ByteArray) :Res {
        return Json.decodeFromString<Res>(resMsg,resMsgBytes.toString())
    }

    fun <T>decodeResBody(resBody:DeserializationStrategy<T>,resBodyBytes:ByteArray):T{
        return Json.decodeFromString<T>(resBody,resBodyBytes.toString())
    }
}

@Serializable
class reqQueryIp{

}
@Serializable
class Res{

}

@Serializable
data class ApiReq(val id:Int,val body:ByteArray?){
}


class ApiId(){
    companion object{
        var C2S_QUERY_IP=1
    }

}