package com.afterhours.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.afterhours.app.ui.AfterHoursApp
import com.afterhours.app.ui.AfterHoursTheme

class MainActivity : ComponentActivity() {
    private val vm: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AfterHoursTheme { AfterHoursApp(vm) } }
    }
}
