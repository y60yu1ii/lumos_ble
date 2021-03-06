package de.fishare.lumosbledemo

import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.fishare.lumosble.*
import de.fishare.lumosbledemo.demos.*
import de.fishare.lumosbledemo.levise.BuddyObj

class MainActivity : AppCompatActivity() {
    private val centralMgr by lazy { CentralManagerBuilder(listOf("1802")).build(this) }
    private lateinit var adapter: ListAdapter
    var avails = mutableListOf<AvailObj>()
    var peris = mutableListOf<PeriObj>()
    lateinit var recyclerView : RecyclerView
    lateinit var swipeLayOut : SwipeRefreshLayout
    val TAG = "MainActivity"
    val AVAIL = 0
    val PERI  = 1
    private var isRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        addBroadcastReceiver()
        setUpCentralManager()
        initListView()
        onRefresh()

    }

    override fun onDestroy() {
        super.onDestroy()
        print(TAG, "unregistered")
        if(isRegistered){unregisterReceiver(receiver)}
    }

    private fun onRefresh(){
        avails = centralMgr.avails
        peris  = centralMgr.peris

//        print(TAG, "onRefresh avail size is ${avails.size}")
//        print(TAG, "onRefresh peris size is ${peris.size}")
        avails.forEach { it.listener = availHandler }
        peris.forEach  { it.listener = periHandler }
        runOnUiThread {
            adapter.reload()
            swipeLayOut.isRefreshing = false
        }
    }

    private fun setUpCentralManager(){
        centralMgr.event = centralEvents
        centralMgr.setting = centralSetting
        centralMgr.loadHistory()
        centralMgr.checkPermit(applicationContext)
    }

    private val centralSetting = object :CentralManager.Setting{

        override fun getCustomAvl(device: BluetoothDevice): AvailObj {
            return BcastAvl(device)
        }

        override fun getCustomObj(mac: String, name:String): PeriObj {
            print(TAG, "GET Custom obj with name is $name")
            return BuddyObj(mac)
        }
    }

    private val centralEvents = object : CentralManager.EventListener{
        override fun didDiscover(availObj: AvailObj) {
            availObj.listener = availHandler
            runOnUiThread { adapter.reload() }
        }
    }

    //   Data Update point
    private val availHandler = object: AvailObj.Listener{
        override fun onRSSIChanged(rssi: Int, availObj: AvailObj) {
            val idx = getAvailIdx(availObj.mac) ?: return
            val vh = getRenderItem(ListAdapter.IndexPath(AVAIL, idx))
            vh?.lblRSSI?.post { vh.lblRSSI.text = rssi.toString() }
        }

        override fun onUpdated(label: String, value: Any, availObj: AvailObj) {
            val idx = getAvailIdx(availObj.mac) ?: return
            val vh = getRenderItem(ListAdapter.IndexPath(AVAIL, idx))
            vh?.lblEvent?.post {
                if(label == "raw"){
                    vh.lblEvent.text = value.toString()
                }
            }
        }
    }

    private val periHandler = object : PeriObjListener{
        override fun onRSSIChanged(rssi: Int, periObj: PeriObj) {
            val idx = getPeripheralIdx(periObj.mac) ?: return
            val vh = getRenderItem(ListAdapter.IndexPath(PERI, idx))
            vh?.lblRSSI?.post { vh.lblRSSI.text = periObj.rssi.toString() }
        }

        override fun onUpdated(label: String, value: Any, periObj: PeriObj) {
            val idx = getPeripheralIdx(periObj.mac) ?: return
            val vh = getRenderItem(ListAdapter.IndexPath(PERI, idx))
            vh?.lblEvent?.post {
                if(value is ByteArray){
                    vh.lblEvent.text = "[NOTIFY] $label [${value.hex4Human()}]"
                }
            }
        }
    }

    private fun getAvailIdx(mac:String):Int?{
        val idx = avails.indexOfFirst { it.mac == mac }
        return if(idx < avails.size && idx >= 0) idx else null
    }

    private fun getPeripheralIdx(mac:String):Int?{
        val idx = peris.indexOfFirst { it.mac == mac }
        return if(idx < peris.size && idx >= 0) idx else null
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
                                   vh?.btnConnect?.post { vh.btnConnect.text = "removing" }
                                   centralMgr.remove(peris[indexPath.row].mac)
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

        swipeLayOut = findViewById(R.id.swipeRefreshLayout)
        swipeLayOut.setOnRefreshListener{
            centralMgr.clearAvl()
        }
    }

    private fun setItemViewContent(v:RenderItem, indexPath: ListAdapter.IndexPath){
        if(indexPath.section == 0 && indexPath.row < avails.size){
            //avail
            val avl = avails[indexPath.row]
            v.lblName.text = avl.name
            v.lblData.text = avl.device.address
            v.lblMac.text =  avl.mac
            v.lblEvent.text =  ""
            v.lblData.text =  ""
            v.btnConnect.text = "Connect"
            v.lblRSSI.text = avl.rssi.toString()
            v.btnConnect.setOnClickListener{ adapter.listener?.onItemClick(it, indexPath) }

        }else if(indexPath.section == 1 && indexPath.row < peris.size){
            //peri
            val peri = peris[indexPath.row]
            v.lblName.text = peri.name
            v.lblMac.text =  peri.mac
            v.lblEvent.text =  ""
            v.lblData.text =  ""
            if(peri.isConnected){
                v.btnConnect.text = "Disconnect"
            }else{
                v.btnConnect.text = "Remove"
            }
            v.lblRSSI.text = peri.rssi.toString()
            v.btnConnect.setOnClickListener{ adapter.listener?.onItemClick(it, indexPath) }
        }
    }

    /**
     *  Broadcast relative
     *
     **/
    private val receiver = object :BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action){
                Event.CONNECTION->{
                    val mac = intent.getStringExtra("mac") ?: ""
                    val isConnected = intent.getBooleanExtra("connected", false)
                    print(TAG, "$mac is ${if(isConnected) "CONNECT" else "DISCONNECT" }")
                    alert(mac, isConnected)
                    onRefresh()
                }
                Event.REFRESH->{ onRefresh() }
            }
        }
    }

    private fun addBroadcastReceiver(){
        val filter = IntentFilter().apply {
            addAction(Event.CONNECTION)
            addAction(Event.REFRESH)
        }
        registerReceiver(receiver, filter)
        isRegistered = true
    }

    private fun alert(key:String, isConnect:Boolean){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Connection")
        builder.setMessage("$key is ${if(isConnect)"connected" else "dropped"}.")
        builder.setNegativeButton("OK"){ _,_-> print("cancel") }
        val dialog = builder.create()
        runOnUiThread {
            dialog.show()
        }
    }

}
