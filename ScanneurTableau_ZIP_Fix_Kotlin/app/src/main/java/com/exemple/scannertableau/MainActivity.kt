package com.exemple.scannertableau

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.exemple.scannertableau.ui.DetectionFragment
import com.exemple.scannertableau.ui.BoardFragment
import com.exemple.scannertableau.ui.SettingsFragment

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    val bottom = findViewById<BottomNavigationView>(R.id.bottom_navigation)
    bottom.setOnItemSelectedListener { item ->
      when (item.itemId) {
        R.id.nav_detect -> swap(DetectionFragment())
        R.id.nav_board -> swap(BoardFragment())
        R.id.nav_settings -> swap(SettingsFragment())
      }
      true
    }
    if (savedInstanceState == null) bottom.selectedItemId = R.id.nav_detect
  }
  private fun swap(f: Fragment) {
    supportFragmentManager.beginTransaction().replace(R.id.fragment_container, f).commit()
  }
}
