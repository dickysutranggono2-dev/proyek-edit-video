package com.dik.videoedit.storage

import com.dik.videoedit.models.AspectOption
import com.dik.videoedit.models.Clip
import com.dik.videoedit.models.EffectType
import com.dik.videoedit.models.FilterType
import com.dik.videoedit.models.MusicTrack
import com.dik.videoedit.models.Project
import com.dik.videoedit.models.TextItem
import com.dik.videoedit.models.TextPosition
import com.dik.videoedit.models.TextSize
import com.dik.videoedit.models.TransitionType
import org.json.JSONArray
import org.json.JSONObject

/** Serialisasi proyek ke JSON memakai org.json bawaan Android (tanpa library tambahan). */
object ProjectJson {

    private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
        enumValues<T>().firstOrNull { it.name == this } ?: default

    fun toJson(p: Project): String = JSONObject().apply {
        put("version", 1)
        put("id", p.id)
        put("name", p.name)
        put("createdAt", p.createdAt)
        put("updatedAt", p.updatedAt)
        put("aspect", p.aspect.name)
        put("rotation", p.rotation)
        put("filter", p.filter.name)
        put("effect", p.effect.name)
        put("transition", p.transition.name)
        put("clips", JSONArray().apply {
            p.clips.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("uri", c.uri)
                    put("name", c.name)
                    put("sourceDurationMs", c.sourceDurationMs)
                    put("width", c.width)
                    put("height", c.height)
                    put("frameRate", c.frameRate)
                    put("trimStartMs", c.trimStartMs)
                    put("trimEndMs", c.trimEndMs)
                    put("speed", c.speed.toDouble())
                    put("volume", c.volume.toDouble())
                })
            }
        })
        p.music?.let { m ->
            put("music", JSONObject().apply {
                put("uri", m.uri)
                put("name", m.name)
                put("durationMs", m.durationMs)
                put("volume", m.volume.toDouble())
            })
        }
        put("texts", JSONArray().apply {
            p.texts.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id)
                    put("text", t.text)
                    put("position", t.position.name)
                    put("color", t.color)
                    put("size", t.size.name)
                })
            }
        })
    }.toString()

    fun fromJson(json: String): Project? = runCatching {
        val o = JSONObject(json)
        val clipsJson = o.getJSONArray("clips")
        val clips = (0 until clipsJson.length()).map { i ->
            val c = clipsJson.getJSONObject(i)
            Clip(
                id = c.getString("id"),
                uri = c.getString("uri"),
                name = c.optString("name", "video"),
                sourceDurationMs = c.getLong("sourceDurationMs"),
                width = c.getInt("width"),
                height = c.getInt("height"),
                frameRate = c.optInt("frameRate", 30),
                trimStartMs = c.getLong("trimStartMs"),
                trimEndMs = c.getLong("trimEndMs"),
                speed = c.optDouble("speed", 1.0).toFloat(),
                volume = c.optDouble("volume", 1.0).toFloat(),
            )
        }
        val textsJson = o.optJSONArray("texts") ?: JSONArray()
        val texts = (0 until textsJson.length()).map { i ->
            val t = textsJson.getJSONObject(i)
            TextItem(
                id = t.getString("id"),
                text = t.getString("text"),
                position = t.optString("position").toEnum(TextPosition.BOTTOM),
                color = t.optInt("color", 0xFFFFFFFF.toInt()),
                size = t.optString("size").toEnum(TextSize.MEDIUM),
            )
        }
        val music = o.optJSONObject("music")?.let { m ->
            MusicTrack(
                uri = m.getString("uri"),
                name = m.optString("name", "Musik"),
                durationMs = m.getLong("durationMs"),
                volume = m.optDouble("volume", 0.8).toFloat(),
            )
        }
        Project(
            id = o.getString("id"),
            name = o.optString("name", "Proyek"),
            createdAt = o.optLong("createdAt", 0L),
            updatedAt = o.optLong("updatedAt", 0L),
            clips = clips,
            aspect = o.optString("aspect").toEnum(AspectOption.ORIGINAL),
            rotation = o.optInt("rotation", 0),
            music = music,
            texts = texts,
            filter = o.optString("filter").toEnum(FilterType.NONE),
            effect = o.optString("effect").toEnum(EffectType.NONE),
            transition = o.optString("transition").toEnum(TransitionType.CUT),
        )
    }.getOrNull()
}
