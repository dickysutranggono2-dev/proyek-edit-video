package com.dik.videoedit.storage

import android.content.Context
import com.dik.videoedit.models.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** Proyek disimpan sebagai satu file JSON per proyek di penyimpanan internal aplikasi. */
class ProjectRepository(context: Context) {
    private val dir = File(context.filesDir, "projects").apply { mkdirs() }
    private val mutex = Mutex()

    suspend fun list(): List<Project> = withContext(Dispatchers.IO) {
        dir.listFiles { f -> f.extension == "json" }
            ?.mapNotNull { runCatching { ProjectJson.fromJson(it.readText()) }.getOrNull() }
            ?.sortedByDescending { it.updatedAt }
            ?: emptyList()
    }

    suspend fun get(id: String): Project? = withContext(Dispatchers.IO) {
        val f = File(dir, "$id.json")
        if (!f.exists()) null else runCatching { ProjectJson.fromJson(f.readText()) }.getOrNull()
    }

    suspend fun save(project: Project) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                // Tulis ke file sementara lalu rename agar file tidak rusak bila proses terhenti.
                val tmp = File(dir, "${project.id}.tmp")
                tmp.writeText(ProjectJson.toJson(project))
                if (!tmp.renameTo(File(dir, "${project.id}.json"))) {
                    File(dir, "${project.id}.json").writeText(tmp.readText())
                    tmp.delete()
                }
            }
        }
    }

    suspend fun delete(id: String) {
        mutex.withLock {
            withContext(Dispatchers.IO) { File(dir, "$id.json").delete() }
        }
    }
}
