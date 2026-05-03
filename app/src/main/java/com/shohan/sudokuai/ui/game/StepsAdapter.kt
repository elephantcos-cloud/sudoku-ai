package com.shohan.sudokuai.ui.game

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.shohan.sudokuai.R
import com.shohan.sudokuai.engine.SudokuSolver
import com.shohan.sudokuai.util.ThemeManager

class StepsAdapter(
    private val context: Context,
    private val steps: List<SudokuSolver.SolveStep>
) : RecyclerView.Adapter<StepsAdapter.StepViewHolder>() {

    private var currentHighlight = -1
    private val isDark = ThemeManager.isDarkTheme(context)

    class StepViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStepNum: TextView = view.findViewById(R.id.tvStepNumber)
        val tvTechnique: TextView = view.findViewById(R.id.tvTechnique)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val tvCell: TextView = view.findViewById(R.id.tvCell)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_step, parent, false)
        return StepViewHolder(view)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        val step = steps[position]

        holder.tvStepNum.text = "Step ${position + 1}"
        holder.tvTechnique.text = step.technique
        holder.tvDescription.text = step.description

        if (step.value != 0) {
            holder.tvCell.text = "Placed ${step.value} at (R${step.row + 1},C${step.col + 1})"
            holder.tvCell.visibility = View.VISIBLE
        } else {
            holder.tvCell.text = "Erased at (R${step.row + 1},C${step.col + 1})"
            holder.tvCell.visibility = View.VISIBLE
        }

        // Color by technique
        val techniqueColor = when {
            step.technique.contains("Naked") -> if (isDark) "#90CAF9" else "#1565C0"
            step.technique.contains("Hidden") -> if (isDark) "#A5D6A7" else "#2E7D32"
            step.technique.contains("Backtracking Undo") -> if (isDark) "#EF9A9A" else "#C62828"
            step.technique.contains("Backtracking") -> if (isDark) "#FFCC80" else "#E65100"
            step.technique.contains("AI") -> if (isDark) "#CE93D8" else "#6A1B9A"
            else -> if (isDark) "#FFFFFF" else "#000000"
        }
        holder.tvTechnique.setTextColor(Color.parseColor(techniqueColor))

        // Highlight current step
        if (position == currentHighlight) {
            holder.itemView.setBackgroundColor(
                if (isDark) Color.parseColor("#1A237E") else Color.parseColor("#E3F2FD")
            )
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    override fun getItemCount() = steps.size

    fun highlightStep(position: Int) {
        val old = currentHighlight
        currentHighlight = position
        if (old >= 0) notifyItemChanged(old)
        if (position >= 0) notifyItemChanged(position)
    }
}
