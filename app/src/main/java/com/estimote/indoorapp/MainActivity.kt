package com.estimote.indoorapp

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.estimote.coresdk.service.BeaconManager
import com.estimote.indoorsdk.IndoorLocationManagerBuilder
import com.estimote.indoorsdk_module.algorithm.OnPositionUpdateListener
import com.estimote.indoorsdk_module.algorithm.ScanningIndoorLocationManager
import com.estimote.indoorsdk_module.b.b.i
import com.estimote.indoorsdk_module.cloud.Location
import com.estimote.indoorsdk_module.cloud.LocationPosition
import com.estimote.indoorsdk_module.view.IndoorLocationView
import kotlinx.android.synthetic.main.activity_main.*
import java.text.DecimalFormat

/**
 * Main view for indoor location
 */

class MainActivity : AppCompatActivity() {

    private lateinit var indoorLocationView: IndoorLocationView
    private lateinit var indoorLocationManager: ScanningIndoorLocationManager
    private lateinit var location: Location
    private lateinit var notification: Notification

    var currentX = "asd"
    var currentY = "asd"


    companion object {
        val intentKeyLocationId = "mikko-anttonen-s-location-ogl"
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

        puhelinID.setText("ID = " + android_id )


        // Declare notification that will be displayed in user's notification bar.
        // You can modify it as you want/
        notification = Notification.Builder(this)
                .setSmallIcon(R.drawable.beacon_gray)
                .setContentTitle("Estimote Inc. \u00AE")
                .setContentText("Indoor location is running..." + android_id)
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
                    //x.setText("X = " + location.beacons[1].position.x)
                    //nbeacon.setText("Beacon data = " + location.beacons)

                    // sininen 0
                    // candy aka pinkki 1
                    // lemon aka keltanen 2
                    // beetroot aka punanen 3

                    var nearestBeacon = 10000.00
                    var beaconColor = ""
                    var beaconID = ""
                    for (i in location.beacons.indices)
                    {

                        var positionX = location.beacons[i].position.x
                        var positionY = location.beacons[i].position.y
                        var distance = Math.sqrt(((currentX - positionX) * (currentX - positionX)) + ((currentY- positionY) * (currentY- positionY)))

                        if(distance < nearestBeacon){
                            nearestBeacon = distance
                            beaconColor = location.beacons[i].beacon.color.toString()
                            beaconID = location.beacons[i].beacon.mac
                        }

                    }
                    puhelinID.setText("Pekonin ID " + beaconID)
                    nbeacon.setText("Pekonin väri = " + beaconColor)
                    //Log.d("Lista", "Tiedot: " + beaconColr)

     /*

                    var sininenX1 = location.beacons[0].position.x
                    var sininenY1 = location.beacons[0].position.y

                    var beaconX2 = location.beacons[2].position.x
                    var beaconY2 = location.beacons[2].position.y

                    var compareX1 = currentX - sininenX1
                    var compareY1 = currentY - sininenY1

                    var compareX2 = currentX - beaconX2
                    var compareY2 = currentY - beaconY2

                    if (Math.abs(compareX1) < Math.abs(compareX2)) {
                        nbeacon.setText("Nearest beacon = " + location.beacons[3].beacon.color)

                        //Log.d("asd","asd" + "Sinisen beaconin teksti")
                        //Log.d("asd","asd" + compareX1 + " Sininen")
                        //Log.d("asd","asd" + compareX2 + " Keltanen")
                    } else {
                        nbeacon.setText("Nearest beacon = " + location.beacons[1].beacon.color)
                        //Log.d("asd","asd" + "Keltaisen beaconin teksti")
                    }
*/
                }

            }


        })

        // Start positioning in onCreate. Because we are using withScannerInForegroundService(...)
        // The scanning will last until  indoorLocationManager.stopPositioning() be called.
        // We call it in onDestroy().
        // You can enable indoor positioning even when an app is killed, but you will need to start/stop
        // positioning in your object that extends Application -> in case of this app, the IndoorApplication.kt file
        indoorLocationManager.startPositioning()


        //hello.setText("ID = " + android_id + "\n" + "Start Location X = " + LocationPosition().x + "\n" +
          //      "Start Location y = " + LocationPosition().y )

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