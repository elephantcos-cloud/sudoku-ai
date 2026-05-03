package com.shohan.sudokuai.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.shohan.sudokuai.R
import com.shohan.sudokuai.databinding.FragmentHomeBinding
import com.shohan.sudokuai.engine.Difficulty
import com.shohan.sudokuai.util.ThemeManager

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateThemeButton()

        binding.btnThemeToggle.setOnClickListener {
            ThemeManager.toggleTheme(requireActivity() as androidx.appcompat.app.AppCompatActivity)
        }

        binding.btnEasy.setOnClickListener {
            navigateToGame(Difficulty.EASY)
        }

        binding.btnMedium.setOnClickListener {
            navigateToGame(Difficulty.MEDIUM)
        }

        binding.btnHard.setOnClickListener {
            navigateToGame(Difficulty.HARD)
        }

        binding.btnExpert.setOnClickListener {
            navigateToGame(Difficulty.EXPERT)
        }

        binding.btnAiSolver.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_solverFragment)
        }

        binding.btnPuzzles.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_levelSelectFragment)
        }
    }

    private fun navigateToGame(difficulty: Difficulty) {
        val bundle = Bundle().apply {
            putString("difficulty", difficulty.name)
        }
        findNavController().navigate(R.id.action_homeFragment_to_gameFragment, bundle)
    }

    private fun updateThemeButton() {
        val isDark = ThemeManager.isDarkTheme(requireContext())
        binding.btnThemeToggle.setImageResource(
            if (isDark) R.drawable.ic_sun else R.drawable.ic_moon
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
