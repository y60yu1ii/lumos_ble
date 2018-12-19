package de.fishare.lumosbledemo

import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.cell_device.view.*

class RenderItem(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val lblName  = itemView.lblName!!
    val lblMac   = itemView.lblMac!!
    val lblRSSI  = itemView.lblRSSI!!
    val lblEvent = itemView.lblEvent!!
    val lblData  = itemView.lblData!!
    val btnConnect  = itemView.btnConnect!!
}