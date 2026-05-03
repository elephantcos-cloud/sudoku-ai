package com.shohan.sudokuai.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shohan.sudokuai.databinding.ItemPuzzleBinding
import com.shohan.sudokuai.engine.SudokuPuzzle

class PuzzleAdapter(
    private var puzzles: List<SudokuPuzzle>,
    private val onClick: (SudokuPuzzle) -> Unit
) : RecyclerView.Adapter<PuzzleAdapter.PuzzleViewHolder>() {

    inner class PuzzleViewHolder(val binding: ItemPuzzleBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PuzzleViewHolder {
        val binding = ItemPuzzleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PuzzleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PuzzleViewHolder, position: Int) {
        val puzzle = puzzles[position]
        val b = holder.binding

        b.tvPuzzleTitle.text = puzzle.title
        b.tvPuzzleDifficulty.text = puzzle.difficulty.label
        b.tvPuzzleNumber.text = "#${puzzle.id}"

        try {
            b.tvPuzzleDifficulty.setTextColor(Color.parseColor(puzzle.difficulty.displayColor))
            b.viewDifficultyBar.setBackgroundColor(Color.parseColor(puzzle.difficulty.displayColor))
        } catch (_: Exception) {}

        val given = puzzle.puzzle.count { it != '0' }
        b.tvGivenCount.text = "$given given"

        b.root.setOnClickListener { onClick(puzzle) }
    }

    override fun getItemCount() = puzzles.size

    fun updateList(newList: List<SudokuPuzzle>) {
        puzzles = newList
        notifyDataSetChanged()
    }
}
