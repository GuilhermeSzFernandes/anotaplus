package com.guilherme.anotaplus

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.EntryType
import com.guilherme.anotaplus.data.SubscriptionPrefs
import com.guilherme.anotaplus.databinding.ActivityAnotacoesBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AnotacoesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnotacoesBinding
    private val adapter = EntryAdapter { entry ->
        startActivity(
            Intent(this, ManualIdeiaActivity::class.java)
                .putExtra(ManualIdeiaActivity.EXTRA_ENTRY_ID, entry.id)
        )
    }
    private var adViewBanner: AdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnotacoesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        aplicarEdgeToEdge(binding.root, binding.header, binding.bottomNav.root)

        configurarBottomNav(binding.bottomNav, NavTab.ANOTACOES)
        binding.btnAddIdeia.setOnClickListener {
            startActivity(Intent(this, ManualIdeiaActivity::class.java))
        }
        binding.btnEmptyAdd.setOnClickListener {
            startActivity(Intent(this, ManualIdeiaActivity::class.java))
        }

        binding.recyclerEntries.layoutManager = LinearLayoutManager(this)
        binding.recyclerEntries.adapter = adapter

        val dao = AppDatabase.getInstance(applicationContext).entryDao()

        lifecycleScope.launch {
            dao.getByType(EntryType.PENSAMENTO).collectLatest { entries ->
                adapter.submitList(entries)
                binding.layoutEmpty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        atualizarBanner()
    }

    // runCatching em volta de tudo: anúncio é sempre secundário, uma falha
    // do AdMob não pode derrubar a tela (ver FinanceiroActivity, mesmo padrão).
    private fun atualizarBanner() {
        runCatching { adViewBanner?.destroy() }
        binding.adContainer.removeAllViews()
        adViewBanner = null

        if (SubscriptionPrefs.isPro(this)) return

        runCatching {
            val adView = AdView(this).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BuildConfig.AD_BANNER_UNIT_ID
            }
            adViewBanner = adView
            binding.adContainer.addView(adView)
            adView.loadAd(AdRequest.Builder().build())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { adViewBanner?.destroy() }
    }
}
