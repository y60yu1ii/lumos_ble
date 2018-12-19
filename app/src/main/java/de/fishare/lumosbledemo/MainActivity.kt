package de.fishare.lumosbledemo

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.fishare.lumosble.*
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private val centralMgr by lazy { CentralManager.getInstance(applicationContext) }
    private lateinit var adapter: ListAdapter
    var avails = mutableListOf<AvailObj>()
    var peris = mutableListOf<PeriObj>()
    lateinit var recyclerView : RecyclerView
    val TAG = "MainActivity"
    val AVAIL = 0
    val PERI  = 1
    private var isRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        centralMgr.checkPermit(this)
        centralMgr.event = centralEvents
        initListView()
        onRefresh()
        addBroadcastReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        print(TAG, "unregistered")
        if(isRegistered){unregisterReceiver(receiver)}
    }

    private fun onRefresh(){
        avails = centralMgr.avails
        peris  = centralMgr.peris

        print(TAG, "onRefresh avail size is ${avails.size}")
        print(TAG, "onRefresh peris size is ${peris.size}")
        avails.forEach { it.listener = availHandler }
        peris.forEach  { it.listener = periHandler }
        runOnUiThread { adapter.reload() }
    }

    private val centralEvents = object : CentralManager.EventListener{
        override fun onRefresh() {
            onRefresh()
        }

        override fun didDiscover(availObj: AvailObj) {
            availObj.listener = availHandler
            runOnUiThread { adapter.reload() }
        }
    }

    //   Data Update point
    private val availHandler = object: AvailObj.Listener{
        override fun onRSSIChanged(rssi: Int, mac: String) {
            val idx = avails.indexOfFirst { it.mac == mac }
            if(idx < avails.size && idx >= 0){
                val vh = getRenderItem(ListAdapter.IndexPath(AVAIL, idx))
                vh?.lblRSSI?.post { vh.lblRSSI.text = rssi.toString() }
            }
        }
    }

    private val periHandler = object : PeriObj.Listener{
        override fun onRSSIChanged(rssi: Int, mac: String) {
            val idx = peris.indexOfFirst { it.mac == mac }
            if(idx < peris.size && idx >= 0){
                val vh = getRenderItem(ListAdapter.IndexPath(PERI, idx))
                vh?.lblRSSI?.post { vh.lblRSSI.text = rssi.toString() }
            }
        }

        override fun onUpdated(label: String, value: Any, periObj: PeriObj) {
            val idx = peris.indexOfFirst { it.mac == periObj.mac }
            if(idx < peris.size && idx >= 0){
                val vh = getRenderItem(ListAdapter.IndexPath(PERI, idx))
                vh?.lblEvent?.post {
                    if(value is ByteArray){
                        vh.lblEvent.text = "[NOTIFY] $label [${value.hex4Human()}]"
                    }
                }
            }
        }
    }

    fun setItemViewContent(v:RenderItem, indexPath: ListAdapter.IndexPath){
        if(indexPath.section == 0 && indexPath.row < avails.size){
            //avail
            val avl = avails[indexPath.row]
            v.lblName.text = avl.name
            v.lblData.text = avl.device.address
            v.lblMac.text =  avl.mac
            v.btnConnect.text = "Connect"
            v.lblRSSI.text = avl.rssi.toString()
            v.btnConnect.setOnClickListener{ adapter.listener?.onItemClick(it, indexPath) }

        }else if(indexPath.section == 1 && indexPath.row < peris.size){
            //peri
            val peri = peris[indexPath.row]
            v.lblName.text = peri.name
            v.lblMac.text =  peri.mac
            if(peri.isConnected){
                v.btnConnect.text = "Disconnect"
            }else{
                v.btnConnect.text = "Remove"
            }
            v.lblRSSI.text = peri.rssi.toString()
            v.btnConnect.setOnClickListener{ adapter.listener?.onItemClick(it, indexPath) }
        }
    }

    private fun getRenderItem(indexPath: ListAdapter.IndexPath):RenderItem?{
        val position = adapter.getPositionOf(indexPath)
        val view = recyclerView.findViewHolderForLayoutPosition(position)?.itemView
        return if(view != null){ RenderItem(view) } else null
    }

    private fun initListView(){
        adapter = ListAdapter()
        adapter.dataSource = object : ListAdapter.AdapterDataSource {
            override fun numberOfRowIn(section: Int): Int {
                return when(section){
                    0 -> avails.count()
                    1 -> peris.count()
                    else-> 0
                }
            }

            override fun numberOfSection(): Int {
                return 2
            }

            override fun onBindOfRow(vh: RecyclerView.ViewHolder, indexPath: ListAdapter.IndexPath) {
                setItemViewContent(vh as RenderItem, indexPath)
            }

            override fun cellForRow(parent: ViewGroup, section: Int): RecyclerView.ViewHolder {
                val view = LayoutInflater.from(applicationContext).inflate(R.layout.cell_device, parent, false)
                return RenderItem(view)
            }
        }

        adapter.listener = object : ListAdapter.ItemEvent{
            override fun onItemClick(view: View, indexPath: ListAdapter.IndexPath) {
                when(view.id){
                   R.id.btnConnect -> {
                       print(TAG, "[EVENT] connect button is click ")
                       val vh = getRenderItem(indexPath)
                       when(indexPath.section){
                           AVAIL ->{
                             if(indexPath.row < avails.size){
                                 vh?.btnConnect?.post { vh.btnConnect.text = "connecting" }
                                 centralMgr.connect(avails[indexPath.row].mac)
                             }
                           }

                           PERI ->{
                               if(indexPath.row < peris.size){
                                   vh?.btnConnect?.post { vh.btnConnect.text = "disconnecting" }
                                   centralMgr.disconnect(peris[indexPath.row].mac)
                               }
                           }
                       }
                   }
                }
            }
        }

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    /**
     *  Broadcast relative
     *
     **/
    private val receiver = object :BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action){
                CONNECTION_EVENT->{
                    val mac = intent.getStringExtra("mac") ?: ""
                    val isConnected = intent.getBooleanExtra("connected", false)
                    print(TAG, "$mac is ${if(isConnected) "CONNECT" else "DISCONNECT" }")
                    onRefresh()
                }
            }
        }
    }

    private fun addBroadcastReceiver(){
        val filter = IntentFilter().apply {
            addAction(CONNECTION_EVENT)
        }
        registerReceiver(receiver, filter)
        isRegistered = true
    }
}
