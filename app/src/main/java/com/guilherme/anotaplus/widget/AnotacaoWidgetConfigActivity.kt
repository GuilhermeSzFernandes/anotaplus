package com.guilherme.anotaplus.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.guilherme.anotaplus.EntryAdapter
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.EntryType
import com.guilherme.anotaplus.databinding.ActivityAnotacaoWidgetConfigBinding
import kotlinx.coroutines.launch

/**
 * Aberta automaticamente pelo sistema (android:configure no
 * widget_anotacao_info.xml) toda vez que uma instância do
 * AnotacaoWidgetProvider é adicionada — deixa escolher qual Ideia esse
 * widget específico vai mostrar.
 */
class AnotacaoWidgetConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnotacaoWidgetConfigBinding
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Resultado padrão CANCELED: se o usuário voltar sem escolher
        // nada, o sistema desfaz a adição do widget sozinho.
        setResult(Activity.RESULT_CANCELED)

        binding = ActivityAnotacaoWidgetConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        binding.toolbar.setNavigationOnClickListener { finish() }

        val adapter = EntryAdapter { entry -> escolher(entry.id) }
        binding.recyclerIdeias.layoutManager = LinearLayoutManager(this)
        binding.recyclerIdeias.adapter = adapter

        lifecycleScope.launch {
            AppDatabase.getInstance(applicationContext).entryDao().getByType(EntryType.PENSAMENTO).collect { lista ->
                adapter.submitList(lista)
                binding.textEmpty.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun escolher(entryId: Long) {
        AnotacaoWidgetPrefs.salvarEntryId(this, appWidgetId, entryId)
        lifecycleScope.launch {
            AnotacaoWidgetUpdater.atualizar(applicationContext, appWidgetId)
            setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
            finish()
        }
    }
}
