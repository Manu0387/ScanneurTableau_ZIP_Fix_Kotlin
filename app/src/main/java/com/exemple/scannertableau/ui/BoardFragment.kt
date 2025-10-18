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
    if (list.isEmpty()) {
        Toast.makeText(requireContext(), "Aucun module", Toast.LENGTH_SHORT).show()
        return
    }

    val resolver = requireContext().contentResolver
    val cv = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, "TableauModules.xlsx")
        put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }

    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
    if (uri == null) {
        Toast.makeText(requireContext(), "Erreur d’export", Toast.LENGTH_SHORT).show()
        return
    }

    resolver.openOutputStream(uri)?.use { output ->
        // ✅ Nouvelle méthode avec FastExcel
        val workbook = org.dhatim.fastexcel.Workbook(output, "ScanneurTableau", "1.0")
        val sheet = workbook.newWorksheet("Modules")

        // En-têtes
        val headers = listOf("N°", "SSID", "Nom", "Disjoncteur")
        headers.forEachIndexed { i, t -> sheet.value(0, i, t) }

        // Données
        list.forEachIndexed { index, module ->
            sheet.value(index + 1, 0, index + 1)
            sheet.value(index + 1, 1, module.ssid)
            sheet.value(index + 1, 2, module.customName ?: "")
            sheet.value(index + 1, 3, module.disjoncteur ?: "")
        }

        workbook.finish()
    }

    Toast.makeText(requireContext(), "Exporté dans Téléchargements", Toast.LENGTH_LONG).show()
}
)
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
