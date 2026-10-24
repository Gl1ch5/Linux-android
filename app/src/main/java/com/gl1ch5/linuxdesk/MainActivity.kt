package com.gl1ch5.linuxdesk

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gl1ch5.linuxdesk.catalog.Distro
import com.gl1ch5.linuxdesk.catalog.Distros
import com.gl1ch5.linuxdesk.catalog.SessionType
import com.gl1ch5.linuxdesk.log.AppLog
import com.gl1ch5.linuxdesk.runtime.ProotRunner
import com.gl1ch5.linuxdesk.runtime.SystemChecks
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LinuxDeskTheme {
                AppRoot()
            }
        }
    }
}

private enum class Screen { Home, Diagnostics, Logs }

@Composable
private fun AppRoot() {
    var screen by remember { mutableStateOf(Screen.Home) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (screen) {
                            Screen.Home -> "LinuxDesk"
                            Screen.Diagnostics -> "Диагностика"
                            Screen.Logs -> "Логи"
                        },
                    )
                },
                navigationIcon = {
                    if (screen != Screen.Home) {
                        OutlinedButton(onClick = { screen = Screen.Home }) { Text("Назад") }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when (screen) {
            Screen.Home -> HomeScreen(
                padding = padding,
                onOpenDiagnostics = { screen = Screen.Diagnostics },
                onOpenLogs = { screen = Screen.Logs },
                onDistroTap = { distro ->
                    AppLog.i("UI", "Нажата плитка ${distro.id}; установка пока не реализована")
                    scope.launch { snackbar.showSnackbar("«${distro.name}»: установка появится на этапе 2") }
                },
            )
            Screen.Diagnostics -> DiagnosticsScreen(padding)
            Screen.Logs -> LogsScreen(padding)
        }
    }
}

@Composable
private fun HomeScreen(
    padding: PaddingValues,
    onOpenDiagnostics: () -> Unit,
    onOpenLogs: () -> Unit,
    onDistroTap: (Distro) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onOpenDiagnostics) { Text("Диагностика") }
            OutlinedButton(onClick = onOpenLogs) { Text("Логи") }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(Distros.all, key = { it.id }) { distro ->
                DistroTile(distro = distro, onClick = { onDistroTap(distro) })
            }
        }
    }
}

@Composable
private fun DistroTile(distro: Distro, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(180.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = distro.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = distro.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Badge(if (distro.session == SessionType.X11) "X11" else "Wayland")
                Badge("Не установлен")
            }
        }
    }
}

@Composable
private fun Badge(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

@Composable
private fun DiagnosticsScreen(padding: PaddingValues) {
    val context = LocalContext.current
    val phantom = SystemChecks.phantomProcessRestriction(context)
    val vulkan = SystemChecks.vulkanVersionLabel(context)
    val bundled = remember { ProotRunner(context).isBundled() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Section("Устройство") {
            SystemChecks.deviceSummary().forEach { (k, v) -> Line(k, v) }
            Line("Vulkan", vulkan)
        }
        Section("Компоненты") {
            Line("proot и loader в APK", if (bundled) "да" else "нет (появятся после сборки этапа 1)")
        }
        Section("Фоновая работа") {
            when (phantom) {
                SystemChecks.Flag.Disabled -> Line("Ограничения дочерних процессов", "отключены")
                SystemChecks.Flag.Enabled -> {
                    Line("Ограничения дочерних процессов", "ВКЛЮЧЕНЫ")
                    Text(
                        "Android может убить Linux-сессию в фоне. Откройте: Настройки → Для разработчиков → " +
                            "«Отключить ограничения дочерних процессов». Без этого сессия может прерваться.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                SystemChecks.Flag.Unknown -> Line(
                    "Ограничения дочерних процессов",
                    "не удалось определить; проверьте вручную в «Для разработчиков»",
                )
            }
        }
    }
}

@Composable
private fun LogsScreen(padding: PaddingValues) {
    val context = LocalContext.current
    var text by remember { mutableStateOf(AppLog.snapshot(maxLines = 500)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { text = AppLog.snapshot(maxLines = 500) }) { Text("Обновить") }
            Button(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "LinuxDesk log")
                    putExtra(Intent.EXTRA_TEXT, "Android ${Build.VERSION.RELEASE}, ${Build.MODEL}\n\n" + AppLog.snapshot(maxLines = 1500))
                }
                context.startActivity(Intent.createChooser(send, "Поделиться логом"))
            }) { Text("Поделиться") }
        }
        Text(
            text = text.ifEmpty { "Записей пока нет." },
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun LinuxDeskTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
