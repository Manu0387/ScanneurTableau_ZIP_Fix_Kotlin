package com.exemple.scannertableau.ui

import android.Manifest
import android.app.AlertDialog
import android.content.*
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
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

    private lateinit var progress: ProgressBar
    private lateinit var tvCount: TextView
    private lateinit var breakerIds: List<Int>
    private var currentScanBreaker: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_detect, container, false)

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)
        wifi = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        progress = v.findViewById(R.id.progress)
        tvCount = v.findViewById(R.id.tvCount)

        breakerIds = (1..39).map { i -> resources.getIdentifier("dj"+i, "id", requireContext().packageName) }

        val rv = v.findViewById<RecyclerView>(R.id.recyclerView)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = WifiAdapter { sr ->
            Storage.upsertBySSID(requireContext(), sr.SSID) { }
            Toast.makeText(requireContext(), "Ajouté: " + sr.SSID, Toast.LENGTH_SHORT).show()
        }
        rv.adapter = adapter

        v.findViewById<Button>(R.id.btnScan).setOnClickListener {
            currentScanBreaker = null
            startWifiScan()
        }

        rx = object: BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val prefix = Storage.getPrefix(requireContext())
                val res = wifi.scanResults
                    .filter { it.SSID.startsWith(prefix, true) }
                    .sortedByDescending { it.level }

                val now = System.currentTimeMillis()
                res.forEach { sr ->
                    Storage.upsertBySSID(requireContext(), sr.SSID) {
                        it.lastSeen = now
                        currentScanBreaker?.let { dj -> it.disjoncteur = dj }
                    }
                }

                tvCount.text = "Modules trouvés: ${res.size}"
                adapter.submitList(res)
                updateAllBreakersUI()
                progress.visibility = View.GONE
                currentScanBreaker = null
            }
        }
        requireContext().registerReceiver(rx, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))

        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        breakerIds.forEachIndexed { index, id ->
            val btn = v.findViewById<Button>(id)
            val label = "DJ${index+1}"

            btn.setOnClickListener {
                currentScanBreaker = label
                progress.visibility = View.VISIBLE
                startWifiScan()
            }

            btn.setOnLongClickListener {
                showEditDialog(label)
                true
            }
        }

        updateAllBreakersUI()
    }

    private fun startWifiScan() { progress.visibility = View.VISIBLE; wifi.startScan() }

    private fun showEditDialog(disj: String) {
        val mods = Storage.loadModules(requireContext()).filter { it.disjoncteur == disj }
        val defaultText = mods.firstOrNull()?.customName ?: mods.firstOrNull()?.ssid ?: ""
        val input = EditText(requireContext()); input.hint = "Nom du module (ex: Prise Salon)"; input.setText(defaultText)
        AlertDialog.Builder(requireContext())
            .setTitle("Modifier les modules de $disj")
            .setView(input)
            .setPositiveButton("Enregistrer") { _, _ ->
                val name = input.text.toString().trim()
                if (mods.isNotEmpty()) {
                    mods.forEach { m -> Storage.upsertBySSID(requireContext(), m.ssid) { it.customName = name } }
                } else if (name.isNotEmpty()) {
                    Storage.upsertBySSID(requireContext(), name) { it.customName = name; it.disjoncteur = disj; it.lastSeen = 0L }
                }
                updateAllBreakersUI()
                Toast.makeText(requireContext(), "Enregistré", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun updateAllBreakersUI() {
        val mods = Storage.loadModules(requireContext())
        breakerIds.forEachIndexed { index, id ->
            val btn = view?.findViewById<Button>(id) ?: return@forEachIndexed
            val label = "DJ${index+1}"
            val linked = mods.filter { it.disjoncteur == label }
            val names = linked.map { it.customName?.takeIf { n -> n.isNotBlank() } ?: it.ssid }
            btn.text = if (names.isEmpty()) label else "$label\n" + names.joinToString(", ")
            val now = System.currentTimeMillis()
            val present = linked.any { now - it.lastSeen <= 5*60*1000 }
            val color = if (present) R.color.st_ok else R.color.st_ko
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(requireContext().getColor(color))
        }
    }

    override fun onDestroyView() { super.onDestroyView(); requireContext().unregisterReceiver(rx) }
}

class WifiAdapter(private val onClick: (ScanResult) -> Unit) : RecyclerView.Adapter<WifiAdapter.VH>() {
    private var data = listOf<ScanResult>()
    fun submitList(l: List<ScanResult>) { data = l; notifyDataSetChanged() }
    class VH(v: View) : RecyclerView.ViewHolder(v) { val n: TextView = v.findViewById(R.id.tvName); val i: TextView = v.findViewById(R.id.tvInfo) }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_wifi, p, false))
    override fun onBindViewHolder(h: VH, p: Int) { val s = data[p]; h.n.text = s.SSID; h.i.text = "RSSI: ${s.level} dBm • ${s.BSSID}"; h.itemView.setOnClickListener { onClick(s) } }
    override fun getItemCount() = data.size
}
