package com.example.ymobile

import kotlinx.serialization.Serializable


class api {
    api(socket)
    queryIp() {
        sendRequets(ApiId.C2S_QUERY_IP)
    }

    sendRequest(msgId,bodyMsg?) {
        var bodyBytes:Bytes
        if bodyMsg != nil {
            bodyBytes  = json.encodeToString(bodyMsg)
        }
        var req = ApiReq(msgId,bodyBytes)
        var msgBytes  = json.marshal(req)
        socket.write(msgBytes)
    }

    fun Res decodeRes(resMsg) {
        return  json.decodeFromString<Res>(resMsg)
    }

    fun decodeResBody<T>(resBodyBytes):T{
        return json.decodeFromString<T>(resBodyBytes)
    }
}

@Serializable
class reqQueryIp{

}
@Serializable
class Res{

}

@Serializable
class ApiReq{
    ApiReq(msgId,msgBodyBytes)
}

enum class ApiId(id:Int){
    C2S_QUERY_IP = 1

}