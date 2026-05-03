package com.shohan.sudokuai

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.shohan.sudokuai.databinding.ActivityMainBinding
import com.shohan.sudokuai.util.ThemeManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
