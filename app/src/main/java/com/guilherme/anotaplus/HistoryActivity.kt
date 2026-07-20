package com.guilherme.anotaplus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.databinding.ActivityHistoryBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private val adapter = EntryAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.recyclerEntries.layoutManager = LinearLayoutManager(this)
        binding.recyclerEntries.adapter = adapter

        lifecycleScope.launch {
            AppDatabase.getInstance(applicationContext).entryDao().getAll().collectLatest { entries ->
                adapter.submitList(entries)
                binding.textEmpty.visibility =
                    if (entries.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
    }
}
