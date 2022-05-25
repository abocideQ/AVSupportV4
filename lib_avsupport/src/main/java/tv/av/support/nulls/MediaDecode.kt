package tv.av.support.nulls

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Build
import android.view.Surface
import tv.av.support.model.Support
import java.io.File

internal object MediaDecode {

    /**
     * @param
     * h26n: h264/h265
     * surface: render(nullable)
     * onTap: decode a frame or full
     */
    fun codecSupport(h26n: File, surface: Surface?, onTap: Boolean): Support {
        var ret = false
        //1.config
        val mediacodec: MediaCodec
        try {
            if (h26n.name.contains("265")) {
                val mediaFormat = MediaFormat.createVideoFormat("video/hevc", 1920, 1080)
                mediacodec = MediaCodec.createDecoderByType("video/hevc")
                mediacodec.configure(mediaFormat, surface, null, 0)
            } else {
                val mediaFormat = MediaFormat.createVideoFormat("video/avc", 1920, 1080)
                mediacodec = MediaCodec.createDecoderByType("video/avc")
                mediacodec.configure(mediaFormat, surface, null, 0)
            }
            mediacodec.start()
        } catch (e: Exception) {
            e.printStackTrace()
            return Support("MediaDecode config: ${e.cause}", ret)
        }
        //2.encoding
        try {
            val bytes = h26n.readBytes()
            val totalSize = bytes.size
            var startFrame = 0
            while (true) {
                Thread.sleep(1)//10毫秒
                if (totalSize <= 0) break
                else if (startFrame >= totalSize) break
                val nextFrame = frameReceive(bytes, startFrame, totalSize)
                if (nextFrame == -1) {
                    break
                }
                //输入 如果DSP芯片的buffer全部被占用 返回-1, 超时10 * 1000 = 10毫秒 = 0.01秒
                val inIndex: Int = mediacodec.dequeueInputBuffer((1 * 1000).toLong())
                if (inIndex >= 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val inBuffer = mediacodec.getInputBuffer(inIndex)
                        inBuffer?.clear()
                        inBuffer?.put(bytes, startFrame, nextFrame - startFrame)
                    } else {
                        val inBuffer = mediacodec.inputBuffers
                        val byteBuffer = inBuffer[inIndex]
                        byteBuffer.clear()
                        byteBuffer.put(bytes, startFrame, nextFrame - startFrame)
                    }
                    mediacodec.queueInputBuffer(inIndex, 0, nextFrame - startFrame, 0, 0)
                    startFrame = nextFrame
                } else {
                    continue
                }
                //输出
                while (true) {
                    val bufferInfo = MediaCodec.BufferInfo()
                    val outIndex =
                        mediacodec.dequeueOutputBuffer(bufferInfo, (1 * 1000).toLong())
                    when {
                        outIndex >= 0 -> {
                            ret = true
//                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                                val outBuffer = mediacodec.getOutputBuffer(outIndex)
//                            } else {
//                                val outBuffer = mediacodec.outputBuffers
//                            }
                            mediacodec.releaseOutputBuffer(outIndex, true)
                            if (onTap) return Support("MediaDecode: success", ret)
                            else continue
                        }
                        bufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM -> {
                            return Support("MediaDecode: decode end of stream", ret)
                        }
                        else -> {
                            //-1 INFO_TRY_AGAIN_LATER again
                            //-2 INFO_OUTPUT_FORMAT_CHANGED
                            //-3 INFO_OUTPUT_BUFFERS_CHANGED
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return Support("MediaDecode encoding: ${e.stackTraceToString()}", ret)
        }
        //3.release
        try {
            mediacodec.stop()
            mediacodec.release()
        } catch (e: Exception) {
            e.printStackTrace()
            return Support("MediaDecode release: ${e.message}", ret)
        }
        return Support("MediaDecode: end, out of loop", ret)
    }

    private fun frameReceive(bytes: ByteArray, start: Int, totalSize: Int): Int {
        for (i in start + 1 until totalSize - 4) {
            if (bytes[i].toString() == (0x00).toString() &&
                bytes[i + 1].toString() == (0x00).toString() &&
                bytes[i + 2].toString() == (0x00).toString() &&
                bytes[i + 3].toString() == (0x01).toString()
            ) {//第一个slice
                return i
            } else if (bytes[i].toString() == (0x00).toString() &&
                bytes[i + 1].toString() == (0x00).toString() &&
                bytes[i + 2].toString() == (0x01).toString()
            ) {
                continue
            }
        }
        return -1
    }


}