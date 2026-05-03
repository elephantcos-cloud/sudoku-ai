package com.shohan.sudokuai.ui.game

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohan.sudokuai.engine.Difficulty
import com.shohan.sudokuai.engine.SudokuPuzzle
import com.shohan.sudokuai.engine.SudokuPuzzleDatabase
import com.shohan.sudokuai.engine.SudokuSolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameViewModel : ViewModel() {

    private var puzzle: SudokuPuzzle? = null
    private var givenCells = Array(9) { BooleanArray(9) }
    private var currentBoard = Array(9) { IntArray(9) }
    private var solutionBoard = Array(9) { IntArray(9) }

    // Move history for undo
    private val moveHistory = ArrayDeque<Triple<Int, Int, Int>>()

    private val _board = MutableLiveData<Array<IntArray>>()
    val board: LiveData<Array<IntArray>> = _board

    private val _givens = MutableLiveData<Array<BooleanArray>>()
    val givens: LiveData<Array<BooleanArray>> = _givens

    private val _errors = MutableLiveData<Array<BooleanArray>>()
    val errors: LiveData<Array<BooleanArray>> = _errors

    private val _selectedNumber = MutableLiveData(0)
    val selectedNumber: LiveData<Int> = _selectedNumber

    private val _selectedCell = MutableLiveData(Pair(-1, -1))
    val selectedCell: LiveData<Pair<Int, Int>> = _selectedCell

    private val _isSolved = MutableLiveData(false)
    val isSolved: LiveData<Boolean> = _isSolved

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _solveSteps = MutableLiveData<List<SudokuSolver.SolveStep>>()
    val solveSteps: LiveData<List<SudokuSolver.SolveStep>> = _solveSteps

    private val _hintStep = MutableLiveData<SudokuSolver.SolveStep?>()
    val hintStep: LiveData<SudokuSolver.SolveStep?> = _hintStep

    private val _timerSeconds = MutableLiveData(0L)
    val timerSeconds: LiveData<Long> = _timerSeconds

    private val _difficultyLabel = MutableLiveData("")
    val difficultyLabel: LiveData<String> = _difficultyLabel

    private val _puzzleTitle = MutableLiveData("")
    val puzzleTitle: LiveData<String> = _puzzleTitle

    private val _numberCounts = MutableLiveData(IntArray(10))
    val numberCounts: LiveData<IntArray> = _numberCounts

    fun loadPuzzle(difficulty: Difficulty) {
        val p = SudokuPuzzleDatabase.getRandomPuzzle(difficulty)
        puzzle = p
        _difficultyLabel.value = difficulty.label
        _puzzleTitle.value = p.title
        initializePuzzle(p)
    }

    fun loadPuzzleById(id: Int) {
        val p = SudokuPuzzleDatabase.getPuzzleById(id) ?: return
        puzzle = p
        _difficultyLabel.value = p.difficulty.label
        _puzzleTitle.value = p.title
        initializePuzzle(p)
    }

    private fun initializePuzzle(p: SudokuPuzzle) {
        val puzzleGrid = p.getPuzzleGrid()
        solutionBoard = p.getSolutionGrid()
        currentBoard = puzzleGrid.map { it.clone() }.toTypedArray()
        givenCells = Array(9) { row -> BooleanArray(9) { col -> puzzleGrid[row][col] != 0 } }
        moveHistory.clear()
        _board.value = currentBoard.map { it.clone() }.toTypedArray()
        _givens.value = givenCells.map { it.clone() }.toTypedArray()
        _errors.value = Array(9) { BooleanArray(9) }
        _isSolved.value = false
        _timerSeconds.value = 0L
        updateNumberCounts()
    }

    fun onCellTouched(row: Int, col: Int) {
        _selectedCell.value = Pair(row, col)
        val selNum = _selectedNumber.value ?: 0
        if (selNum != 0) {
            placeNumber(row, col, selNum)
        }
    }

    fun onNumberSelected(num: Int) {
        val current = _selectedNumber.value ?: 0
        _selectedNumber.value = if (current == num) 0 else num

        // If cell is already selected, place the number
        val cell = _selectedCell.value
        if (cell != null && cell.first >= 0 && _selectedNumber.value != 0) {
            placeNumber(cell.first, cell.second, _selectedNumber.value ?: 0)
        }
    }

    fun onEraseSelected() {
        _selectedNumber.value = 0
        val cell = _selectedCell.value ?: return
        val row = cell.first
        val col = cell.second
        if (row < 0 || col < 0) return
        if (givenCells[row][col]) return
        if (currentBoard[row][col] == 0) return
        moveHistory.addLast(Triple(row, col, currentBoard[row][col]))
        currentBoard[row][col] = 0
        _board.value = currentBoard.map { it.clone() }.toTypedArray()
        validateBoard()
        updateNumberCounts()
    }

    private fun placeNumber(row: Int, col: Int, num: Int) {
        if (row < 0 || col < 0) return
        if (givenCells[row][col]) return
        if (num == 0) return
        val prev = currentBoard[row][col]
        if (prev == num) return
        moveHistory.addLast(Triple(row, col, prev))
        currentBoard[row][col] = num
        _board.value = currentBoard.map { it.clone() }.toTypedArray()
        validateBoard()
        updateNumberCounts()
        checkWin()
    }

    fun undoMove() {
        if (moveHistory.isEmpty()) return
        val (row, col, prev) = moveHistory.removeLast()
        currentBoard[row][col] = prev
        _board.value = currentBoard.map { it.clone() }.toTypedArray()
        validateBoard()
        updateNumberCounts()
    }

    fun restartPuzzle() {
        puzzle?.let { initializePuzzle(it) }
    }

    fun requestHint() {
        viewModelScope.launch(Dispatchers.IO) {
            val hint = SudokuSolver.findHint(currentBoard.map { it.clone() }.toTypedArray())
            withContext(Dispatchers.Main) {
                _hintStep.value = hint
                if (hint != null) {
                    placeNumber(hint.row, hint.col, hint.value)
                }
            }
        }
    }

    fun requestAiSolve() {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val result = SudokuSolver.solve(currentBoard.map { it.clone() }.toTypedArray())
            withContext(Dispatchers.Main) {
                _isLoading.value = false
                if (result.solved && result.solution != null) {
                    _solveSteps.value = result.steps
                    currentBoard = result.solution.map { it.clone() }.toTypedArray()
                    _board.value = currentBoard.map { it.clone() }.toTypedArray()
                    _errors.value = Array(9) { BooleanArray(9) }
                    _isSolved.value = true
                    updateNumberCounts()
                }
            }
        }
    }

    private fun validateBoard() {
        val errors = Array(9) { BooleanArray(9) }
        for (row in 0..8) {
            for (col in 0..8) {
                val num = currentBoard[row][col]
                if (num != 0 && !givenCells[row][col]) {
                    if (!SudokuSolver.isValidPlacement(currentBoard, row, col, num)) {
                        errors[row][col] = true
                    }
                }
            }
        }
        _errors.value = errors
    }

    private fun checkWin() {
        if (SudokuSolver.isSolved(currentBoard)) {
            _isSolved.value = true
        }
    }

    private fun updateNumberCounts() {
        val counts = IntArray(10)
        for (row in 0..8) {
            for (col in 0..8) {
                val n = currentBoard[row][col]
                if (n != 0) counts[n]++
            }
        }
        _numberCounts.value = counts
    }

    fun tickTimer() {
        if (_isSolved.value == true) return
        _timerSeconds.value = (_timerSeconds.value ?: 0L) + 1L
    }

    fun formatTimer(): String {
        val seconds = _timerSeconds.value ?: 0L
        val m = seconds / 60
        val s = seconds % 60
        return String.format("%02d:%02d", m, s)
    }

    fun getGivenCells(): Array<BooleanArray> = givenCells.map { it.clone() }.toTypedArray()
    fun getCurrentBoard(): Array<IntArray> = currentBoard.map { it.clone() }.toTypedArray()
}
