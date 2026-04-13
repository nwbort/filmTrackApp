package com.filmtrack.app.util

import android.util.Base64
import com.filmtrack.app.data.model.Frame
import com.filmtrack.app.data.model.Roll
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater

object RollExporter {

    private const val WEB_BASE_URL = "https://nwbort.github.io/filmtrackapp/web/"

    /** Returns a URL that encodes all roll + frame metadata for the web matcher. */
    fun buildShareUrl(roll: Roll, frames: List<Frame>): String {
        val json    = buildJson(roll, frames)
        val encoded = compressAndEncode(json)
        return "$WEB_BASE_URL?d=$encoded"
    }

    private fun buildJson(roll: Roll, frames: List<Frame>): String {
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
            val obj = JSONObject().apply {
                put("n",  frame.frameNumber)
                frame.latitude?.let  { put("lat", it) }
                frame.longitude?.let { put("lng", it) }
                put("ts", frame.capturedAt)
                if (frame.note.isNotBlank()) put("note", frame.note)
            }
            framesArr.put(obj)
        }

        return JSONObject().apply {
            put("v",      1)
            put("roll",   rollObj)
            put("frames", framesArr)
        }.toString()
    }

    /**
     * Compresses UTF-8 JSON with zlib DEFLATE (nowrap=false so the browser's
     * DecompressionStream('deflate') can decompress it without extra libraries),
     * then encodes the result as URL-safe base64 with no padding.
     */
    private fun compressAndEncode(input: String): String {
        val bytes    = input.toByteArray(Charsets.UTF_8)
        val deflater = Deflater(Deflater.DEFAULT_COMPRESSION, /* nowrap = */ false)
        deflater.setInput(bytes)
        deflater.finish()

        val buf = ByteArray(4096)
        val out = ByteArrayOutputStream(bytes.size)
        while (!deflater.finished()) {
            out.write(buf, 0, deflater.deflate(buf))
        }
        deflater.end()

        return Base64.encodeToString(
            out.toByteArray(),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }
}
