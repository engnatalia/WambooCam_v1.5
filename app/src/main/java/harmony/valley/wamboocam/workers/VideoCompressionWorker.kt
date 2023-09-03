/**
 * Copyright (c) 2023 Natalia Molinero Mingorance
 * All rights reserved.
 */

package harmony.valley.wamboocam.workers

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.Worker
import androidx.work.WorkerParameters
import harmony.valley.wamboocam.services.VideoCompressionService


class VideoCompressionWorker(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val videoUrl = Uri.parse(inputData.getString(ForegroundWorker.VideoURI))
        val selectedMethod = Uri.parse(inputData.getString(ForegroundWorker.SELECTION_TYPE))
        val selectedFormat = Uri.parse(inputData.getString(ForegroundWorker.SELECTION_FORMAT))
        val selectedResolution = Uri.parse(inputData.getString(ForegroundWorker.VIDEO_RESOLUTION))
        val selectedCodec = Uri.parse(inputData.getString(ForegroundWorker.VIDEO_CODEC))
        val audio = Uri.parse(inputData.getString(ForegroundWorker.VIDEO_AUDIO))
        val selectedSpeed = Uri.parse(inputData.getString(ForegroundWorker.COMPRESS_SPEED))
        val serviceIntent = Intent(context, VideoCompressionService::class.java)
        serviceIntent.putExtra(ForegroundWorker.VideoURI, videoUrl.toString())
        serviceIntent.putExtra(ForegroundWorker.SELECTION_TYPE, selectedMethod.toString())
        serviceIntent.putExtra(ForegroundWorker.SELECTION_FORMAT, selectedFormat.toString())
        serviceIntent.putExtra(ForegroundWorker.VIDEO_RESOLUTION, selectedResolution.toString())
        serviceIntent.putExtra(ForegroundWorker.VIDEO_CODEC, selectedCodec.toString())
        serviceIntent.putExtra(ForegroundWorker.VIDEO_AUDIO, audio.toString())
        serviceIntent.putExtra(ForegroundWorker.COMPRESS_SPEED, selectedSpeed.toString())
        context.startForegroundService(serviceIntent)
        return Result.success()
    }

}