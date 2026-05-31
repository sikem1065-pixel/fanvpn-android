package com.github.kr328.clash.design

object FlagUtil {
    private val codes = listOf(
        "美国" to "US", "日本" to "JP",
        "法国" to "FR", "法國" to "FR",
        "台湾" to "TW", "台灣" to "TW",
        "香港" to "HK",
        "德国" to "DE", "德國" to "DE",
        "新加坡" to "SG", "狮城" to "SG",
        "英国" to "GB", "英國" to "GB",
        "韩国" to "KR", "韓國" to "KR",
        "俄罗斯" to "RU", "俄國" to "RU",
        "加拿大" to "CA",
        "澳大利亚" to "AU", "澳洲" to "AU",
        "荷兰" to "NL", "荷蘭" to "NL",
        "印度" to "IN", "越南" to "VN",
        "泰国" to "TH", "泰國" to "TH",
        "马来" to "MY", "馬來" to "MY",
        "土耳其" to "TR", "巴西" to "BR"
    )

    @JvmStatic
    fun flagOf(name: String?): String {
        if (name.isNullOrEmpty()) return ""
        for ((k, v) in codes) if (name.contains(k)) return v
        return "··"
    }
}