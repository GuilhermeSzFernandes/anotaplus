package com.guilherme.anotaplus

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.Carteira
import com.guilherme.anotaplus.data.Category
import com.guilherme.anotaplus.data.CategoryDao
import com.guilherme.anotaplus.data.SubscriptionPrefs
import com.guilherme.anotaplus.databinding.ActivityCategoriasBinding
import com.guilherme.anotaplus.databinding.ItemCarteiraBinding
import com.guilherme.anotaplus.databinding.ItemCategoryBinding
import kotlinx.coroutines.launch

// Gestão de Categorias e Carteiras — saiu de Conta pra cá porque só faz
// sentido perto de onde você categoriza Gasto/Recebimento (aberta a
// partir de um ícone no header do Financeiro).
class CategoriasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoriasBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoriasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        val categoryDao = AppDatabase.getInstance(applicationContext).categoryDao()

        binding.btnAddCategoria.setOnClickListener {
            val nome = binding.editNovaCategoria.text?.toString()?.trim().orEmpty()
            if (nome.isNotEmpty()) {
                lifecycleScope.launch {
                    categoryDao.insert(Category(nome = nome))
                    if (SubscriptionPrefs.podeFazerBackup(this@CategoriasActivity)) {
                        SyncWorker.agendar(applicationContext)
                    }
                }
                binding.editNovaCategoria.text?.clear()
            }
        }

        lifecycleScope.launch {
            categoryDao.getAll().collect { categorias ->
                binding.containerCategorias.removeAllViews()
                binding.textEmptyCategorias.visibility =
                    if (categorias.isEmpty()) View.VISIBLE else View.GONE

                categorias.forEach { categoria ->
                    val row = ItemCategoryBinding.inflate(
                        LayoutInflater.from(this@CategoriasActivity),
                        binding.containerCategorias,
                        false
                    )
                    row.textNomeCategoria.text = categoria.nome
                    if (categoria.limite != null) {
                        row.textLimiteCategoria.visibility = View.VISIBLE
                        row.textLimiteCategoria.text = getString(
                            R.string.format_limite_categoria,
                            String.format(MesUtil.locale, "%.2f", categoria.limite)
                        )
                    } else {
                        row.textLimiteCategoria.visibility = View.GONE
                    }
                    row.rowCategoriaClicavel.setOnClickListener {
                        mostrarDialogoLimite(categoria, categoryDao)
                    }
                    row.btnRemoverCategoria.setOnClickListener {
                        lifecycleScope.launch { categoryDao.deleteById(categoria.id) }
                    }
                    binding.containerCategorias.addView(row.root)
                }
            }
        }

        val carteiraDao = AppDatabase.getInstance(applicationContext).carteiraDao()

        binding.btnAddCarteira.setOnClickListener {
            val nome = binding.editNovaCarteira.text?.toString()?.trim().orEmpty()
            if (nome.isNotEmpty()) {
                lifecycleScope.launch {
                    carteiraDao.insert(Carteira(nome = nome))
                    if (SubscriptionPrefs.podeFazerBackup(this@CategoriasActivity)) {
                        SyncWorker.agendar(applicationContext)
                    }
                }
                binding.editNovaCarteira.text?.clear()
            }
        }

        lifecycleScope.launch {
            carteiraDao.getAll().collect { carteiras ->
                binding.containerCarteiras.removeAllViews()
                binding.textEmptyCarteiras.visibility =
                    if (carteiras.isEmpty()) View.VISIBLE else View.GONE

                carteiras.forEach { carteira ->
                    val row = ItemCarteiraBinding.inflate(
                        LayoutInflater.from(this@CategoriasActivity),
                        binding.containerCarteiras,
                        false
                    )
                    row.textNomeCarteira.text = carteira.nome
                    row.btnRemoverCarteira.setOnClickListener {
                        lifecycleScope.launch { carteiraDao.deleteById(carteira.id) }
                    }
                    binding.containerCarteiras.addView(row.root)
                }
            }
        }
    }

    // Limite mensal por categoria (orçamento): grava local sempre; só
    // propaga pro backend se o usuário for PRO (mesmo gate do backup) —
    // categoria ainda sem remoteId nem precisa de push isolado aqui, o
    // limite já viaja junto na primeira sincronização.
    private fun mostrarDialogoLimite(categoria: Category, categoryDao: CategoryDao) {
        val editLimite = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = getString(R.string.hint_limite_categoria)
            setPadding(48, 24, 48, 24)
            categoria.limite?.let { setText(String.format(MesUtil.locale, "%.2f", it)) }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.title_definir_limite, categoria.nome))
            .setView(editLimite)
            .setPositiveButton(R.string.btn_salvar_limite) { _, _ ->
                val novoLimite = editLimite.text?.toString()?.trim()?.replace(",", ".")?.toDoubleOrNull()
                lifecycleScope.launch {
                    categoryDao.atualizarLimite(categoria.id, novoLimite)
                    if (SubscriptionPrefs.podeFazerBackup(this@CategoriasActivity)) {
                        val atualizada = categoria.copy(limite = novoLimite)
                        if (atualizada.remoteId != null) {
                            SyncManager.enviarLimiteCategoria(applicationContext, atualizada)
                        } else {
                            SyncWorker.agendar(applicationContext)
                        }
                    }
                }
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }
}
