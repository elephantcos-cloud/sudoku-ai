package com.shohan.sudokuai.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.min

class SudokuBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Board state
    private var board = Array(9) { IntArray(9) }
    private var givenCells = Array(9) { BooleanArray(9) }
    private var errorCells = Array(9) { BooleanArray(9) }
    private var notesCells = Array(9) { Array(9) { BooleanSet() } }

    // Selection
    var selectedRow = -1
        private set
    var selectedCol = -1
        private set
    var selectedNumber = 0

    // Theme
    private var isDark = false

    // Colors
    private var colorBg = Color.WHITE
    private var colorSurface = Color.parseColor("#F8F8F8")
    private var colorLineThin = Color.parseColor("#AAAAAA")
    private var colorLineThick = Color.BLACK
    private var colorCellSelected = Color.parseColor("#BBDEFB")
    private var colorCellHighlight = Color.parseColor("#E8F4FD")
    private var colorCellSame = Color.parseColor("#C5CAE9")
    private var colorNumberGiven = Color.parseColor("#111111")
    private var colorNumberUser = Color.parseColor("#1565C0")
    private var colorNumberError = Color.parseColor("#C62828")
    private var colorNumberSame = Color.parseColor("#5C6BC0")
    private var colorAccent = Color.parseColor("#1565C0")
    private var colorBoxBorder = Color.parseColor("#333333")

    // Paints
    private val bgPaint = Paint()
    private val cellPaint = Paint()
    private val thinLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val thickLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val givenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val userPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val errorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val samePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val notesPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    // Dimensions
    private var cellSize = 0f
    private var boardSize = 0f
    private var padding = 4f

    // Animation
    private var animatedCells = mutableMapOf<Pair<Int, Int>, Float>()

    // Callbacks
    var onCellTouched: ((row: Int, col: Int) -> Unit)? = null
    var isEditable: Boolean = true

    init {
        applyTheme()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val size = if (h > 0) min(w, h) else w
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        padding = w * 0.012f
        boardSize = w - 2 * padding
        cellSize = boardSize / 9f
        updatePaintSizes()
    }

    override fun onDraw(canvas: Canvas) {
        drawBackground(canvas)
        drawHighlights(canvas)
        drawGrid(canvas)
        drawNumbers(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        bgPaint.color = colorBg
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
    }

    private fun drawHighlights(canvas: Canvas) {
        for (row in 0..8) {
            for (col in 0..8) {
                val left = padding + col * cellSize
                val top = padding + row * cellSize
                val right = left + cellSize
                val bottom = top + cellSize

                val color = when {
                    row == selectedRow && col == selectedCol -> colorCellSelected
                    selectedRow >= 0 && isInSameGroup(row, col, selectedRow, selectedCol) -> colorCellHighlight
                    selectedNumber != 0 && board[row][col] == selectedNumber && board[row][col] != 0 -> colorCellSame
                    else -> null
                }

                if (color != null) {
                    val animScale = animatedCells[row to col] ?: 1f
                    val cx = (left + right) / 2
                    val cy = (top + bottom) / 2
                    val hw = (right - left) / 2 * animScale
                    val hh = (bottom - top) / 2 * animScale
                    cellPaint.color = color
                    canvas.drawRect(cx - hw, cy - hh, cx + hw, cy + hh, cellPaint)
                }
            }
        }
    }

    private fun drawGrid(canvas: Canvas) {
        for (i in 0..9) {
            val isBoxLine = i % 3 == 0
            val paint = if (isBoxLine) thickLinePaint else thinLinePaint
            val pos = padding + i * cellSize

            canvas.drawLine(padding, pos, padding + boardSize, pos, paint)
            canvas.drawLine(pos, padding, pos, padding + boardSize, paint)
        }
        // Outer border
        outerPaint.color = colorLineThick
        outerPaint.strokeWidth = thickLinePaint.strokeWidth + 1f
        val rect = RectF(padding, padding, padding + boardSize, padding + boardSize)
        canvas.drawRect(rect, outerPaint)
    }

    private fun drawNumbers(canvas: Canvas) {
        for (row in 0..8) {
            for (col in 0..8) {
                val num = board[row][col]
                val cx = padding + col * cellSize + cellSize / 2f
                val cy = padding + row * cellSize + cellSize / 2f

                if (num != 0) {
                    val paint = when {
                        errorCells[row][col] -> errorPaint
                        givenCells[row][col] -> givenPaint
                        selectedNumber != 0 && num == selectedNumber -> samePaint
                        else -> userPaint
                    }
                    val animScale = animatedCells[row to col] ?: 1f
                    val scaledSize = paint.textSize * animScale
                    val origSize = paint.textSize
                    paint.textSize = scaledSize
                    canvas.drawText(num.toString(), cx, cy + scaledSize * 0.35f, paint)
                    paint.textSize = origSize
                }
            }
        }
    }

    private fun isInSameGroup(r1: Int, c1: Int, r2: Int, c2: Int): Boolean {
        return r1 == r2 || c1 == c2 || (r1 / 3 == r2 / 3 && c1 / 3 == c2 / 3)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEditable) return false
        if (event.action == MotionEvent.ACTION_UP) {
            val col = ((event.x - padding) / cellSize).toInt()
            val row = ((event.y - padding) / cellSize).toInt()
            if (row in 0..8 && col in 0..8) {
                selectedRow = row
                selectedCol = col
                onCellTouched?.invoke(row, col)
                invalidate()
                return true
            }
        }
        return true
    }

    // ──── Public API ────

    fun setBoard(puzzle: Array<IntArray>, givens: Array<BooleanArray>) {
        board = puzzle.map { it.clone() }.toTypedArray()
        givenCells = givens.map { it.clone() }.toTypedArray()
        errorCells = Array(9) { BooleanArray(9) }
        invalidate()
    }

    fun updateCell(row: Int, col: Int, value: Int, animate: Boolean = true) {
        board[row][col] = value
        if (animate && value != 0) {
            animateCell(row, col)
        } else {
            animatedCells.remove(row to col)
        }
        invalidate()
    }

    fun setErrors(errors: Array<BooleanArray>) {
        errorCells = errors.map { it.clone() }.toTypedArray()
        invalidate()
    }

    fun setSelectedNumber(num: Int) {
        selectedNumber = num
        invalidate()
    }

    fun clearSelection() {
        selectedRow = -1
        selectedCol = -1
        selectedNumber = 0
        invalidate()
    }

    fun getBoard(): Array<IntArray> = board.map { it.clone() }.toTypedArray()

    fun applyTheme(dark: Boolean = isDark) {
        isDark = dark
        if (isDark) {
            colorBg = Color.BLACK
            colorSurface = Color.parseColor("#0D0D0D")
            colorLineThin = Color.parseColor("#3A3A3A")
            colorLineThick = Color.parseColor("#BBBBBB")
            colorBoxBorder = Color.parseColor("#CCCCCC")
            colorCellSelected = Color.parseColor("#1A237E")
            colorCellHighlight = Color.parseColor("#0D1B2A")
            colorCellSame = Color.parseColor("#263238")
            colorNumberGiven = Color.WHITE
            colorNumberUser = Color.parseColor("#90CAF9")
            colorNumberError = Color.parseColor("#EF5350")
            colorNumberSame = Color.parseColor("#B39DDB")
            colorAccent = Color.parseColor("#7986CB")
        } else {
            colorBg = Color.WHITE
            colorSurface = Color.parseColor("#F8F8F8")
            colorLineThin = Color.parseColor("#CACACA")
            colorLineThick = Color.parseColor("#222222")
            colorBoxBorder = Color.BLACK
            colorCellSelected = Color.parseColor("#BBDEFB")
            colorCellHighlight = Color.parseColor("#EDF6FF")
            colorCellSame = Color.parseColor("#D1C4E9")
            colorNumberGiven = Color.parseColor("#111111")
            colorNumberUser = Color.parseColor("#1565C0")
            colorNumberError = Color.parseColor("#C62828")
            colorNumberSame = Color.parseColor("#512DA8")
            colorAccent = Color.parseColor("#1565C0")
        }
        updatePaintSizes()
        invalidate()
    }

    private fun updatePaintSizes() {
        val textSize = cellSize * 0.56f
        val notesSize = cellSize * 0.22f

        thinLinePaint.apply {
            color = colorLineThin
            strokeWidth = 1f
        }
        thickLinePaint.apply {
            color = colorBoxBorder
            strokeWidth = maxOf(2.5f, cellSize * 0.06f)
        }
        outerPaint.apply {
            color = colorBoxBorder
            strokeWidth = maxOf(3f, cellSize * 0.08f)
        }
        givenPaint.apply {
            color = colorNumberGiven
            this.textSize = textSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        userPaint.apply {
            color = colorNumberUser
            this.textSize = textSize
            typeface = Typeface.DEFAULT
        }
        errorPaint.apply {
            color = colorNumberError
            this.textSize = textSize
            typeface = Typeface.DEFAULT
        }
        samePaint.apply {
            color = colorNumberSame
            this.textSize = textSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        notesPaint.apply {
            color = colorLineThin
            this.textSize = notesSize
        }
    }

    private fun animateCell(row: Int, col: Int) {
        val animator = ValueAnimator.ofFloat(0.3f, 1f)
        animator.duration = 180
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener {
            animatedCells[row to col] = it.animatedValue as Float
            invalidate()
        }
        animator.start()
    }

    private class BooleanSet : HashSet<Int>()
}
