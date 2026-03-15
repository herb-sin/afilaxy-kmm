package com.afilaxy.app.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.afilaxy.app.MainActivity
import com.afilaxy.app.ui.theme.AflixyTheme
import com.afilaxy.domain.repository.EmergencyRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class EmergencyOverlayActivity : ComponentActivity() {
    
    private val emergencyRepository: EmergencyRepository by inject()
    
    private val cancelReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != "com.afilaxy.CANCEL_EMERGENCY") return
            val cancelledEmergencyId = intent.getStringExtra("emergency_id")
                ?.takeIf { it.isNotBlank() } ?: return
            val currentEmergencyId = getIntent().getStringExtra("emergency_id")
                ?.takeIf { it.isNotBlank() } ?: return
            if (cancelledEmergencyId == currentEmergencyId) finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // CWE-306: reject overlay if user is not authenticated
        if (FirebaseAuth.getInstance().currentUser == null) {
            finish()
            return
        }
        
        // Show on lockscreen and turn on screen
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        
        val emergencyId = intent.getStringExtra("emergency_id")
            ?.takeIf { it.isNotBlank() } ?: run { finish(); return }
        val requesterName = intent.getStringExtra("requesterName")?.trim()
            ?.takeIf { it.isNotBlank() } ?: "Alguém"
        val distance = intent.getStringExtra("distance")?.trim()
            ?.takeIf { it.matches(Regex("\\d+(\\.\\d+)?")) } ?: "0"
        
        val filter = IntentFilter("com.afilaxy.CANCEL_EMERGENCY")
        registerReceiver(cancelReceiver, filter, "com.afilaxy.permission.CANCEL_EMERGENCY", null)
        
        setContent {
            AflixyTheme {
                EmergencyOverlayScreen(
                    emergencyId = emergencyId,
                    requesterName = requesterName,
                    distance = distance,
                    onAccept = { 
                        lifecycleScope.launch {
                            emergencyRepository.acceptEmergency(emergencyId)
                        }
                        finish()
                        startMainActivity(emergencyId)
                    },
                    onDecline = { 
                        finish() 
                    }
                )
            }
        }
    }
    
    private fun startMainActivity(emergencyId: String) {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("emergencyId", emergencyId)
            putExtra("openEmergencyResponse", true)
        }
        startActivity(mainIntent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(cancelReceiver)
        } catch (e: Exception) {
            // Receiver já removido
        }
    }
}

@Composable
fun EmergencyOverlayScreen(
    emergencyId: String,
    requesterName: String,
    distance: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Red.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🆘 EMERGÊNCIA",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "$requesterName precisa de ajuda",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Distância: ${distance}m",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("Recusar", color = Color.White)
                    }
                    
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Aceitar Ajudar", color = Color.White)
                    }
                }
            }
        }
    }
}
