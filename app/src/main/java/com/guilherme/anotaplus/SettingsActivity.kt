package com.guilherme.anotaplus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.Category
import com.guilherme.anotaplus.data.EntryType
import com.guilherme.anotaplus.data.Prefs
import com.guilherme.anotaplus.data.SessionPrefs
import com.guilherme.anotaplus.databinding.ActivitySettingsBinding
import com.guilherme.anotaplus.databinding.ItemCategoryBinding
import com.guilherme.anotaplus.network.ApiClient
import com.guilherme.anotaplus.network.dto.GoogleLoginRequest
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        atualizarUiConta()
        binding.btnEntrarGoogle.setOnClickListener { lifecycleScope.launch { entrarComGoogle() } }
        binding.btnSair.setOnClickListener { lifecycleScope.launch { sairDaConta() } }

        val tipoPadrao = Prefs.getTipoPadrao(this)
        if (tipoPadrao == EntryType.GASTO) {
            binding.btnPadraoGasto.isChecked = true
        } else {
            binding.btnPadraoPensamento.isChecked = true
        }
        binding.toggleTipoPadrao.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val tipo = if (checkedId == binding.btnPadraoGasto.id) EntryType.GASTO else EntryType.PENSAMENTO
            Prefs.setTipoPadrao(this, tipo)
        }

        val categoryDao = AppDatabase.getInstance(applicationContext).categoryDao()

        binding.btnAddCategoria.setOnClickListener {
            val nome = binding.editNovaCategoria.text?.toString()?.trim().orEmpty()
            if (nome.isNotEmpty()) {
                lifecycleScope.launch {
                    categoryDao.insert(Category(nome = nome))
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
                        LayoutInflater.from(this@SettingsActivity),
                        binding.containerCategorias,
                        false
                    )
                    row.textNomeCategoria.text = categoria.nome
                    row.btnRemoverCategoria.setOnClickListener {
                        lifecycleScope.launch { categoryDao.deleteById(categoria.id) }
                    }
                    binding.containerCategorias.addView(row.root)
                }
            }
        }
    }

    private fun atualizarUiConta() {
        val logado = SessionPrefs.estaLogado(this)
        binding.textContaStatus.text = if (logado) {
            getString(R.string.conta_logado_como, SessionPrefs.getEmail(this).orEmpty())
        } else {
            getString(R.string.conta_nao_logado)
        }
        binding.btnEntrarGoogle.visibility = if (logado) View.GONE else View.VISIBLE
        binding.btnSair.visibility = if (logado) View.VISIBLE else View.GONE
    }

    private suspend fun entrarComGoogle() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        binding.btnEntrarGoogle.isEnabled = false
        // Fica visível até a próxima tentativa (em vez de um Toast rápido
        // que passa despercebido) — o backend no Render pode levar bem mais
        // que alguns segundos pra responder se estiver "dormindo".
        binding.textContaStatus.text = getString(R.string.conta_entrando)

        try {
            val result = CredentialManager.create(this).getCredential(this, request)
            val credential = result.credential

            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                mostrarErroLogin(getString(R.string.login_erro))
                return
            }

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val resposta = ApiClient.api.loginWithGoogle(GoogleLoginRequest(googleIdTokenCredential.idToken))

            SessionPrefs.salvarSessao(this, resposta.accessToken, resposta.user.email, resposta.user.name)
            atualizarUiConta()
            Toast.makeText(this, getString(R.string.login_sucesso, resposta.user.email), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Cobre tanto falha no picker de contas (GetCredentialException,
            // inclusive o usuário cancelando) quanto erro de rede pro backend.
            // Mostra a mensagem real da exceção pra dar pra diagnosticar sem
            // precisar de logcat.
            mostrarErroLogin(e.message ?: e.toString())
        } finally {
            binding.btnEntrarGoogle.isEnabled = true
        }
    }

    private suspend fun sairDaConta() {
        runCatching { CredentialManager.create(this).clearCredentialState(ClearCredentialStateRequest()) }
        SessionPrefs.limparSessao(this)
        atualizarUiConta()
    }

    private fun mostrarErroLogin(detalhe: String) {
        binding.textContaStatus.text = getString(R.string.login_erro_detalhe, detalhe)
        Toast.makeText(this, R.string.login_erro, Toast.LENGTH_LONG).show()
    }
}
