package com.ebookreader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SplashContent(
                onFinish = {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            )
        }
    }
}

@Composable
private fun SplashContent(onFinish: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1500)
        onFinish()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF6F4CE0)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(0.85f),
            contentScale = ContentScale.Fit
        )
    }
}
