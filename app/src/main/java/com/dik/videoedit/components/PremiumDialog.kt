package com.dik.videoedit.components

import com.dik.videoedit.i18n.tr
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dik.videoedit.LocalAppContainer
import com.dik.videoedit.admob.AdState
import com.dik.videoedit.ui.theme.Blue
import com.dik.videoedit.ui.theme.Gold
import kotlinx.coroutines.launch

/**
 * Layar "Fitur Premium". Satu iklan reward membuka semua fitur premium selama 30 menit.
 * [onGranted] dipanggil HANYA setelah callback reward AdMob diterima.
 */
@Composable
fun PremiumDialog(onDismiss: () -> Unit, onGranted: () -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val adState by container.ads.state.collectAsState()
    val scope = rememberCoroutineScope()
    var showing by remember { mutableStateOf(false) }

    // Iklan dimuat hanya ketika dialog ini dibuka oleh pengguna.
    LaunchedEffect(Unit) { container.ads.load() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(24.dp))
                Box(
                    Modifier.size(110.dp).clip(CircleShape).background(Gold.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.WorkspacePremium, null, tint = Gold, modifier = Modifier.size(64.dp)) }
                Spacer(Modifier.height(16.dp))
                Text(tr("Buka Fitur Premium"), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    tr("Tonton iklan untuk membuka fitur premium berikut selama 30 menit:"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        FeatureRow(Icons.Filled.FilterVintage, "Filter Warna Premium")
                        FeatureRow(Icons.Filled.Movie, "Transisi Keren")
                        FeatureRow(Icons.Filled.AutoAwesome, "Efek Video")
                        FeatureRow(Icons.Filled.Verified, "Ekspor Tanpa Watermark")
                    }
                }
                Spacer(Modifier.weight(1f))

                val statusText = when (val s = adState) {
                    AdState.Loading -> "Memuat iklan…"
                    AdState.Ready -> "Iklan siap ditonton."
                    is AdState.Failed -> s.message
                    AdState.Idle -> ""
                }
                if (statusText.isNotEmpty()) {
                    Text(
                        statusText, fontSize = 13.sp, textAlign = TextAlign.Center,
                        color = if (adState is AdState.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                }

                val label = when (adState) {
                    AdState.Loading -> "Memuat iklan…"
                    AdState.Ready -> "Tonton Iklan untuk Menggunakan Fitur"
                    else -> "Muat Ulang Iklan"
                }
                Button(
                    onClick = {
                        if (adState == AdState.Ready) {
                            val activity = context.findActivity()
                            if (activity == null) {
                                Toast.makeText(context, tr("Tidak dapat menampilkan iklan."), Toast.LENGTH_SHORT).show()
                            } else {
                                showing = true
                                container.ads.show(
                                    activity,
                                    onReward = {
                                        // Reward diberikan HANYA di sini.
                                        scope.launch { container.premium.grant() }
                                        onGranted()
                                    },
                                    onClosed = { showing = false },
                                )
                            }
                        } else {
                            container.ads.load()
                        }
                    },
                    enabled = adState != AdState.Loading && !showing,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Filled.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text(label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(10.dp))
                SecondaryButton("Nanti Saja", onDismiss)
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(Blue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = Blue) }
        Spacer(Modifier.width(14.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Unspecified)
    }
}
