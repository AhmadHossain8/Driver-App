package com.example.myapplication.Services

import com.example.myapplication.Common
import com.example.myapplication.Model.EventBus.DriverRequestReceived
import com.example.myapplication.Utils.UserUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.greenrobot.eventbus.EventBus
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if(FirebaseAuth.getInstance().currentUser!=null)
            UserUtils.updateToken(this,token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        if(data!=null)
        {
            if(data[Common.NOTI_TITLE].equals(Common.REQUEST_DRIVER_TITLE))
            {
                val driverRequestReceived = DriverRequestReceived()
                driverRequestReceived.key = data[Common.RIDER_KEY]
                driverRequestReceived.pickupLocation = data[Common.PICKUP_LOCATION]
                driverRequestReceived.pickupLocationString = data[Common.PICKUP_LOCATION_STRING]
                driverRequestReceived.destinationLocation = data[Common.DESTINATION_LOCATION]
                driverRequestReceived.destinationLocationString = data[Common.DESTINATION_LOCATION_STRING]

                //new info
                driverRequestReceived.distanceValue = data[Common.RIDER_DISTANCE_VALUE]!!.toInt()
                driverRequestReceived.distanceText = data[Common.RIDER_DISTANCE_TEXT]!!.toString()
                driverRequestReceived.durationValue = data[Common.RIDER_DURATION_VALUE]!!.toInt()
                driverRequestReceived.durationText = data[Common.RIDER_DURATION_TEXT]!!.toString()
                driverRequestReceived.totalFee = data[Common.RIDER_TOTAL_FEE]!!.toDouble()



                EventBus.getDefault().postSticky(driverRequestReceived)

            }
            else
            {
                Common.showNotification(this, Random.nextInt(),data[Common.NOTI_TITLE],data[Common.NOTI_BODY],null)
            }

        }

    }

}