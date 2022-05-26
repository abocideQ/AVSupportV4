package tv.av.support.core

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Build
import android.support.annotation.Nullable
import android.view.Surface
import tv.av.support.model.Support
import java.io.File

internal class MediaCodecCore {

    /**
     * @param
     * file: h264/h265
     * oneTap: decode a frame or full
     * mediaFormat: hevc/avc,1080,1920  -> @nullable
     * surface: render                  -> @nullable
     * filter: receive_frame_from_bytes -> @nullable
     */

    private lateinit var mMediaCodec: MediaCodec
    private var mInterrupt = false

    fun codecSupport(
        file: File,
        oneTap: Boolean,
        @Nullable mft: MediaFormat?,
        @Nullable surface: Surface?,
        @Nullable filter: FrameFilter?
    ): Support {
        mInterrupt = false
        var ret = false
        //1.config
        try {
            when {
                mft != null -> {
                    val mime = mft.getString(MediaFormat.KEY_MIME) ?: ""
                    val mediaFormat = MediaFormat.createVideoFormat(
                        mime,
                        mft.getInteger(MediaFormat.KEY_WIDTH),
                        mft.getInteger(MediaFormat.KEY_HEIGHT)
                    )
                    mMediaCodec =
                        MediaCodec.createDecoderByType(mime)
                    mMediaCodec.configure(mediaFormat, surface, null, 0)
                }
                file.name.contains("265") -> {
                    val mediaFormat = MediaFormat.createVideoFormat("video/hevc", 1280, 720)
                    mMediaCodec = MediaCodec.createDecoderByType("video/hevc")
                    mMediaCodec.configure(mediaFormat, surface, null, 0)
                }
                file.name.contains("264") -> {
                    val mediaFormat = MediaFormat.createVideoFormat("video/avc", 1280, 720)
                    mMediaCodec = MediaCodec.createDecoderByType("video/avc")
                    mMediaCodec.configure(mediaFormat, surface, null, 0)
                }
            }
            mMediaCodec.start()
        } catch (e: Exception) {
            e.printStackTrace()
            onRelease()
            return Support("MediaDecode config: ${e.stackTraceToString()}", ret)
        }
        //2.encoding
        try {
            val bytes = file.readBytes()
            val totalSize = bytes.size
            var startFrame = 0
            while (!mInterrupt) {
                Thread.sleep(1)//10毫秒
                if (totalSize <= 0) break
                else if (startFrame >= totalSize) break
                val nextFrame = filter?.frameReceive(bytes, startFrame, totalSize)
                    ?: frameReceive264(bytes, startFrame, totalSize)
                if (nextFrame == -1) {
                    break
                }
                //输入 如果DSP芯片的buffer全部被占用返回-1, timeOutUs 超时10 * 1000 = 10毫秒 = 0.01秒, -1则无限等待
                val inIndex: Int = mMediaCodec.dequeueInputBuffer((1 * 1000).toLong())
                if (inIndex >= 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val inBuffer = mMediaCodec.getInputBuffer(inIndex)
                        inBuffer?.clear()
                        inBuffer?.put(bytes, startFrame, nextFrame - startFrame)
                    } else {
                        val inBuffer = mMediaCodec.inputBuffers
                        val byteBuffer = inBuffer[inIndex]
                        byteBuffer.clear()
                        byteBuffer.put(bytes, startFrame, nextFrame - startFrame)
                    }
                    mMediaCodec.queueInputBuffer(inIndex, 0, nextFrame - startFrame, 0, 0)
                    startFrame = nextFrame
                } else {
                    continue
                }
                //输出
                while (!mInterrupt) {
                    val bufferInfo = MediaCodec.BufferInfo()
                    val outIndex =
                        mMediaCodec.dequeueOutputBuffer(bufferInfo, (1 * 1000).toLong())
                    when {
                        outIndex >= 0 -> {
                            ret = true
//                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                                val outBuffer = mediacodec.getOutputBuffer(outIndex)
//                            } else {
//                                val outBuffer = mediacodec.outputBuffers
//                            }
                            mMediaCodec.releaseOutputBuffer(outIndex, true)
                            if (oneTap) {
                                onRelease()
                                return Support("MediaDecode: success", ret)
                            } else continue
                        }
                        bufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM -> {
                            onRelease()
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
            onRelease()
            return Support("MediaDecode encoding: ${e.stackTraceToString()}", ret)
        }
        //3.release
        try {
            onRelease()
        } catch (e: Exception) {
            e.printStackTrace()
            return Support("MediaDecode release: ${e.stackTraceToString()}", ret)
        }
        return Support("MediaDecode: end, out of loop", ret)
    }

    private fun onRelease() {
        try {
            if (this::mMediaCodec.isInitialized && !mInterrupt) {
                mMediaCodec.stop()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            if (this::mMediaCodec.isInitialized && !mInterrupt) {
                mMediaCodec.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mInterrupt = true
    }

    private fun frameReceive264(bytes: ByteArray, start: Int, totalSize: Int): Int {
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