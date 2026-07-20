package com.guilherme.anotaplus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.guilherme.anotaplus.databinding.ActivityPlansBinding

class PlansActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityPlansBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
    }
}
