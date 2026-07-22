package com.guilherme.anotaplus

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.Carteira
import com.guilherme.anotaplus.data.Category
import com.guilherme.anotaplus.data.CategoriaEstilo
import com.guilherme.anotaplus.data.CategoryDao
import com.guilherme.anotaplus.data.SubscriptionPrefs
import com.guilherme.anotaplus.databinding.ActivityCategoriasBinding
import com.guilherme.anotaplus.databinding.DialogEditarCategoriaBinding
import com.guilherme.anotaplus.databinding.ItemCarteiraBinding
import com.guilherme.anotaplus.databinding.ItemCategoryBinding
import com.guilherme.anotaplus.widget.WidgetUpdater
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
                    row.textIconeCategoria.text = categoria.icone.orEmpty()
                    row.frameSwatchCategoria.background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(CategoriaEstilo.corInt(categoria.cor))
                    }
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
                        mostrarDialogoEditarCategoria(categoria, categoryDao)
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

    // Edição completa de uma categoria: renomear, cor, ícone e limite
    // mensal (orçamento), tudo num dialog só. Renomear é só local (não
    // sincroniza — ver comentário em Category.kt), mas propaga pros
    // lançamentos existentes (categoria é string livre em Entry, não FK).
    private fun mostrarDialogoEditarCategoria(categoria: Category, categoryDao: CategoryDao) {
        val dialogBinding = DialogEditarCategoriaBinding.inflate(layoutInflater)
        dialogBinding.editNomeCategoria.setText(categoria.nome)
        dialogBinding.editLimiteCategoria.setText(
            categoria.limite?.let { String.format(MesUtil.locale, "%.2f", it) }
        )

        var corSelecionada = categoria.cor ?: CategoriaEstilo.CORES.first()
        var iconeSelecionado = categoria.icone

        popularCores(dialogBinding.containerCores, corSelecionada) { corSelecionada = it }
        popularIcones(dialogBinding.containerIcones, iconeSelecionado) { iconeSelecionado = it }

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.title_editar_categoria))
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_salvar) { _, _ ->
                val novoNome = dialogBinding.editNomeCategoria.text?.toString()?.trim().orEmpty()
                if (novoNome.isEmpty()) {
                    Toast.makeText(this, R.string.error_nome_categoria_obrigatorio, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val novoLimite = dialogBinding.editLimiteCategoria.text?.toString()?.trim()
                    ?.replace(",", ".")?.toDoubleOrNull()

                lifecycleScope.launch {
                    categoryDao.atualizar(categoria.id, novoNome, corSelecionada, iconeSelecionado, novoLimite)
                    if (novoNome != categoria.nome) {
                        AppDatabase.getInstance(applicationContext).entryDao()
                            .renomearCategoriaEmTodosLancamentos(categoria.nome, novoNome)
                        WidgetUpdater.atualizarTodos(applicationContext)
                    }
                    if (novoLimite != categoria.limite && SubscriptionPrefs.podeFazerBackup(this@CategoriasActivity)) {
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

    // Redesenha a lista inteira de swatches a cada toque (paleta é pequena,
    // 6 cores — sem custo perceptível) só pra manter o anel de seleção
    // sempre coerente com a cor escolhida no momento.
    private fun popularCores(container: LinearLayout, corInicial: String, aoSelecionar: (String) -> Unit) {
        var corAtual = corInicial

        fun redesenhar() {
            container.removeAllViews()
            val tamanho = dpParaPx(36)
            CategoriaEstilo.CORES.forEach { hex ->
                val selecionada = hex.equals(corAtual, ignoreCase = true)
                val frame = FrameLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(tamanho, tamanho).apply {
                        marginEnd = dpParaPx(10)
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor(hex))
                        if (selecionada) {
                            setStroke(dpParaPx(3), ContextCompat.getColor(this@CategoriasActivity, R.color.ink))
                        }
                    }
                    setOnClickListener {
                        corAtual = hex
                        aoSelecionar(hex)
                        redesenhar()
                    }
                }
                container.addView(frame)
            }
        }
        redesenhar()
    }

    // Ícone é opcional: tocar de novo no já selecionado desmarca.
    private fun popularIcones(container: LinearLayout, iconeInicial: String?, aoSelecionar: (String?) -> Unit) {
        var iconeAtual = iconeInicial

        fun redesenhar() {
            container.removeAllViews()
            val tamanho = dpParaPx(40)
            CategoriaEstilo.ICONES.forEach { emoji ->
                val selecionado = emoji == iconeAtual
                val texto = TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(tamanho, tamanho).apply {
                        marginEnd = dpParaPx(8)
                    }
                    gravity = Gravity.CENTER
                    textSize = 18f
                    text = emoji
                    background = if (selecionado) {
                        GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(ContextCompat.getColor(this@CategoriasActivity, R.color.brass_pill_bg))
                        }
                    } else {
                        null
                    }
                    setOnClickListener {
                        iconeAtual = if (iconeAtual == emoji) null else emoji
                        aoSelecionar(iconeAtual)
                        redesenhar()
                    }
                }
                container.addView(texto)
            }
        }
        redesenhar()
    }

    private fun dpParaPx(dp: Int): Int = (dp * resources.displayMetrics.density).roundToInt()
}
