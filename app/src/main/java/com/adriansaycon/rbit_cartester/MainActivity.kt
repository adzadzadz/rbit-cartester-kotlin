package com.adriansaycon.rbit_cartester

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        testAdz("Is there something wrong?")
        setContentView(R.layout.activity_main)
        val myToolbar = findViewById<Toolbar>(R.id.mainToolbar)
        setSupportActionBar(myToolbar)

        val drawer = findViewById<DrawerLayout>(R.id.mainDrawer)
        drawer.openDrawer(GravityCompat.START)
    }

    fun onMenuItemClick(item: MenuItem): Boolean {
        // Handle item selection
        testAdz(item.itemId.toString())
        return true
//        return when (item.itemId) {
//            R.id.new_game -> {
//                newGame()
//                true
//            }
//            R.id.help -> {
//                showHelp()
//                true
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
    }

    fun testAdz(msg: String) {
        Log.d("adz", msg)
    }
}
