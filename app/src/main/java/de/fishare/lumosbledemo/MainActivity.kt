package de.fishare.lumosbledemo

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.fishare.lumosble.AvailObj
import de.fishare.lumosble.CentralManager
import de.fishare.lumosble.PeriObj
import de.fishare.lumosble.print
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private val centralMgr by lazy { CentralManager.getInstance(applicationContext) }
    private lateinit var adapter: ListAdapter
//    #1 simple string object for list
//    val list = mutableListOf<String>()
    var avails = mutableListOf<AvailObj>()
    var peris = mutableListOf<PeriObj>()
    lateinit var recyclerView : RecyclerView
    val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        centralMgr.checkPermit(this)
        centralMgr.event = centralEvents
        initListView()
        onRefresh()
        Handler().postDelayed({
        }, 2000)
    }

    private fun onRefresh(){
        avails = centralMgr.avails
        peris  = centralMgr.peris

        print(TAG, "onRefresh size is ${avails.size}")
        avails.forEach { it.listener = availHandler }
    }

    private val centralEvents = object : CentralManager.EventListener{
        override fun onRefresh() {
            onRefresh()
        }

        override fun didDiscover(availObj: AvailObj) {
            availObj.listener = availHandler
            runOnUiThread { adapter.reload() }
        }

        override fun didConnect() {

            runOnUiThread { adapter.reload() }
        }

        override fun didDisconnect() {
        }
    }

    //   Data Update point
    private val availHandler = object: AvailObj.Listener{
        override fun onRSSIChanged(rssi: Int, mac: String) {
            val idx = avails.indexOfFirst { it.mac == mac }
            if(idx < avails.size && idx >= 0){
                val vh = getRenderItem(idx)
                vh?.lblRSSI?.post { vh.lblRSSI.text = rssi.toString() }
            }
        }
    }
//     #2 Lets update the view content after a while
//    fun editView(){
//        list.add("d")
//        list.add("e")
//        adapter.reload()
//
//        val vh = getRenderItem(0)
//        vh?.lblName?.post { vh.lblName.text = "NAMEEEEEEEEEEEEEEEEEEEE 1" }
//        vh?.lblData?.post { vh.lblData.text = "I found you !" }
//
//    }

//    fun setItemViewContent(v:RenderItem, position: Int){
//        val data = list[position]
//        val onClick = View.OnClickListener { adapter.listener?.onItemClick(it, position) }
//
//        v.lblName.text = data.toString()
//        v.lblMac.text = position.toString()
//        v.btnConnect.text = "CONNECT"
//        v.btnTest.text = "Test"
//        v.btnConnect.setOnClickListener(onClick)
//        v.btnTest.setOnClickListener(onClick)
//    }

    fun setItemViewContent(v:RenderItem, position: Int){
//        val data = avails[position]
//        val onClick = View.OnClickListener { adapter.listener?.onItemClick(it, position) }
//
//        v.lblName.text = data.name
//        v.lblMac.text  =  data.mac
//        v.btnConnect.text = "Connect"
//        v.lblRSSI.text = data.rssi.toString()
//        v.btnTest.visibility = View.GONE
//        v.btnConnect.setOnClickListener(onClick)
    }

    private fun getRenderItem(position:Int):RenderItem?{
        val view = recyclerView.findViewHolderForLayoutPosition(position)?.itemView
        return if(view != null){ RenderItem(view) } else null
    }

    private fun initListView(){
        adapter = ListAdapter()
        adapter.dataSource = object : ListAdapter.AdapterDataSource {
            override fun numberOfRowIn(section: Int): Int {
                return if(section == 0){
                    5
                }else{
                    4
                }
            }

            override fun numberOfSection(): Int {
                return 2
            }

            override fun onBindOfRow(vh: RecyclerView.ViewHolder, position: Int) {
                setItemViewContent(vh as RenderItem, position)
            }

            override fun cellForRow(parent: ViewGroup, section: Int): RecyclerView.ViewHolder {
                val view = LayoutInflater.from(applicationContext).inflate(R.layout.cell_device, parent, false)
                val vh = RenderItem(view)
                vh.lblData.text = "section is $section"
//                return RenderItem(view)
                return vh
            }

        }

        adapter.listener = object : ListAdapter.ItemEvent{
            override fun onItemClick(view: View, position: Int) {
                when(view.id){
                   R.id.btnConnect -> {
                       centralMgr.connect(avails[position]?.mac)
                       val vh = getRenderItem(position)
                       vh?.btnConnect?.post { vh.btnConnect.text = "connecting" }
                   }
                   R.id.btnTest -> {
//                       print(TAG, "item is click at $position view is test")
                   }
                }
            }
        }

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
}
