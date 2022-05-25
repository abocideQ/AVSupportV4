package tv.av.support.core

interface FrameFilter {
    fun frameReceive(bytes: ByteArray, start: Int, total: Int): Int
}