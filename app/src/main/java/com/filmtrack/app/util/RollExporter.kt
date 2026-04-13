package com.filmtrack.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.filmtrack.app.data.model.Frame
import com.filmtrack.app.data.model.Roll
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.Deflater

object RollExporter {

    private const val WEB_BASE_URL = "https://nwbort.github.io/filmtrackapp/web/"

    /**
     * Metadata-only URL (no thumbnails). Useful for quick sharing but the web
     * matcher cannot show reference images for alignment verification.
     */
    fun buildShareUrl(roll: Roll, frames: List<Frame>): String {
        val json = buildJsonRoot(roll, frames, thumbLoader = null)
        return "$WEB_BASE_URL?d=${compressAndEncode(json)}"
    }

    /**
     * Writes a JSON export file to the app's cache directory. Each frame
     * includes a small JPEG thumbnail (max 240px, quality 60) encoded as a
     * data URI so the web matcher can display reference images alongside the
     * user's developed scans.
     */
    fun buildExportFile(context: Context, roll: Roll, frames: List<Frame>): File {
        val json = buildJsonRoot(roll, frames) { uri -> loadThumb(context, uri) }
        val dir  = File(context.cacheDir, "exports").also { it.mkdirs() }
        val slug = roll.name.replace(Regex("[^A-Za-z0-9]+"), "_").take(40)
        return File(dir, "filmtrack-${slug}-${roll.id}.json").also { it.writeText(json) }
    }

    private fun buildJsonRoot(
        roll: Roll,
        frames: List<Frame>,
        thumbLoader: ((String) -> String?)?,
    ): String {
        val rollObj = JSONObject().apply {
            put("name", roll.name)
            if (roll.filmStock.isNotBlank()) put("filmStock", roll.filmStock)
            if (roll.camera.isNotBlank())    put("camera",    roll.camera)
            if (roll.iso.isNotBlank())       put("iso",       roll.iso)
            put("exposureCount", roll.exposureCount)
            put("dateStarted",   roll.dateStarted)
            roll.dateFinished?.let { put("dateFinished", it) }
        }

        val framesArr = JSONArray()
        for (frame in frames.sortedBy { it.frameNumber }) {
            framesArr.put(JSONObject().apply {
                put("n",  frame.frameNumber)
                frame.latitude?.let  { put("lat", it) }
                frame.longitude?.let { put("lng", it) }
                put("ts", frame.capturedAt)
                if (frame.note.isNotBlank()) put("note", frame.note)
                // Silently skip thumbnail on any error (missing URI, permission, OOM…)
                thumbLoader?.runCatching { invoke(frame.photoUri) }
                    ?.getOrNull()?.let { put("thumb", it) }
            })
        }

        return JSONObject().apply {
            put("v",      2)
            put("roll",   rollObj)
            put("frames", framesArr)
        }.toString()
    }

    private fun loadThumb(context: Context, photoUri: String): String? {
        val bm = context.contentResolver.openInputStream(Uri.parse(photoUri))
            ?.use { BitmapFactory.decodeStream(it) } ?: return null

        val scale  = 240f / maxOf(bm.width, bm.height)
        val scaled = if (scale < 1f)
            Bitmap.createScaledBitmap(
                bm, (bm.width * scale).toInt(), (bm.height * scale).toInt(), true
            ).also { bm.recycle() }
        else bm

        return ByteArrayOutputStream().use { baos ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 60, baos)
            scaled.recycle()
            "data:image/jpeg;base64," + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        }
    }

    private fun compressAndEncode(input: String): String {
        val bytes    = input.toByteArray(Charsets.UTF_8)
        val deflater = Deflater(Deflater.DEFAULT_COMPRESSION, /* nowrap = */ false)
        deflater.setInput(bytes)
        deflater.finish()
        val buf = ByteArray(4096)
        val out = ByteArrayOutputStream(bytes.size)
        while (!deflater.finished()) out.write(buf, 0, deflater.deflate(buf))
        deflater.end()
        return Base64.encodeToString(
            out.toByteArray(),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }
}
