package com.hzz.netconnection.bean

import com.google.gson.annotations.SerializedName
import java.util.Date

data class AudioInfo(
    @SerializedName("link")val link: String,
    @SerializedName("md5")val md5: String,
    @SerializedName("Time") val time: Date
)