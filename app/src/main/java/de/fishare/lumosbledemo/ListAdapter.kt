package de.fishare.lumosbledemo

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.fishare.lumosble.print

class ListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var dataSource:ListAdapter.AdapterDataSource? = null

    interface ItemEvent{
        fun onItemClick(view:View, position:Int)
    }
    var listener:ItemEvent? = null

    override fun onCreateViewHolder(parent: ViewGroup, section: Int): RecyclerView.ViewHolder {
        return dataSource?.cellForRow(parent, section)!!
    }

    override fun getItemCount(): Int {
        val sectionCount = dataSource?.numberOfSection() ?: 0
        var sum = 0
        for(i in 0..sectionCount){
           sum += dataSource?.numberOfRowIn(i) ?: 0
        }
        return sum
    }

    override fun onBindViewHolder(vh: RecyclerView.ViewHolder, position: Int) {
        dataSource?.onBindOfRow(vh, position)
    }

    override fun getItemViewType(position: Int): Int {
        val totalSection= dataSource?.numberOfSection() ?: 0
        print("LIST", "get ViewType is ${position % totalSection}")
        return if(position < totalSection) 0
        else 1
    }

    fun reload(){
        notifyDataSetChanged()
    }

    interface AdapterDataSource {
        fun onBindOfRow(vh: RecyclerView.ViewHolder, position: Int)
        fun cellForRow(parent: ViewGroup, section:Int): RecyclerView.ViewHolder
        fun numberOfSection():Int
        fun numberOfRowIn(section:Int): Int
    }
}