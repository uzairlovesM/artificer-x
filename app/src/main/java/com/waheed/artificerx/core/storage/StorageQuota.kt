package com.waheed.artificerx.core.storage
class StorageQuota(private val limitBytes:Long) {
    init{require(limitBytes>0)}
    private var used=0L
    @Synchronized fun reserve(bytes:Long):Boolean{if(bytes<0)return false;if(used>limitBytes-bytes)return false;used+=bytes;return true}
    @Synchronized fun release(bytes:Long){used=(used-bytes.coerceAtLeast(0)).coerceAtLeast(0)}
    @Synchronized fun usedBytes()=used
    @Synchronized fun remainingBytes()=limitBytes-used
}
