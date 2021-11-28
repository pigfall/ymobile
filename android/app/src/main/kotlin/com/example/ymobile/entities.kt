package com.koitown.ying

import androidx.annotation.Keep
import kotlinx.serialization.Serializable


@Keep
@Serializable
data class EntityClientCfg(val id:String,val serverHost:String,val serverPort:String){

}
