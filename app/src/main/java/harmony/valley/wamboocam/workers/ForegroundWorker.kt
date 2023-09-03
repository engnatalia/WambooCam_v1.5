/**
 * Copyright (c) 2023 Natalia Molinero Mingorance
 * All rights reserved.
 */

package harmony.valley.wamboocam.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ForegroundWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {


    override suspend fun doWork(): Result {
        return Result.success()
    }


    companion object {

        const val VIDEO_CODEC = "videoCodec"
        const val VIDEO_AUDIO = "audio"
        const val COMPRESS_SPEED = "compressSpeed"
        const val VIDEO_RESOLUTION = "videoResolution"
        const val VideoURI = "videoURI"
        const val SELECTION_TYPE = "type"
        const val SELECTION_FORMAT = "format"
    }
}