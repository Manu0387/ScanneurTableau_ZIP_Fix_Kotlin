package com.exemple.scannertableau.ui

import android.content.ContentValues
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.*
import org.dhatim.fastexcel.Workbook
import java.io.OutputStream
import com.exemple.scannertableau.R
import com.exemple.scannertableau.data.*

class BoardFragment : Fragment() {
  private lateinit var adapter: BoardAdapter
  override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) = i.inflate(R.layout.fragment_board, c, false)
  override fun onViewCreated(v: View, s: Bundle?) {
    super.onViewCreated(v, s)
    val rv = v.findViewById<RecyclerView>(R.id.rvBoard)
    rv.layoutManager = LinearLayoutManager(requireContext())
    adapter = BoardAdapter { ssid, breaker -> Storage.upsertBySSID(requireContext(), ssid) { it.disjoncteur = breaker }; Toast.makeText(requireContext(), "Enregistré", Toast.LENGTH_SHORT).show() }
    rv.adapter = adapter
    v.findViewById<Button>(R.id.btnExport).setOnClickListener { exportXlsx(Storage.loadModules(requireContext())) }
  }
  override fun onResume() { super.onResume(); adapter.submit(Storage.loadModules(requireContext())) }
  private fun exportXlsx(list: List<ModuleRecord>) {
    val resolver = requireContext().contentResolver
    val cv = ContentValues().apply {
      put(MediaStore.Downloads.DISPLAY_NAME, "ScannerTableauPro.xlsx")
      put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
      put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv) ?: run { Toast.makeText(requireContext(), "Erreur export", Toast.LENGTH_SHORT).show(); return }
    resolver.openOutputStream(uri)?.use { out: OutputStream ->
      val workbook = Workbook(out, "ScannerTableauPro", "1.0")
      val sheet = workbook.newWorksheet("Modules")
      listOf("Disjoncteur","Nom du module","SSID","Statut").forEachIndexed { i,t -> sheet.value(0,i,t) }
      val now = System.currentTimeMillis()
      val sorted = list.sortedWith(compareBy({ it.disjoncteur ?: "ZZZ" }, { it.customName ?: it.ssid }))
      sorted.forEachIndexed { idx, m ->
        val r = idx+1
        sheet.value(r,0, m.disjoncteur ?: "")
        sheet.value(r,1, m.customName ?: "")
        sheet.value(r,2, m.ssid)
        val present = now - m.lastSeen <= 5*60*1000
        sheet.value(r,3, if (present) "Oui" else "Non")
      }
      workbook.finish()
    }
    Toast.makeText(requireContext(), "Exporté dans Téléchargements", Toast.LENGTH_LONG).show()
  }
}
class BoardAdapter(private val onSave:(String,String)->Unit) : RecyclerView.Adapter<BoardAdapter.VH>() {
  private var data = mutableListOf<ModuleRecord>()
  fun submit(l: List<ModuleRecord>) { data = l.toMutableList(); notifyDataSetChanged() }
  class VH(v: View) : RecyclerView.ViewHolder(v) { val ssid: TextView = v.findViewById(R.id.tvSSID); val et: EditText = v.findViewById(R.id.etBreaker); val btn: Button = v.findViewById(R.id.btnSave) }
  override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_board, p, false))
  override fun onBindViewHolder(h: VH, i: Int) { val r = data[i]; h.ssid.text = r.customName?.takeIf { it.isNotBlank() } ?: r.ssid; h.et.setText(r.disjoncteur ?: ""); h.btn.setOnClickListener { onSave(r.ssid, h.et.text.toString().trim()) } }
  override fun getItemCount() = data.size
}
