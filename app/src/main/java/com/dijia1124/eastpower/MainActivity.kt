package com.dijia1124.eastpower

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dijia1124.eastpower.ui.theme.EastPowerTheme

class MainActivity : ComponentActivity() {
    private lateinit var prefsRepo: PrefsRepository
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefsRepo = PrefsRepository(applicationContext)
        enableEdgeToEdge()
        setContent {
            EastPowerTheme {
                Scaffold(
                    topBar = {TopAppBar(title = {Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)})},
                    modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BrightnessLocker(prefsRepo, innerPadding)
                }
            }
        }
    }
}
