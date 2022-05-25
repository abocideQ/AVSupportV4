package tv.av.support.core

import android.media.MediaCodecList
import android.os.Build
import tv.av.support.model.Support

internal class MediaCodecListCore {

    /**
     * @param
     * avc/hevc
     */
    fun codecSupport(mime: String): Support {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                for (info in MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos) {
                    if (info.name.contains("decoder") && info.name.contains(mime)) {
                        return Support("MediaList: success", true)
                    }
                }
            } else {
                val count = MediaCodecList.getCodecCount()
                for (i in 0 until count) {
                    val info = MediaCodecList.getCodecInfoAt(i)
                    if (info.name.contains("decoder") && info.name.contains(mime)) {
                        return Support("MediaList: success", true)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return Support("MediaList: ${e.printStackTrace()}", false)
        }
        return Support("MediaList: ", false)
    }
}