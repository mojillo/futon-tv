package io.github.mojillo.futontv.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.landwarderer.futon.main.ui.MainActivity
import io.github.mojillo.futontv.databinding.ActivityTvLauncherBinding

/**
 * TV launcher entry point.
 * Shows a simple welcome screen then launches MainActivity.
 */
class TvLauncherActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityTvLauncherBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTvLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.buttonStart.setOnClickListener {
            startMainActivity()
        }
        
        // Auto-start after a brief moment if user doesn't interact
        binding.buttonStart.postDelayed({
            if (!isFinishing) {
                startMainActivity()
            }
        }, 2000)
    }
    
    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
