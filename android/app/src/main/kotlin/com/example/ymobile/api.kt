package com.example.ymobile

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import java.net.DatagramSocket
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.net.DatagramPacket


class api(socket:DatagramSocket) {
    var socket = socket
    fun queryIp() {
         sendRequest(ApiId.C2S_QUERY_IP,null)
    }

    fun sendRequest(msgId:ApiId,bodyMsg:Serializable?) {
        var bodyBytes:String = ""
        if (bodyMsg !=null) {
            bodyBytes  = Json.encodeToString(bodyMsg)
        }
        var req = ApiReq(msgId,bodyBytes?.toByteArray())
        var msgBytes  = Json.encodeToString(req)
        var bytes = ByteArray(msgBytes.length+1)
        bytes = msgBytes.toByteArray().copyInto(bytes,1,0,msgBytes.length)
        socket.send(DatagramPacket(bytes,msgBytes.length+1))
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
class ApiReq(msgId:ApiId,msgBodyBytes:ByteArray?){
    var msgId =msgId
    var msgBodyBytes=msgBodyBytes
}

enum class ApiId{
    C2S_QUERY_IP,

}