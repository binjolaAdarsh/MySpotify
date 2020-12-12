package com.adarsh.spotify

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// app should know that we are using  hilt as depedency framework

@HiltAndroidApp
class MyApplication:Application()