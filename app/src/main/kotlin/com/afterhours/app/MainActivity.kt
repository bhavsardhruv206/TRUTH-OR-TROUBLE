package com.afterhours.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.afterhours.app.ads.AdManager
import com.afterhours.app.ui.AfterHoursApp
import com.afterhours.app.ui.AfterHoursTheme

class MainActivity : ComponentActivity() {
    private val vm: GameViewModel by viewModels()
    private val ads by lazy { AdManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ads.initialize()
        setContent { AfterHoursTheme { AfterHoursApp(vm, ads) } }
    }
}
