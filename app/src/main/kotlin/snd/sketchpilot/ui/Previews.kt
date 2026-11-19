package snd.sketchpilot.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
private fun DesignSample() {
    Column(Modifier.padding(16.dp)) {
        Text("SketchPilot", style = MaterialTheme.typography.headlineSmall)
        Text("Projects • Agent • Capabilities", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(Modifier.padding(top = 16.dp)) {
            Text("QuickNotes", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
        }
        Button(onClick = {}, Modifier.padding(top = 16.dp)) { Text("Run diagnostics") }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun LightPreview() {
    SketchPilotTheme { DesignSample() }
}

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun DarkPreview() {
    SketchPilotTheme { DesignSample() }
}
