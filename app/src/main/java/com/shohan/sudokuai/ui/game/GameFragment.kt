package com.shohan.sudokuai.ui.game

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.shohan.sudokuai.R
import com.shohan.sudokuai.databinding.FragmentGameBinding
import com.shohan.sudokuai.engine.Difficulty
import com.shohan.sudokuai.util.ThemeManager

class GameFragment : Fragment() {

    private var _binding: FragmentGameBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GameViewModel by viewModels()

    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            viewModel.tickTimer()
            timerHandler.postDelayed(this, 1000)
        }
    }

    private var isDark = false
    private var selectedNumView: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isDark = ThemeManager.isDarkTheme(requireContext())
        binding.sudokuBoard.applyTheme(isDark)

        setupToolbar()
        setupNumberPad()
        setupActions()
        observeViewModel()
        loadPuzzle()
        timerHandler.postDelayed(timerRunnable, 1000)
    }

    private fun loadPuzzle() {
        val args = arguments
        val puzzleId = args?.getInt("puzzleId", -1) ?: -1
        val difficultyStr = args?.getString("difficulty")

        if (puzzleId > 0) {
            viewModel.loadPuzzleById(puzzleId)
        } else if (difficultyStr != null) {
            val difficulty = try { Difficulty.valueOf(difficultyStr) } catch (e: Exception) { Difficulty.EASY }
            viewModel.loadPuzzle(difficulty)
        } else {
            viewModel.loadPuzzle(Difficulty.EASY)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupNumberPad() {
        val numViews = listOf(
            binding.num1, binding.num2, binding.num3,
            binding.num4, binding.num5, binding.num6,
            binding.num7, binding.num8, binding.num9
        )

        numViews.forEachIndexed { index, tv ->
            tv.tag = index + 1
            tv.setOnClickListener {
                selectNumber(index + 1, tv)
                viewModel.onNumberSelected(index + 1)
            }
        }

        binding.btnErase.setOnClickListener {
            deselectAll()
            viewModel.onEraseSelected()
        }

        binding.btnUndo.setOnClickListener {
            viewModel.undoMove()
        }

        binding.sudokuBoard.onCellTouched = { row, col ->
            viewModel.onCellTouched(row, col)
        }
    }

    private fun selectNumber(num: Int, tv: TextView) {
        deselectAll()
        selectedNumView = tv
        val accent = if (isDark) Color.parseColor("#5C6BC0") else Color.parseColor("#1565C0")
        tv.setBackgroundColor(accent)
        tv.setTextColor(Color.WHITE)
        binding.sudokuBoard.selectedNumber = num
    }

    private fun deselectAll() {
        val textColor = if (isDark) Color.WHITE else Color.BLACK
        val bgColor = Color.TRANSPARENT
        listOf(
            binding.num1, binding.num2, binding.num3,
            binding.num4, binding.num5, binding.num6,
            binding.num7, binding.num8, binding.num9
        ).forEach {
            it.setBackgroundColor(bgColor)
            it.setTextColor(textColor)
        }
        selectedNumView = null
    }

    private fun setupActions() {
        binding.btnHint.setOnClickListener {
            viewModel.requestHint()
        }

        binding.btnAiSolve.setOnClickListener {
            viewModel.requestAiSolve()
        }

        binding.btnRestart.setOnClickListener {
            viewModel.restartPuzzle()
            deselectAll()
            binding.sudokuBoard.selectedNumber = 0
            Snackbar.make(binding.root, "Puzzle restarted", Snackbar.LENGTH_SHORT).show()
        }

        binding.btnCheck.setOnClickListener {
            val errors = viewModel.errors.value
            val hasErrors = errors?.any { row -> row.any { it } } ?: false
            if (hasErrors) {
                Snackbar.make(binding.root, "There are errors in your solution", Snackbar.LENGTH_SHORT).show()
            } else {
                val board = viewModel.getCurrentBoard()
                val filled = board.sumOf { row -> row.count { it != 0 } }
                if (filled == 81) {
                    Snackbar.make(binding.root, "Perfect! Your solution is correct!", Snackbar.LENGTH_LONG).show()
                } else {
                    Snackbar.make(binding.root, "Looking good so far! No errors found.", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.board.observe(viewLifecycleOwner) { board ->
            val givens = viewModel.getGivenCells()
            binding.sudokuBoard.setBoard(board, givens)
        }

        viewModel.errors.observe(viewLifecycleOwner) { errors ->
            binding.sudokuBoard.setErrors(errors)
        }

        viewModel.selectedNumber.observe(viewLifecycleOwner) { num ->
            binding.sudokuBoard.selectedNumber = num
            if (num == 0) deselectAll()
        }

        viewModel.isSolved.observe(viewLifecycleOwner) { solved ->
            if (solved) {
                timerHandler.removeCallbacks(timerRunnable)
                showWinDialog()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnAiSolve.isEnabled = !loading
            binding.btnHint.isEnabled = !loading
        }

        viewModel.timerSeconds.observe(viewLifecycleOwner) { _ ->
            binding.tvTimer.text = viewModel.formatTimer()
        }

        viewModel.difficultyLabel.observe(viewLifecycleOwner) { label ->
            binding.tvDifficulty.text = label
        }

        viewModel.puzzleTitle.observe(viewLifecycleOwner) { title ->
            binding.toolbar.title = title
        }

        viewModel.solveSteps.observe(viewLifecycleOwner) { steps ->
            if (steps.isNotEmpty()) {
                showStepsBottomSheet(steps)
            }
        }

        viewModel.hintStep.observe(viewLifecycleOwner) { step ->
            step?.let {
                Snackbar.make(
                    binding.root,
                    "Hint: ${it.technique} — ${it.description}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }

        viewModel.numberCounts.observe(viewLifecycleOwner) { counts ->
            updateNumberPadCounts(counts)
        }
    }

    private fun updateNumberPadCounts(counts: IntArray) {
        val numViews = listOf(
            binding.num1, binding.num2, binding.num3,
            binding.num4, binding.num5, binding.num6,
            binding.num7, binding.num8, binding.num9
        )
        numViews.forEachIndexed { index, tv ->
            val num = index + 1
            val count = counts.getOrElse(num) { 0 }
            if (count >= 9) {
                tv.alpha = 0.35f
            } else {
                tv.alpha = 1.0f
            }
        }
    }

    private fun showWinDialog() {
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Puzzle Solved!")
            .setMessage("Congratulations! You solved the puzzle in ${viewModel.formatTimer()}.")
            .setPositiveButton("New Game") { _, _ ->
                findNavController().navigateUp()
            }
            .setNegativeButton("Stay", null)
            .create()
        dialog.show()
    }

    private fun showStepsBottomSheet(steps: List<com.shohan.sudokuai.engine.SudokuSolver.SolveStep>) {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_steps, null)
        dialog.setContentView(sheetView)

        val rvSteps = sheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvSteps)
        val tvStepCount = sheetView.findViewById<TextView>(R.id.tvStepCount)
        val btnClose = sheetView.findViewById<View>(R.id.btnCloseSheet)

        tvStepCount.text = "${steps.size} steps to solve"
        val adapter = StepsAdapter(requireContext(), steps)
        rvSteps.layoutManager = LinearLayoutManager(requireContext())
        rvSteps.adapter = adapter

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onPause() {
        super.onPause()
        timerHandler.removeCallbacks(timerRunnable)
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.isSolved.value != true) {
            timerHandler.postDelayed(timerRunnable, 1000)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timerHandler.removeCallbacks(timerRunnable)
        _binding = null
    }
}
