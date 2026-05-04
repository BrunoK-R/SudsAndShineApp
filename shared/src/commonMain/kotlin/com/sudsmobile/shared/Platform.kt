package com.sudsmobile.shared

expect fun platformName(): String

object SharedInfo {
    fun scaffoldDescription(): String = "KMP scaffold on ${platformName()}"
}
