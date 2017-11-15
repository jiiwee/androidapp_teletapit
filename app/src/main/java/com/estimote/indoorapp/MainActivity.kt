package com.estimote.indoorapp

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import com.estimote.indoorsdk.IndoorLocationManagerBuilder
import com.estimote.indoorsdk_module.algorithm.OnPositionUpdateListener
import com.estimote.indoorsdk_module.algorithm.ScanningIndoorLocationManager
import com.estimote.indoorsdk_module.cloud.Location
import com.estimote.indoorsdk_module.cloud.LocationPosition
import com.estimote.indoorsdk_module.view.IndoorLocationView
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import java.io.IOException


/**
 * Main view for indoor location
 */

class MainActivity : AppCompatActivity() {

    private lateinit var indoorLocationView: IndoorLocationView
    private lateinit var indoorLocationManager: ScanningIndoorLocationManager
    private lateinit var location: Location
    private lateinit var notification: Notification

    val client = OkHttpClient()

    var s = 0.0
    var beaconNear = ""

    var beaconID = ""

    companion object {
        val intentKeyLocationId = "labra"
        fun createIntent(context: Context, locationId: String): Intent {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(intentKeyLocationId, locationId)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val android_id = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)

        puhelinID.setText("ID = " + android_id)


        // Declare notification that will be displayed in user's notification bar.
        // You can modify it as you want/
        notification = Notification.Builder(this)
                .setSmallIcon(R.drawable.beacon_gray)
                .setContentTitle("Estimote Inc. \u00AE")
                .setContentText("Indoor location is running...")
                .setPriority(Notification.PRIORITY_HIGH)
                .build()

        // Get location id from intent and get location object from list of locations
        setupLocation()

        // Init indoor location view here
        indoorLocationView = findViewById(R.id.indoor_view) as IndoorLocationView

        // Give location object to your view to draw it on your screen
        indoorLocationView.setLocation(location)

        // Create IndoorManager object.
        // Long story short - it takes list of scanned beacons, does the magic and returns estimated position (x,y)
        // You need to setup it with your app context,  location data object,
        // and your cloud credentials that you declared in IndoorApplication.kt file
        // we are using .withScannerInForegroundService(notification)
        // this will allow for scanning in background and will ensura that the system won't kill the scanning.
        // You can also use .withSimpleScanner() that will be handled without service.
        indoorLocationManager = IndoorLocationManagerBuilder(this, location, (application as IndoorApplication).cloudCredentials)
                .withScannerInForegroundService(notification)
                .build()

        // Hook the listener for position update events
        indoorLocationManager.setOnPositionUpdateListener(object : OnPositionUpdateListener {
            override fun onPositionOutsideLocation() {
                indoorLocationView.hidePosition()
                x.setText("X = ")
                y.setText("Y = ")
            }

            override fun onPositionUpdate(locationPosition: LocationPosition) {
                indoorLocationView.updatePosition(locationPosition)

                runOnUiThread {
                    val currentX = Math.round(locationPosition.x * 100.0) / 100.0
                    val currentY = Math.round(locationPosition.y * 100.0) / 100.0
                    x.setText("X = " + currentX.toString())
                    y.setText("Y = " + currentY.toString())

                    // candy 0 , sweet beet 1 , blueberry 2, lemon 3

                    var nearestBeacon = 10000.00
                    var beaconColor = ""
                    for (i in location.beacons.indices) {

                        var positionX = location.beacons[i].position.x
                        var positionY = location.beacons[i].position.y

                        // Distanceen lasketaan matka lähimmältä beaconilta puhelimeen pythagoraan lauseen avulla
                        var distance = Math.sqrt(((currentX - positionX) * (currentX - positionX)) + ((currentY - positionY) * (currentY - positionY)))


                        // Kun havaitaan että joku beacon on lähellä, tallennetaan se muuttujaan
                        if (distance < nearestBeacon) {
                            nearestBeacon = distance
                            beaconColor = location.beacons[i].beacon.color.toString()
                            beaconID = location.beacons[i].beacon.mac
                            s = nearestBeacon


                            baconID.setText("Beaconin ID = " + beaconID)
                            baconColor.setText("Beaconin väri = " + beaconColor)
                        }
                    }

                    run("https://tranquil-chamber-16062.herokuapp.com/api/Y", "[{\"Phoneid\":\""+ android_id + "\",\"Beaconid\":\"" + beaconID +"\",\"X\":\""+ currentX + "\",\"Y\":\"" + currentY + "\"}]")

                    if(beaconColor == beaconNear){

                    } else {
                        if(s < 2){
                        beaconNear = beaconColor


                        run("https://tranquil-chamber-16062.herokuapp.com/api/Yes", "[{\"Phoneid\":\""+ android_id + "\",\"Beaconid\":\"" + beaconID +"\"}]")
                        } else {

                        }
                    }
                }
            }
        })

        // Start positioning in onCreate. Because we are using withScannerInForegroundService(...)
        // The scanning will last until  indoorLocationManager.stopPositioning() be called.
        // We call it in onDestroy().
        // You can enable indoor positioning even when an app is killed, but you will need to start/stop
        // positioning in your object that extends Application -> in case of this app, the IndoorApplication.kt file
        indoorLocationManager.startPositioning()
    }

    fun run(url: String, data : String) {

    val body = RequestBody.create(MediaType.parse("application/json"), data )
      val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) = println(response.body()?.string())
        })
    }

    private fun setupLocation() {
        // get id of location to show from intent
        val locationId = intent.extras.getString(intentKeyLocationId)
        // get object of location. If something went wrong, we build empty location with no data.
        location = (application as IndoorApplication).locationsById[locationId] ?: buildEmptyLocation()
        // Set the Activity title to you location name
        title = location.name
    }

    private fun buildEmptyLocation(): Location {
        return Location("", "", true, "", 0.0, emptyList(), emptyList(), emptyList())
    }


    override fun onDestroy() {
        indoorLocationManager.stopPositioning()
        super.onDestroy()
    }
}

// run("https://tranquil-chamber-16062.herokuapp.com/api/Y", "[{\"Phoneid\":\""+ android_id + "\",\"Beaconid\":\"" + beaconID +"\",\"X\":\""+ currentX + "\",\"Y\":\"" + currentY + "\"}]")
