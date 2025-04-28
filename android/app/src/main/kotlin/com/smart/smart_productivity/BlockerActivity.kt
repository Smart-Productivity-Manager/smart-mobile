package com.smart.smart_productivity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView

class BlockerActivity : Activity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocker)
        
        val blockedPackage = intent.getStringExtra("blocked_package") ?: ""
        
        val messageTextView = findViewById<TextView>(R.id.message_text)
        val returnButton = findViewById<Button>(R.id.return_button)
        
        // Obtenir le nom de l'application à partir du package
        val packageManager = packageManager
        try {
            val appInfo = packageManager.getApplicationInfo(blockedPackage, 0)
            val appName = packageManager.getApplicationLabel(appInfo)
            messageTextView.text = "L'application $appName est bloquée en mode concentration"
        } catch (e: Exception) {
            messageTextView.text = "Cette application est bloquée en mode concentration"
        }
        
        returnButton.setOnClickListener {
            val homeIntent = Intent(Intent.ACTION_MAIN)
            homeIntent.addCategory(Intent.CATEGORY_HOME)
            homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(homeIntent)
            finish()
        }
    }
    
    override fun onBackPressed() {
        // Empêcher l'utilisateur de fermer cette activité avec le bouton retour
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(homeIntent)
    }
}