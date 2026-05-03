package com.shohan.sudokuai.engine

object SudokuSolver {

    data class SolveStep(
        val row: Int,
        val col: Int,
        val value: Int,
        val technique: String,
        val description: String,
        val isUndo: Boolean = false,
        val boardSnapshot: Array<IntArray>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SolveStep) return false
            return row == other.row && col == other.col && value == other.value
                    && technique == other.technique
        }

        override fun hashCode(): Int {
            var result = row
            result = 31 * result + col
            result = 31 * result + value
            return result
        }
    }

    data class SolveResult(
        val solved: Boolean,
        val solution: Array<IntArray>?,
        val steps: List<SolveStep>
    )

    fun solve(initialGrid: Array<IntArray>): SolveResult {
        val grid = initialGrid.map { it.clone() }.toTypedArray()
        val steps = mutableListOf<SolveStep>()
        val solved = solveWithSteps(grid, steps)
        return SolveResult(solved, if (solved) grid else null, steps)
    }

    private fun solveWithSteps(grid: Array<IntArray>, steps: MutableList<SolveStep>): Boolean {
        var progress = true
        while (progress) {
            progress = false
            if (applyNakedSingles(grid, steps)) { progress = true; continue }
            if (applyHiddenSinglesByRow(grid, steps)) { progress = true; continue }
            if (applyHiddenSinglesByCol(grid, steps)) { progress = true; continue }
            if (applyHiddenSinglesByBox(grid, steps)) { progress = true; continue }
        }
        if (isSolved(grid)) return true
        return backtrack(grid, steps)
    }

    private fun applyNakedSingles(grid: Array<IntArray>, steps: MutableList<SolveStep>): Boolean {
        var found = false
        for (row in 0..8) {
            for (col in 0..8) {
                if (grid[row][col] != 0) continue
                val candidates = getPossibles(grid, row, col)
                if (candidates.size == 1) {
                    val value = candidates.first()
                    grid[row][col] = value
                    steps.add(
                        SolveStep(
                            row = row, col = col, value = value,
                            technique = "Naked Single",
                            description = "Row ${row + 1}, Column ${col + 1}: Only " +
                                    "$value can be placed here. Every other digit already " +
                                    "appears in this row, column, or 3x3 box.",
                            boardSnapshot = grid.map { it.clone() }.toTypedArray()
                        )
                    )
                    found = true
                }
            }
        }
        return found
    }

    private fun applyHiddenSinglesByRow(grid: Array<IntArray>, steps: MutableList<SolveStep>): Boolean {
        for (row in 0..8) {
            for (num in 1..9) {
                val positions = (0..8).filter { col ->
                    grid[row][col] == 0 && getPossibles(grid, row, col).contains(num)
                }
                if (positions.size == 1) {
                    val col = positions[0]
                    grid[row][col] = num
                    steps.add(
                        SolveStep(
                            row = row, col = col, value = num,
                            technique = "Hidden Single (Row)",
                            description = "Row ${row + 1}: The digit $num can only fit " +
                                    "in Column ${col + 1}. No other cell in this row allows $num.",
                            boardSnapshot = grid.map { it.clone() }.toTypedArray()
                        )
                    )
                    return true
                }
            }
        }
        return false
    }

    private fun applyHiddenSinglesByCol(grid: Array<IntArray>, steps: MutableList<SolveStep>): Boolean {
        for (col in 0..8) {
            for (num in 1..9) {
                val positions = (0..8).filter { row ->
                    grid[row][col] == 0 && getPossibles(grid, row, col).contains(num)
                }
                if (positions.size == 1) {
                    val row = positions[0]
                    grid[row][col] = num
                    steps.add(
                        SolveStep(
                            row = row, col = col, value = num,
                            technique = "Hidden Single (Column)",
                            description = "Column ${col + 1}: The digit $num can only fit " +
                                    "in Row ${row + 1}. No other cell in this column allows $num.",
                            boardSnapshot = grid.map { it.clone() }.toTypedArray()
                        )
                    )
                    return true
                }
            }
        }
        return false
    }

    private fun applyHiddenSinglesByBox(grid: Array<IntArray>, steps: MutableList<SolveStep>): Boolean {
        for (boxRow in 0..2) {
            for (boxCol in 0..2) {
                for (num in 1..9) {
                    val positions = mutableListOf<Pair<Int, Int>>()
                    for (r in 0..2) {
                        for (c in 0..2) {
                            val row = boxRow * 3 + r
                            val col = boxCol * 3 + c
                            if (grid[row][col] == 0 && getPossibles(grid, row, col).contains(num)) {
                                positions.add(row to col)
                            }
                        }
                    }
                    if (positions.size == 1) {
                        val (row, col) = positions[0]
                        grid[row][col] = num
                        steps.add(
                            SolveStep(
                                row = row, col = col, value = num,
                                technique = "Hidden Single (Box)",
                                description = "3x3 Box (${boxRow + 1},${boxCol + 1}): " +
                                        "The digit $num can only be placed at Row ${row + 1}, " +
                                        "Column ${col + 1} within this box.",
                                boardSnapshot = grid.map { it.clone() }.toTypedArray()
                            )
                        )
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun backtrack(grid: Array<IntArray>, steps: MutableList<SolveStep>): Boolean {
        var minCandidates = 10
        var targetRow = -1
        var targetCol = -1

        for (row in 0..8) {
            for (col in 0..8) {
                if (grid[row][col] == 0) {
                    val candidates = getPossibles(grid, row, col)
                    if (candidates.isEmpty()) return false
                    if (candidates.size < minCandidates) {
                        minCandidates = candidates.size
                        targetRow = row
                        targetCol = col
                    }
                }
            }
        }

        if (targetRow == -1) return isSolved(grid)

        val candidates = getPossibles(grid, targetRow, targetCol)
        for (value in candidates) {
            grid[targetRow][targetCol] = value
            steps.add(
                SolveStep(
                    row = targetRow, col = targetCol, value = value,
                    technique = "Backtracking",
                    description = "Trying $value at Row ${targetRow + 1}, Column ${targetCol + 1} " +
                            "(logical deduction exhausted, using intelligent trial).",
                    boardSnapshot = grid.map { it.clone() }.toTypedArray()
                )
            )
            if (backtrack(grid, steps)) return true
            grid[targetRow][targetCol] = 0
            steps.add(
                SolveStep(
                    row = targetRow, col = targetCol, value = 0,
                    technique = "Backtracking Undo",
                    description = "$value caused a contradiction at Row ${targetRow + 1}, " +
                            "Column ${targetCol + 1}. Reverting and trying next candidate.",
                    isUndo = true,
                    boardSnapshot = grid.map { it.clone() }.toTypedArray()
                )
            )
        }
        return false
    }

    fun findHint(grid: Array<IntArray>): SolveStep? {
        // Naked singles first
        for (row in 0..8) {
            for (col in 0..8) {
                if (grid[row][col] != 0) continue
                val candidates = getPossibles(grid, row, col)
                if (candidates.size == 1) {
                    val value = candidates.first()
                    return SolveStep(
                        row = row, col = col, value = value,
                        technique = "Naked Single",
                        description = "Row ${row + 1}, Column ${col + 1}: Only $value fits here.",
                        boardSnapshot = grid.map { it.clone() }.toTypedArray()
                    )
                }
            }
        }
        // Hidden singles by row
        for (row in 0..8) {
            for (num in 1..9) {
                val positions = (0..8).filter { col ->
                    grid[row][col] == 0 && getPossibles(grid, row, col).contains(num)
                }
                if (positions.size == 1) {
                    val col = positions[0]
                    return SolveStep(
                        row = row, col = col, value = num,
                        technique = "Hidden Single",
                        description = "Row ${row + 1}: $num can only go in Column ${col + 1}.",
                        boardSnapshot = grid.map { it.clone() }.toTypedArray()
                    )
                }
            }
        }
        // Hidden singles by col
        for (col in 0..8) {
            for (num in 1..9) {
                val positions = (0..8).filter { row ->
                    grid[row][col] == 0 && getPossibles(grid, row, col).contains(num)
                }
                if (positions.size == 1) {
                    val row = positions[0]
                    return SolveStep(
                        row = row, col = col, value = num,
                        technique = "Hidden Single",
                        description = "Column ${col + 1}: $num can only go in Row ${row + 1}.",
                        boardSnapshot = grid.map { it.clone() }.toTypedArray()
                    )
                }
            }
        }
        // Hidden singles by box
        for (boxRow in 0..2) {
            for (boxCol in 0..2) {
                for (num in 1..9) {
                    val positions = mutableListOf<Pair<Int, Int>>()
                    for (r in 0..2) {
                        for (c in 0..2) {
                            val row = boxRow * 3 + r
                            val col = boxCol * 3 + c
                            if (grid[row][col] == 0 && getPossibles(grid, row, col).contains(num)) {
                                positions.add(row to col)
                            }
                        }
                    }
                    if (positions.size == 1) {
                        val (row, col) = positions[0]
                        return SolveStep(
                            row = row, col = col, value = num,
                            technique = "Hidden Single",
                            description = "3x3 Box: $num must go at Row ${row + 1}, Column ${col + 1}.",
                            boardSnapshot = grid.map { it.clone() }.toTypedArray()
                        )
                    }
                }
            }
        }
        // Backtrack solve to get correct value for one cell
        for (row in 0..8) {
            for (col in 0..8) {
                if (grid[row][col] == 0) {
                    val solveGrid = grid.map { it.clone() }.toTypedArray()
                    if (backtrack(solveGrid, mutableListOf())) {
                        val correctValue = solveGrid[row][col]
                        return SolveStep(
                            row = row, col = col, value = correctValue,
                            technique = "AI Analysis",
                            description = "Row ${row + 1}, Column ${col + 1}: " +
                                    "$correctValue is the correct value (determined by deep AI analysis).",
                            boardSnapshot = grid.map { it.clone() }.toTypedArray()
                        )
                    }
                }
            }
        }
        return null
    }

    fun getPossibles(grid: Array<IntArray>, row: Int, col: Int): Set<Int> {
        val used = mutableSetOf<Int>()
        for (c in 0..8) if (grid[row][c] != 0) used.add(grid[row][c])
        for (r in 0..8) if (grid[r][col] != 0) used.add(grid[r][col])
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        for (r in boxRow until boxRow + 3)
            for (c in boxCol until boxCol + 3)
                if (grid[r][c] != 0) used.add(grid[r][c])
        return (1..9).filter { it !in used }.toSet()
    }

    fun isSolved(grid: Array<IntArray>): Boolean {
        if (grid.any { row -> row.any { it == 0 } }) return false
        return isValidGrid(grid)
    }

    fun isValidGrid(grid: Array<IntArray>): Boolean {
        val expected = (1..9).toSet()
        for (row in 0..8) {
            if (grid[row].toSet() != expected) return false
        }
        for (col in 0..8) {
            if ((0..8).map { grid[it][col] }.toSet() != expected) return false
        }
        for (br in 0..2) {
            for (bc in 0..2) {
                val nums = mutableSetOf<Int>()
                for (r in 0..2) for (c in 0..2) nums.add(grid[br * 3 + r][bc * 3 + c])
                if (nums != expected) return false
            }
        }
        return true
    }

    fun isValidPlacement(grid: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        if (num == 0) return true
        for (c in 0..8) if (c != col && grid[row][c] == num) return false
        for (r in 0..8) if (r != row && grid[r][col] == num) return false
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        for (r in boxRow until boxRow + 3)
            for (c in boxCol until boxCol + 3)
                if ((r != row || c != col) && grid[r][c] == num) return false
        return true
    }

    fun stringToGrid(s: String): Array<IntArray> {
        val flat = s.replace(Regex("[^0-9]"), "")
        return Array(9) { row -> IntArray(9) { col -> flat[row * 9 + col].digitToInt() } }
    }
}
