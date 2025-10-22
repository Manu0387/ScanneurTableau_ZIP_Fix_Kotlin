package com.exemple.scannertableau.ui

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.*
import com.exemple.scannertableau.R
import com.exemple.scannertableau.data.*

class SettingsFragment : Fragment() {
  override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) = i.inflate(R.layout.fragment_settings, c, false)
  override fun onViewCreated(v: View, s: Bundle?) {
    super.onViewCreated(v, s)
    val et = v.findViewById<EditText>(R.id.etPrefix); et.setText(Storage.getPrefix(requireContext()))
    v.findViewById<Button>(R.id.btnSavePrefix).setOnClickListener { Storage.setPrefix(requireContext(), et.text.toString().trim()); Toast.makeText(requireContext(), "Préfixe enregistré", Toast.LENGTH_SHORT).show() }
    val rv = v.findViewById<RecyclerView>(R.id.rvNames); rv.layoutManager = LinearLayoutManager(requireContext())
    rv.adapter = NamesAdapter { ssid, name -> Storage.upsertBySSID(requireContext(), ssid) { it.customName = name }; Toast.makeText(requireContext(), "Nom enregistré", Toast.LENGTH_SHORT).show() }.apply { submit(Storage.loadModules(requireContext())) }
  }
}
class NamesAdapter(private val onSave:(String,String)->Unit) : RecyclerView.Adapter<NamesAdapter.VH>() {
  private var data = mutableListOf<ModuleRecord>()
  fun submit(l: List<ModuleRecord>) { data = l.toMutableList(); notifyDataSetChanged() }
  class VH(v: View): RecyclerView.ViewHolder(v) { val ssid: TextView = v.findViewById(R.id.tvSSIDName); val et: EditText = v.findViewById(R.id.etCustom); val btn: Button = v.findViewById(R.id.btnSaveName) }
  override fun onCreateViewHolder(p: ViewGroup, t: Int)= VH(LayoutInflater.from(p.context).inflate(R.layout.item_name,p,false))
  override fun onBindViewHolder(h: VH, i: Int) { val r = data[i]; h.ssid.text = r.ssid; h.et.setText(r.customName ?: ""); h.btn.setOnClickListener { onSave(r.ssid, h.et.text.toString().trim()) } }
  override fun getItemCount() = data.size
}
