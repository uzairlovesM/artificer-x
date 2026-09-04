package com.waheed.artificerx.util.serialization

object CanonicalText { fun normalize(value: String): String = value.replace("\r\n", "\n").replace("\r", "\n").trimEnd()+"\n" }
