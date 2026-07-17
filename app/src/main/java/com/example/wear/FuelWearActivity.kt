package com.example.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.AppDatabase
import com.example.data.model.FuelStation
import com.example.data.repository.FuelRepository
import com.example.ui.FuelProduct
import okhttp3.OkHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FuelWearActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            WearFuelScreen()
        }
    }
}

@Composable
fun WearFuelScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { AppDatabase.getDatabase(context.applicationContext) }
    val repository = remember { FuelRepository(db.favoriteStationDao(), db.wazeIncidentDao(), OkHttpClient()) }
    
    var cheapestStation by remember { mutableStateOf<FuelStation?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedProduct by remember { mutableStateOf(FuelProduct.UNLEADED) }

    fun loadCheapest() {
        isLoading = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stations = repository.fetchFuelPrices(productId = selectedProduct.id)
                cheapestStation = stations.minByOrNull { it.price }
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedProduct) {
        loadCheapest()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "WA FUEL WATCH",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF00F0FF),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { selectedProduct = FuelProduct.UNLEADED },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedProduct == FuelProduct.UNLEADED) Color(0xFF00F0FF) else Color.White.copy(alpha = 0.1f),
                        contentColor = if (selectedProduct == FuelProduct.UNLEADED) Color.Black else Color.White
                    ),
                    modifier = Modifier.size(width = 44.dp, height = 26.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("ULP", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = { selectedProduct = FuelProduct.DIESEL },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedProduct == FuelProduct.DIESEL) Color(0xFF00F0FF) else Color.White.copy(alpha = 0.1f),
                        contentColor = if (selectedProduct == FuelProduct.DIESEL) Color.Black else Color.White
                    ),
                    modifier = Modifier.size(width = 44.dp, height = 26.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("DSL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 2.dp, color = Color(0xFF00F0FF))
                }
            } else {
                cheapestStation?.let { station ->
                    Text(
                        text = station.tradingName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                    Text(
                        text = "${station.price} ¢/L",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color(0xFF22C55E),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = station.location.uppercase(),
                        fontSize = 8.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                } ?: run {
                    Text("No station found", fontSize = 10.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = { loadCheapest() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.size(width = 60.dp, height = 24.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Refresh", fontSize = 9.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
