package com.exemple.scannertableau.ui

import android.Manifest
import android.content.*
import android.net.wifi.*
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.*
import com.exemple.scannertableau.R
import com.exemple.scannertableau.data.Storage

class DetectionFragment : Fragment() {
  private lateinit var wifi: WifiManager
  private lateinit var rx: BroadcastReceiver
  private lateinit var adapter: WifiAdapter

  override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) = i.inflate(R.layout.fragment_detect, c, false)

  override fun onViewCreated(v: View, s: Bundle?) {
    super.onViewCreated(v, s)
    wifi = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val rv = v.findViewById<RecyclerView>(R.id.recyclerView)
    rv.layoutManager = LinearLayoutManager(requireContext())
    adapter = WifiAdapter { sr ->
      Storage.upsertBySSID(requireContext(), sr.SSID) { }
      Toast.makeText(requireContext(), "Ajouté: " + sr.SSID, Toast.LENGTH_SHORT).show()
    }
    rv.adapter = adapter

    rx = object: BroadcastReceiver() {
      override fun onReceive(ctx: Context?, intent: Intent?) {
        val prefix = Storage.getPrefix(requireContext())
        val res = wifi.scanResults.filter { it.SSID.startsWith(prefix, true) }.sortedByDescending { it.level }
        v.findViewById<TextView>(R.id.tvCount).text = "Modules trouvés: " + res.size
        adapter.submitList(res)
        res.forEach { sr -> Storage.upsertBySSID(requireContext(), sr.SSID) { } }
      }
    }
    requireContext().registerReceiver(rx, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))

    registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) wifi.startScan() }.launch(Manifest.permission.ACCESS_FINE_LOCATION)
  }

  override fun onDestroyView() { super.onDestroyView(); requireContext().unregisterReceiver(rx) }
}

class WifiAdapter(private val onClick: (ScanResult) -> Unit) : RecyclerView.Adapter<WifiAdapter.VH>() {
  private var data = listOf<ScanResult>()
  fun submitList(l: List<ScanResult>) { data = l; notifyDataSetChanged() }
  class VH(v: View) : RecyclerView.ViewHolder(v) { val n: TextView = v.findViewById(R.id.tvName); val i: TextView = v.findViewById(R.id.tvInfo) }
  override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_wifi, p, false))
  override fun onBindViewHolder(h: VH, p: Int) { val s = data[p]; h.n.text = s.SSID; h.i.text = "RSSI: " + s.level + " dBm • " + s.BSSID; h.itemView.setOnClickListener { onClick(s) } }
  override fun getItemCount() = data.size
}
