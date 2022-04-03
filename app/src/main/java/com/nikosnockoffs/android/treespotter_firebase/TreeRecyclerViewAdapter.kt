package com.nikosnockoffs.android.treespotter_firebase

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// job is to connect data with things that can be seen in recycler view
// treeheartlistener doesn't return anything so we put -> unit; this is an argument that is a function
class TreeRecyclerViewAdapter(var trees: List<Tree>, val treeHeartListener: (Tree, Boolean) -> Unit ): RecyclerView.Adapter<TreeRecyclerViewAdapter.ViewHolder>() {


    // can you use the Tree object's toString method to get this data for display?
    inner class ViewHolder(private val view: View): RecyclerView.ViewHolder(view) {
        fun bind(tree: Tree) {
            view.findViewById<TextView>(R.id.tree_name).text = tree.name
            view.findViewById<TextView>(R.id.date_spotted).text = "${tree.dateSpotted}"
            // this is where we would find a piece of the tree fragment list item and set up a listener for it
            view.findViewById<CheckBox>(R.id.heart_check).apply { // scope function apply keeps all code related to checkbox together in one block
                isChecked = tree.favorite ?: false
                setOnCheckedChangeListener { checkbox, isChecked ->
                    treeHeartListener(tree, isChecked)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_tree_list_item, parent, false)
        return ViewHolder(view)
    }

    // getting data for item in the list
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tree = trees[position]
        holder.bind(tree)
    }

    override fun getItemCount(): Int {
        return trees.size
    }
}