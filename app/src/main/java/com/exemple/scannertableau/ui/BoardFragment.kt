package com.exemple.scannertableau.ui

import android.content.ContentValues
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.dhatim.fastexcel.Workbook
import java.io.OutputStream
import com.exemple.scannertableau.R
import com.exemple.scannertableau.data.ModuleRecord
import com.exemple.scannertableau.data.Storage

class BoardFragment : Fragment() {

    private lateinit var adapter: BoardAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_board, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rv = view.findViewById<RecyclerView>(R.id.rvBoard)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = BoardAdapter { ssid, breaker ->
            Storage.upsertBySSID(requireContext(), ssid) { it.disjoncteur = breaker }
            Toast.makeText(requireContext(), "Enregistré", Toast.LENGTH_SHORT).show()
        }
        rv.adapter = adapter

        view.findViewById<Button>(R.id.btnExport).setOnClickListener {
            exportXlsx(Storage.loadModules(requireContext()))
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.submit(Storage.loadModules(requireContext()))
    }

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

        resolver.openOutputStream(uri)?.use { output: OutputStream ->
            val workbook = Workbook(output, "ScanneurTableau", "1.0")
            val sheet = workbook.newWorksheet("Modules")

            val headers = listOf("N°", "SSID", "Nom", "Disjoncteur")
            headers.forEachIndexed { i, t -> sheet.value(0, i, t) }

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
}

/** Adaptateur RecyclerView **/
class BoardAdapter(private val onSave: (String, String) -> Unit) :
    RecyclerView.Adapter<BoardAdapter.VH>() {

    private var data = mutableListOf<ModuleRecord>()

    fun submit(list: List<ModuleRecord>) {
        data = list.toMutableList()
        notifyDataSetChanged()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val ssid: TextView = view.findViewById(R.id.tvSSID)
        val et: EditText = view.findViewById(R.id.etBreaker)
        val btn: Button = view.findViewById(R.id.btnSave)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_board, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val record = data[position]
        holder.ssid.text = record.customName?.takeIf { it.isNotBlank() } ?: record.ssid
        holder.et.setText(record.disjoncteur ?: "")
        holder.btn.setOnClickListener {
            onSave(record.ssid, holder.et.text.toString().trim())
        }
    }

    override fun getItemCount() = data.size
}
