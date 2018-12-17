package de.fishare.lumosbledemo

import android.support.v7.widget.RecyclerView
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
        val secCap = dataSource?.numberOfSection() ?: 0
        var sum = 0
        for(i in 0 until secCap){
           sum += dataSource?.numberOfRowIn(i) ?: 0
        }
        return sum
    }

    override fun onBindViewHolder(vh: RecyclerView.ViewHolder, position: Int) {
        dataSource?.onBindOfRow(vh, getIndexPath(position))
    }

    override fun getItemViewType(position: Int): Int {
        var pos = position
        val secCap = dataSource?.numberOfSection() ?: 0
        var viewType = 0
        for(section in 0 until secCap){
            val row = dataSource?.numberOfRowIn(section) ?:0
            if(pos < row){
                viewType = section
                break
            } else{
                pos -= row
                continue
            }
        }
        return viewType
    }

    private fun getIndexPath(position: Int):IndexPath{
        val secCap = dataSource?.numberOfSection() ?: 0
        val indexPath = getRecursiveIdxPath(position, IndexPath(0, position), secCap)
        print("LIST", "index path is $indexPath")
        return indexPath
    }

    private fun getRecursiveIdxPath(pos:Int, indexPath: IndexPath, cap:Int):IndexPath{
        val section = indexPath.section
        val numberOfRow = dataSource?.numberOfRowIn(section) ?:0
        return if(pos < numberOfRow || section >= cap){
            indexPath.row = pos
            indexPath
        }else{
            indexPath.section += 1
            getRecursiveIdxPath(pos - numberOfRow, indexPath, cap)
        }
    }

    fun reload(){
        notifyDataSetChanged()
    }

    interface AdapterDataSource {
        fun onBindOfRow(vh: RecyclerView.ViewHolder, indexPath: IndexPath)
        fun cellForRow(parent: ViewGroup, section:Int): RecyclerView.ViewHolder
        fun numberOfSection():Int
        fun numberOfRowIn(section:Int): Int
    }

    data class IndexPath(var section:Int = -1, var row:Int = -1)
}