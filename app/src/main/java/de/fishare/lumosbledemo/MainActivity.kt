package de.fishare.lumosbledemo

import android.app.Activity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import de.fishare.lumosble.CentralManager

class MainActivity : Activity() {
    private val centralMgr by lazy { CentralManager.getInstance(applicationContext) }
    private lateinit var adapter: ListAdapter
    val list = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        centralMgr.checkPermit(this)
        initListView()
    }

    private fun initListView(){
        list.add("a")
        list.add("b")
        list.add("c")

        adapter = ListAdapter()
        adapter.dataSource = object : ListAdapter.AdapterDataSource {
            override fun onBindOfRow(adapter: ListAdapter, vh: RecyclerView.ViewHolder, position: Int) {
                val data = list[position]
                val v = vh as RenderItem
                v.lblName.text = data
                v.lblMac.text = position.toString()
            }

            override fun cellForRow(adapter: ListAdapter, parent: ViewGroup, row: Int): RecyclerView.ViewHolder {
                val view = LayoutInflater.from(applicationContext).inflate(R.layout.cell_device, parent, false)
                return RenderItem(view)
            }

            override fun numberOfRow(adapter: ListAdapter): Int {
                return list.size
            }

        }
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

}
