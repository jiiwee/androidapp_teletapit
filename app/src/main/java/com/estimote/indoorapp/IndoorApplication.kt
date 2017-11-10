package com.estimote.indoorapp

import android.app.Application
import com.estimote.cloud_plugin.common.EstimoteCloudCredentials
import com.estimote.indoorsdk_module.cloud.Location

/**
 * START YOUR JOURNEY HERE!
 * Main app class
 */
class IndoorApplication : Application() {

    // This is map for holding all locations from your account.
    // You can move it somewhere else, but for sake of simplicity we put it in here.
    val locationsById: MutableMap<String, Location> = mutableMapOf()

    // !!! ULTRA IMPORTANT !!!
    // Change your credentials below to have access to locations from your account.
    // Make sure you have any locations created in cloud!
    // If you don't have your Estimote Cloud Account - go to https://cloud.estimote.com/ and create one :)
    val cloudCredentials = EstimoteCloudCredentials("mikko-anttonen-karelia-fi--60s", "5c427e0e9862b71936492cf94c38fe0b")


}