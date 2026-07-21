package com.guilherme.anotaplus

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            SyncManager.sincronizarTudo(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private val CONSTRAINTS = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Chamado depois de salvar localmente (se o usuário estiver
        // logado) — roda em segundo plano via WorkManager, sobrevivendo à
        // activity que disparou (QuickCaptureActivity sempre finish() logo
        // em seguida) e respeitando conectividade/bateria automaticamente.
        fun agendar(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(CONSTRAINTS)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
