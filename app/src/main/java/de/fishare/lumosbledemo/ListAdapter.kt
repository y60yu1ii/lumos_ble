package de.fishare.lumosbledemo

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class ListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var dataSource:ListAdapter.AdapterDataSource? = null

    override fun onCreateViewHolder(parent: ViewGroup, row: Int): RecyclerView.ViewHolder {
        return dataSource?.cellForRow(this, parent, row)!!
    }

    override fun getItemCount(): Int {
        return dataSource?.numberOfRow(this)  ?: 0
    }

    override fun onBindViewHolder(vh: RecyclerView.ViewHolder, position: Int) {
        dataSource?.onBindOfRow(this, vh, position)
    }

    fun reload(){
        notifyDataSetChanged()
    }

    interface AdapterDataSource {
        fun onBindOfRow(adapter: ListAdapter, vh: RecyclerView.ViewHolder, position: Int)
        fun cellForRow(adapter: ListAdapter, parent: ViewGroup, row:Int): RecyclerView.ViewHolder
        fun numberOfRow(adapter: ListAdapter): Int
    }
}