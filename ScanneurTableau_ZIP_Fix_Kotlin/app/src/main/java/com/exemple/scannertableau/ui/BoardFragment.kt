package com.exemple.scannertableau.ui

import android.content.ContentValues
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
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
    if (list.isEmpty()) { Toast.makeText(requireContext(), "Aucun module", Toast.LENGTH_SHORT).show(); return }
    val wb = XSSFWorkbook(); val sh = wb.createSheet("Tableau Modules")
    for (i in 0 until 13) sh.setColumnWidth(i, 50 * 256) // ≈ 17,5 mm
    val head = sh.createRow(0); val headers = listOf("N°","SSID","Nom","Disjoncteur")
    headers.forEachIndexed { i, t -> head.createCell(i).setCellValue(t) }
    list.forEachIndexed { idx, m ->
      val r = sh.createRow(idx+1)
      r.createCell(0).setCellValue((idx+1).toDouble())
      r.createCell(1).setCellValue(m.ssid)
      r.createCell(2).setCellValue(m.customName ?: "")
      r.createCell(3).setCellValue(m.disjoncteur ?: "")
    }
    val res = requireContext().contentResolver
    val cv = ContentValues().apply {
      put(MediaStore.Downloads.DISPLAY_NAME, "TableauModules.xlsx")
      put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
      put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }
    val uri = res.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
    if (uri != null) {
      res.openOutputStream(uri)?.use { out: OutputStream -> wb.write(out) }
      wb.close()
      Toast.makeText(requireContext(), "Exporté dans Téléchargements", Toast.LENGTH_LONG).show()
    } else Toast.makeText(requireContext(), "Erreur export", Toast.LENGTH_SHORT).show()
  }
}

class BoardAdapter(private val onSave: (String,String)->Unit) : RecyclerView.Adapter<BoardAdapter.VH>() {
  private var data = mutableListOf<ModuleRecord>()
  fun submit(l: List<ModuleRecord>) { data = l.toMutableList(); notifyDataSetChanged() }
  class VH(v: View) : RecyclerView.ViewHolder(v) { val ssid: TextView = v.findViewById(R.id.tvSSID); val et: EditText = v.findViewById(R.id.etBreaker); val btn: Button = v.findViewById(R.id.btnSave) }
  override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_board, p, false))
  override fun onBindViewHolder(h: VH, i: Int) { val r = data[i]; h.ssid.text = r.customName?.takeIf { !it.isNullOrBlank() } ?: r.ssid; h.et.setText(r.disjoncteur ?: ""); h.btn.setOnClickListener { onSave(r.ssid, h.et.text.toString().trim()) } }
  override fun getItemCount() = data.size
}
