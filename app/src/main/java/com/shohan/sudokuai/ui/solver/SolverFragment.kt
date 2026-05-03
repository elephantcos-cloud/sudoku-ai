package com.shohan.sudokuai.ui.solver

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.shohan.sudokuai.R
import com.shohan.sudokuai.databinding.FragmentSolverBinding
import com.shohan.sudokuai.engine.SudokuSolver
import com.shohan.sudokuai.ui.game.StepsAdapter
import com.shohan.sudokuai.util.ThemeManager

class SolverFragment : Fragment() {

    private var _binding: FragmentSolverBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SolverViewModel by viewModels()

    private var isDark = false
    private var selectedNumView: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSolverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isDark = ThemeManager.isDarkTheme(requireContext())
        binding.sudokuBoard.applyTheme(isDark)
        binding.sudokuBoard.isEditable = true

        setupToolbar()
        setupNumberPad()
        setupActions()
        observeViewModel()
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
            tv.setOnClickListener {
                selectNumber(index + 1, tv)
                viewModel.onNumberSelected(index + 1)
            }
        }

        binding.btnErase.setOnClickListener {
            deselectAll()
            viewModel.onErase()
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
        binding.sudokuBoard.setSelectedNumber(num)
    }

    private fun deselectAll() {
        val textColor = if (isDark) Color.WHITE else Color.BLACK
        listOf(
            binding.num1, binding.num2, binding.num3,
            binding.num4, binding.num5, binding.num6,
            binding.num7, binding.num8, binding.num9
        ).forEach {
            it.setBackgroundColor(Color.TRANSPARENT)
            it.setTextColor(textColor)
        }
        selectedNumView = null
    }

    private fun setupActions() {
        binding.btnSolveAi.setOnClickListener {
            viewModel.solveWithAI()
        }

        binding.btnClearBoard.setOnClickListener {
            viewModel.clearBoard()
            deselectAll()
            binding.sudokuBoard.setSelectedNumber(0)
            binding.sudokuBoard.clearSelection()
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
            binding.sudokuBoard.setSelectedNumber(num)
            if (num == 0) deselectAll()
        }

        viewModel.isSolving.observe(viewLifecycleOwner) { solving ->
            binding.progressBar.visibility = if (solving) View.VISIBLE else View.GONE
            binding.btnSolveAi.isEnabled = !solving
            binding.btnClearBoard.isEnabled = !solving
            binding.tvStatus.text = if (solving) "AI Engine working..." else ""
        }

        viewModel.statusMessage.observe(viewLifecycleOwner) { msg ->
            if (!viewModel.isSolving.value!!) {
                binding.tvStatus.text = msg
            }
        }

        viewModel.solveSteps.observe(viewLifecycleOwner) { steps ->
            if (steps.isNotEmpty()) {
                binding.btnShowSteps.visibility = View.VISIBLE
                binding.btnShowSteps.setOnClickListener {
                    showStepsBottomSheet(steps)
                }
            } else {
                binding.btnShowSteps.visibility = View.GONE
            }
        }

        viewModel.isSolved.observe(viewLifecycleOwner) { solved ->
            if (solved) {
                binding.sudokuBoard.isEditable = false
            } else {
                binding.sudokuBoard.isEditable = true
            }
        }
    }

    private fun showStepsBottomSheet(steps: List<SudokuSolver.SolveStep>) {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_steps, null)
        dialog.setContentView(sheetView)

        val rvSteps = sheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvSteps)
        val tvStepCount = sheetView.findViewById<TextView>(R.id.tvStepCount)
        val btnClose = sheetView.findViewById<View>(R.id.btnCloseSheet)

        tvStepCount.text = "${steps.size} steps — AI Solve Walkthrough"
        val adapter = StepsAdapter(requireContext(), steps)
        rvSteps.layoutManager = LinearLayoutManager(requireContext())
        rvSteps.adapter = adapter

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
