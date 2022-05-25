package tv.av.support

import android.media.MediaFormat
import android.view.Surface
import tv.av.support.core.FrameFilter
import tv.av.support.core.MediaCodecCore
import tv.av.support.core.MediaCodecListCore
import tv.av.support.model.Support
import java.io.File

class AVSupport {

    private val mMediaCodecListCore = MediaCodecListCore()
    private val mMediaCodecCore = MediaCodecCore()
    private val mLock = Object()

    fun supportByList(mime: String): Support {
        return mMediaCodecListCore.codecSupport(mime)
    }

    fun supportByCodec(
        file: File,
        oneTap: Boolean,
        mtf: MediaFormat?,
        surface: Surface?,
        filter: FrameFilter?
    ): Support {
        synchronized(mLock) {
            return mMediaCodecCore.codecSupport(file, oneTap, mtf, surface, filter)
        }
    }
}