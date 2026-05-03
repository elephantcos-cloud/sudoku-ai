package com.shohan.sudokuai.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.shohan.sudokuai.R
import com.shohan.sudokuai.databinding.FragmentLevelSelectBinding
import com.shohan.sudokuai.engine.Difficulty
import com.shohan.sudokuai.engine.SudokuPuzzle
import com.shohan.sudokuai.engine.SudokuPuzzleDatabase

class LevelSelectFragment : Fragment() {

    private var _binding: FragmentLevelSelectBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLevelSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val allPuzzles = SudokuPuzzleDatabase.getAllPuzzles()
        val adapter = PuzzleAdapter(allPuzzles) { puzzle ->
            navigateToGame(puzzle)
        }

        binding.rvPuzzles.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPuzzles.adapter = adapter

        setupDifficultyFilter()
    }

    private fun setupDifficultyFilter() {
        var currentDifficulty: Difficulty? = null

        val btnAll = binding.chipAll
        val btnEasy = binding.chipEasy
        val btnMedium = binding.chipMedium
        val btnHard = binding.chipHard
        val btnExpert = binding.chipExpert

        fun updateFilter(difficulty: Difficulty?) {
            currentDifficulty = difficulty
            val filtered = if (difficulty == null) SudokuPuzzleDatabase.getAllPuzzles()
            else SudokuPuzzleDatabase.getPuzzlesByDifficulty(difficulty).filter { it.puzzle.any { c -> c == '0' } }
            (binding.rvPuzzles.adapter as PuzzleAdapter).updateList(filtered)
        }

        btnAll.setOnClickListener { updateFilter(null) }
        btnEasy.setOnClickListener { updateFilter(Difficulty.EASY) }
        btnMedium.setOnClickListener { updateFilter(Difficulty.MEDIUM) }
        btnHard.setOnClickListener { updateFilter(Difficulty.HARD) }
        btnExpert.setOnClickListener { updateFilter(Difficulty.EXPERT) }
    }

    private fun navigateToGame(puzzle: SudokuPuzzle) {
        val bundle = Bundle().apply {
            putInt("puzzleId", puzzle.id)
        }
        findNavController().navigate(R.id.action_levelSelectFragment_to_gameFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
