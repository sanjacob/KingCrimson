package ch.jsan.crimson

import android.Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.jsan.crimson.ui.theme.KingCrimsonTheme


private const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
private const val EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KingCrimsonTheme {
                MyScaffold(onClick = {
                    val componentName = ComponentName(this, SkipService::class.java)
                    requestPermission(componentName)
                })
            }
        }
    }

    private fun requestPermission(componentName: ComponentName) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) return

        val i = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)

            val bundle = Bundle()
            bundle.putString(EXTRA_FRAGMENT_ARG_KEY, componentName.flattenToString())
            putExtra(EXTRA_FRAGMENT_ARG_KEY, componentName.flattenToString())
            putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, bundle)
        }

        startActivity(i)
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopBar() {
    TopAppBar(
        colors = topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        title = {
            Text("King Crimson for YouTube Web")
        }
    )
}


@Composable
fun MyScaffold(onClick: () -> Unit) {
    Scaffold(
        topBar = { MyTopBar() }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.padding(16.dp),
                text =
                """
                To allow the app to work, please enable notification access in settings.
                """.trimIndent(),
            )

            Button(onClick = onClick) {
                Row {
                    Text(text = "Go to settings")
                }
            }
        }

    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    KingCrimsonTheme {
        MyScaffold(onClick = {})
    }
}