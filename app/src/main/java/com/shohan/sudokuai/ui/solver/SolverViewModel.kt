package com.shohan.sudokuai.ui.solver

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohan.sudokuai.engine.SudokuSolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SolverViewModel : ViewModel() {

    private var currentBoard = Array(9) { IntArray(9) }

    private val _board = MutableLiveData<Array<IntArray>>()
    val board: LiveData<Array<IntArray>> = _board

    private val _errors = MutableLiveData<Array<BooleanArray>>()
    val errors: LiveData<Array<BooleanArray>> = _errors

    private val _solveSteps = MutableLiveData<List<SudokuSolver.SolveStep>>()
    val solveSteps: LiveData<List<SudokuSolver.SolveStep>> = _solveSteps

    private val _isSolving = MutableLiveData(false)
    val isSolving: LiveData<Boolean> = _isSolving

    private val _isSolved = MutableLiveData(false)
    val isSolved: LiveData<Boolean> = _isSolved

    private val _selectedNumber = MutableLiveData(0)
    val selectedNumber: LiveData<Int> = _selectedNumber

    private val _selectedCell = MutableLiveData(Pair(-1, -1))
    val selectedCell: LiveData<Pair<Int, Int>> = _selectedCell

    private val _statusMessage = MutableLiveData("")
    val statusMessage: LiveData<String> = _statusMessage

    init {
        clearBoard()
    }

    fun onCellTouched(row: Int, col: Int) {
        _selectedCell.value = Pair(row, col)
        val num = _selectedNumber.value ?: 0
        if (num != 0) placeNumber(row, col, num)
    }

    fun onNumberSelected(num: Int) {
        val current = _selectedNumber.value ?: 0
        _selectedNumber.value = if (current == num) 0 else num
        val cell = _selectedCell.value
        if (cell != null && cell.first >= 0 && _selectedNumber.value != 0) {
            placeNumber(cell.first, cell.second, _selectedNumber.value ?: 0)
        }
    }

    fun onErase() {
        _selectedNumber.value = 0
        val cell = _selectedCell.value ?: return
        if (cell.first < 0) return
        currentBoard[cell.first][cell.second] = 0
        publishBoard()
        validateBoard()
    }

    private fun placeNumber(row: Int, col: Int, num: Int) {
        if (row < 0 || col < 0 || num == 0) return
        currentBoard[row][col] = num
        publishBoard()
        validateBoard()
        _isSolved.value = false
        _solveSteps.value = emptyList()
    }

    fun clearBoard() {
        currentBoard = Array(9) { IntArray(9) }
        publishBoard()
        _errors.value = Array(9) { BooleanArray(9) }
        _isSolved.value = false
        _solveSteps.value = emptyList()
        _statusMessage.value = "Enter your Sudoku puzzle, then tap Solve"
    }

    fun solveWithAI() {
        if (!SudokuSolver.isValidGrid(currentBoard) && currentBoard.any { row -> row.any { it != 0 } }) {
            val allZero = currentBoard.all { row -> row.all { it == 0 } }
            if (allZero) {
                _statusMessage.value = "Please enter a Sudoku puzzle first"
                return
            }
        }

        _isSolving.value = true
        _statusMessage.value = "AI is solving..."
        viewModelScope.launch(Dispatchers.IO) {
            val grid = currentBoard.map { it.clone() }.toTypedArray()
            val result = SudokuSolver.solve(grid)
            withContext(Dispatchers.Main) {
                _isSolving.value = false
                if (result.solved && result.solution != null) {
                    currentBoard = result.solution.map { it.clone() }.toTypedArray()
                    publishBoard()
                    _errors.value = Array(9) { BooleanArray(9) }
                    _solveSteps.value = result.steps
                    _isSolved.value = true
                    _statusMessage.value = "Solved! ${result.steps.size} steps taken"
                } else {
                    _statusMessage.value = "No solution found. Please check your puzzle."
                    _isSolved.value = false
                }
            }
        }
    }

    private fun validateBoard() {
        val errors = Array(9) { BooleanArray(9) }
        for (row in 0..8) {
            for (col in 0..8) {
                val num = currentBoard[row][col]
                if (num != 0 && !SudokuSolver.isValidPlacement(currentBoard, row, col, num)) {
                    errors[row][col] = true
                }
            }
        }
        _errors.value = errors
    }

    private fun publishBoard() {
        _board.value = currentBoard.map { it.clone() }.toTypedArray()
    }

    fun getGivenCells(): Array<BooleanArray> = Array(9) { BooleanArray(9) }
    fun getCurrentBoard(): Array<IntArray> = currentBoard.map { it.clone() }.toTypedArray()
}
