package com.example.myapplication.ui.home

import android.content.res.Resources
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentHomeBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import android.Manifest
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.CountDownTimer
import android.text.TextUtils
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.example.myapplication.ChatActivity
import com.example.myapplication.Common
import com.example.myapplication.DriverHomeActivity
import com.example.myapplication.Model.EventBus.DriverRequestReceived
import com.example.myapplication.Model.EventBus.NotifyRiderEvent
import com.example.myapplication.Model.RiderModel
import com.example.myapplication.Model.TripPlanModel
import com.example.myapplication.Remote.IGoogleAPI
import com.example.myapplication.Remote.RetrofitClient
import com.example.myapplication.Utils.UserUtils
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.maps.model.*
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.kusu.loadingbutton.LoadingButton
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_home.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap


class HomeFragment : Fragment(), OnMapReadyCallback{

    internal var number:String?=""
    internal var emergency:String?="999"
    //Views
    private lateinit var chip_decline:Chip
    private lateinit var layout_accept:CardView
    private lateinit var circularProgressBar:CircularProgressBar
    private lateinit var txt_estimate_time:TextView
    private lateinit var txt_estimate_distance:TextView
    private lateinit var root_layout:FrameLayout

    private lateinit var txt_rating:TextView
    private lateinit var txt_type_uber:TextView
    private lateinit var img_round:ImageView
    private lateinit var layout_start_uber:CardView
    private lateinit var txt_rider_name:TextView
    private lateinit var txt_rider_id:TextView
    private lateinit var txt_rider_number:TextView
    private lateinit var txt_start_uber_estimate_distance:TextView;
    private lateinit var txt_start_uber_estimate_time:TextView;
    private lateinit var img_phone_call:ImageView
    private lateinit var btn_start_uber:LoadingButton
    private lateinit var btn_complete_trip:LoadingButton
    private lateinit var btn_chat:LoadingButton
    private lateinit var btn_phone_call:LoadingButton
    private lateinit var btn_emergency:LoadingButton


    private lateinit var layout_notify_rider:LinearLayout
    private lateinit var txt_notify_rider:TextView
    private lateinit var progress_notify:ProgressBar

    private var pickupGeoFire:GeoFire?=null
    private var pickupGeoQuery: GeoQuery?=null

    private var destinationGeoFire:GeoFire ?= null
    private var destinationGeoQuery:GeoQuery ?= null

    private val pickupGeoQueryListener = object : GeoQueryEventListener{
        override fun onKeyEntered(key: String?, location: GeoLocation?) {
            btn_start_uber.isEnabled=true
            UserUtils.sendNotifyToRider(requireContext()!!,root_layout,key)
            if(pickupGeoQuery!=null)
            {
                //remove
                pickupGeoFire!!.removeLocation(key)
                pickupGeoFire=null
                pickupGeoQuery!!.removeAllListeners()
            }
        }

        override fun onKeyExited(key: String?) {
            btn_start_uber.isEnabled=false
        }

        override fun onKeyMoved(key: String?, location: GeoLocation?) {

        }

        override fun onGeoQueryReady() {

        }

        override fun onGeoQueryError(error: DatabaseError?) {

        }
    }

    private val destinationGeoQueryListener = object : GeoQueryEventListener{
        override fun onKeyEntered(key: String?, location: GeoLocation?) {
            Toast.makeText(requireContext(),"Destination Entered",Toast.LENGTH_SHORT).show()
            btn_complete_trip.isEnabled = true
            if(destinationGeoQuery != null){
                destinationGeoFire!!.removeLocation(key)
                destinationGeoFire = null
                destinationGeoQuery!!.removeAllListeners()
            }
        }

        override fun onKeyExited(key: String?) {

        }

        override fun onKeyMoved(key: String?, location: GeoLocation?) {

        }

        override fun onGeoQueryReady() {

        }

        override fun onGeoQueryError(error: DatabaseError?) {

        }
    }

    private var waiting_timer:CountDownTimer?=null

    private  var isTripStart = false
    private  var onlineSystemAlreadyRegister = false

    private var tripNumberId:String ?= ""


    //Rutes
    private val compositeDisposable = CompositeDisposable()
    private lateinit var iGoogleAPI: IGoogleAPI
    private var blackPolyLine: Polyline?=null
    private var grayPolyLine: Polyline?=null
    private var polyLineOptions: PolylineOptions?=null
    private var blackPolylineOptions: PolylineOptions?=null
    private var polylineList:ArrayList<LatLng?>?=null

    private lateinit var mMap: GoogleMap
    private var _binding: FragmentHomeBinding? = null
    private lateinit var mapFragment:SupportMapFragment

    //Location
    private var locationRequest:LocationRequest?=null
    private var locationCallback:LocationCallback?=null
    private var fusedLocationProviderClient: FusedLocationProviderClient?=null
    // This property is only valid between onCreateView and
    // onDestroyView.
    //online system
    private lateinit var onlineRef:DatabaseReference
    private var currentUserRef:DatabaseReference?=null
    private lateinit var driversLocationRef:DatabaseReference
    private lateinit var geoFire: GeoFire

    //decline
    private var driverRequestReceived : DriverRequestReceived?=null
    private var countDownEvent:Disposable?=null

    private val onlineValueEventListener=object :ValueEventListener{
        override fun onDataChange(p0: DataSnapshot) {
            if(p0.exists() && currentUserRef != null)
                currentUserRef!!.onDisconnect().removeValue()

        }

        override fun onCancelled(p0: DatabaseError) {
            Snackbar.make(mapFragment.requireView(),p0.message,Snackbar.LENGTH_LONG).show()
        }

    }

    override fun onStart() {
        super.onStart()
        if(!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this )
    }

    override fun onDestroy() {
        fusedLocationProviderClient!!.removeLocationUpdates(locationCallback!!)
        geoFire.removeLocation(FirebaseAuth.getInstance().currentUser!!.uid)
        onlineRef.removeEventListener(onlineValueEventListener)

        compositeDisposable.clear()

        onlineSystemAlreadyRegister = false

        if(EventBus.getDefault().hasSubscriberForEvent(DriverHomeActivity::class.java))
            EventBus.getDefault().removeStickyEvent(DriverHomeActivity::class.java)
        if(EventBus.getDefault().hasSubscriberForEvent(NotifyRiderEvent::class.java))
            EventBus.getDefault().removeStickyEvent(NotifyRiderEvent::class.java)
        EventBus.getDefault().unregister(this);
        super.onDestroy()
    }

    override fun onResume() {

        super.onResume()
        registerOnlineSysten()
    }

    private fun registerOnlineSysten() {
        if(!onlineSystemAlreadyRegister) {
            onlineRef.addValueEventListener(onlineValueEventListener)
            onlineSystemAlreadyRegister = true
        }
    }

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        initViews(root);

        init()
        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        return root
    }

    private fun initViews(root: View?) {
        chip_decline=root!!.findViewById(R.id.chip_decline) as Chip
        layout_accept =root!!.findViewById(R.id.layout_accept) as CardView
        circularProgressBar=root!!.findViewById(R.id.circularProgressBar) as CircularProgressBar
        txt_estimate_distance =root!!.findViewById(R.id.txt_estimate_distance) as TextView
        txt_estimate_time=root!!.findViewById(R.id.txt_estimate_time) as TextView
        root_layout=root!!.findViewById(R.id.root_layout) as FrameLayout


        txt_rating = root!!.findViewById(R.id.txt_rating) as TextView
        txt_type_uber = root!!.findViewById(R.id.txt_type_uber) as TextView
        img_round = root!!.findViewById(R.id.img_round) as ImageView
        layout_start_uber = root!!.findViewById(R.id.layout_start_uber) as CardView
        txt_rider_name = root!!.findViewById(R.id.txt_rider_name) as TextView
        txt_rider_id=root!!.findViewById(R.id.txt_rider_id) as TextView
        txt_rider_number=root!!.findViewById(R.id.txt_rider_number) as TextView
        txt_start_uber_estimate_distance = root!!.findViewById(R.id.txt_start_uber_estimate_distance) as TextView
        txt_start_uber_estimate_time = root!!.findViewById(R.id.txt_start_uber_estimate_time) as TextView

        btn_start_uber = root!!.findViewById(R.id.btn_start_uber) as LoadingButton
        btn_complete_trip = root!!.findViewById(R.id.btn_complete_trip) as LoadingButton
        btn_chat=root!!.findViewById(R.id.btn_chat) as LoadingButton
        btn_phone_call=root!!.findViewById(R.id.btn_phone_call) as LoadingButton
        btn_emergency=root!!.findViewById(R.id.btn_emergency) as LoadingButton

        layout_notify_rider=root!!.findViewById(R.id.layout_notify_rider) as LinearLayout
        txt_notify_rider=root!!.findViewById(R.id.txt_notify_rider) as TextView
        progress_notify=root!!.findViewById(R.id.progress_notify) as ProgressBar
        //Event
        chip_decline.setOnClickListener{
            if(TextUtils.isEmpty(tripNumberId))
            {
                if(driverRequestReceived != null){
                    if(countDownEvent != null)
                        countDownEvent!!.dispose()

                    chip_decline.visibility = View.GONE
                    layout_accept.visibility = View.GONE
                    mMap.clear()
                    circularProgressBar.progress = 0f
                    UserUtils.sendDeclineRequest(root_layout,requireActivity()!!,driverRequestReceived!!.key!!)
                    driverRequestReceived = null
                }
            }
            else
            {
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Snackbar.make(mapFragment.requireView(),getString(R.string.permission_require),Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                fusedLocationProviderClient!!.lastLocation
                    .addOnFailureListener { e->
                        Snackbar.make(mapFragment.requireView(),e.message!!,Snackbar.LENGTH_SHORT).show()
                    }
                    .addOnSuccessListener { location->
                        chip_decline.visibility = View.GONE
                        layout_start_uber.visibility=View.GONE
                        mMap.clear()
                        UserUtils.sendDeclineAndRemoveTripRequest(root_layout,requireActivity()!!,driverRequestReceived!!.key!!,tripNumberId)
                        tripNumberId="" //set it empty after remove
                        driverRequestReceived = null
                        makeDriverOnline(location)
                    }
            }

        }
        btn_chat.setOnClickListener {
            val intent=Intent(context,ChatActivity::class.java)
            intent.putExtra("riderName",txt_rider_name.text.toString())
            intent.putExtra("receiverUid",txt_rider_id.text.toString())
            startActivity(intent)

        }
        btn_phone_call.setOnClickListener {
            number=txt_rider_number.text.toString().trim()
            val intent=Intent(Intent.ACTION_DIAL, Uri.parse("tel:"+ Uri.encode(number)))
            startActivity(intent)
        }
        btn_emergency.setOnClickListener {

            val intent=Intent(Intent.ACTION_DIAL, Uri.parse("tel:"+ Uri.encode(emergency)))
            startActivity(intent)
        }

        btn_start_uber.setOnClickListener {
            if(blackPolyLine != null)blackPolyLine!!.remove()
            if(grayPolyLine != null)grayPolyLine!!.remove()
            if(waiting_timer != null)waiting_timer!!.cancel();
            layout_notify_rider.visibility = View.GONE
            if(driverRequestReceived != null){
                val destinationLatLng = LatLng(
                    driverRequestReceived!!.destinationLocation!!.split(",")[0].toDouble(),
                    driverRequestReceived!!.destinationLocation!!.split(",")[1].toDouble()
                )
                mMap.addMarker(MarkerOptions().position(destinationLatLng)
                    .title(driverRequestReceived!!.destinationLocationString)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)))

                drawPathFromCurrentLocation(driverRequestReceived!!.destinationLocation)
            }
            btn_start_uber.visibility = View.GONE
            chip_decline.visibility = View.GONE
            btn_complete_trip.visibility = View.VISIBLE
        }

        btn_complete_trip.setOnClickListener {
            //Trip done update
            val update_trip = HashMap<String,Any>()
            update_trip.put("done",true)
            FirebaseDatabase.getInstance().getReference(Common.TRIP)
                .child(tripNumberId!!)
                .updateChildren(update_trip)
                .addOnFailureListener { e -> Snackbar.make(requireView(),e.message!!,Snackbar.LENGTH_LONG).show() }
                .addOnSuccessListener { location ->
                    fusedLocationProviderClient!!.lastLocation
                        .addOnFailureListener { e->
                            Snackbar.make(requireView(),e.message!!,Snackbar.LENGTH_LONG).show()
                        }
                        .addOnSuccessListener { location->
                            UserUtils.sendCompleteTripToRider(mapFragment.requireView(),
                            requireContext(),driverRequestReceived!!.key,tripNumberId!!)

                            //Reset View

                            mMap.clear()
                            tripNumberId = ""
                            isTripStart = false
                            chip_decline.visibility = View.GONE

                            layout_accept.visibility = View.GONE
                            circularProgressBar.progress = 0.toFloat()

                            layout_start_uber.visibility = View.GONE

                            layout_notify_rider.visibility = View.GONE
                            progress_notify.progress = 0

                            btn_complete_trip.isEnabled = false
                            btn_complete_trip.visibility = View.GONE

                            btn_start_uber.isEnabled = false
                            btn_start_uber.visibility = View.GONE

                            destinationGeoFire = null
                            pickupGeoFire = null

                            driverRequestReceived = null
                            makeDriverOnline(location)

                        }
                }
        }

    }

    private fun drawPathFromCurrentLocation(destinationLocation: String?) {

        if (ActivityCompat.checkSelfPermission(
                requireContext()!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext()!!,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Snackbar.make(  requireView(),R.string.permission_require,Snackbar.LENGTH_LONG).show()
            return
        }
        fusedLocationProviderClient!!.lastLocation
            .addOnFailureListener{ e->
                Snackbar.make(requireView(),e.message!!,Snackbar.LENGTH_LONG).show()
            }
            .addOnSuccessListener { location->
                compositeDisposable.add(iGoogleAPI.getDirection("driving" ,
                    "less_driving",
                    StringBuilder()
                        .append(location.latitude)
                        .append(",")
                        .append(location.longitude)
                        .toString(),destinationLocation,
                    "AIzaSyCDz6LWeUuWz7f4OYPeKkFIJy5QCgxyW84")
                !!.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe{ returnResult->
                        Log.d("API_RETURN",returnResult)
                        try{
                            val jsonObject = JSONObject(returnResult)
                            val jsonArray=jsonObject.getJSONArray("routes");
                            for(i in 0 until jsonArray.length())
                            {
                                val route=jsonArray.getJSONObject(i)
                                val poly=route.getJSONObject("overview_polyline")
                                val polyline=poly.getString("points")
                                polylineList=Common.decodePoly(polyline)

                            }

                            polyLineOptions= PolylineOptions()

                            polyLineOptions!!.color(Color.GRAY)
                            polyLineOptions!!.width(12f)

                            polyLineOptions!!.startCap(SquareCap())
                            polyLineOptions!!.jointType(JointType.ROUND)
                            polyLineOptions!!.addAll(polylineList!!)
                            grayPolyLine=mMap.addPolyline(polyLineOptions!!)

                            blackPolylineOptions= PolylineOptions()
                            blackPolylineOptions!!.color(Color.BLACK)
                            blackPolylineOptions!!.width(5f)
                            blackPolylineOptions!!.startCap(SquareCap())
                            blackPolylineOptions!!.jointType(JointType.ROUND)
                            blackPolylineOptions!!.addAll(polylineList!!)
                            blackPolyLine=mMap.addPolyline(blackPolylineOptions!!)



                            val origin=LatLng(location.latitude,location.longitude)
                            val destination =LatLng(destinationLocation!!.split(",")[0].toDouble(),
                                destinationLocation!!.split(",")[1].toDouble())

                            val latLngBound= LatLngBounds.Builder().include(origin)
                                .include(destination)
                                .build()

                            createGeoFireDestinationLocation(driverRequestReceived!!.key,destination)

                            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBound,160))
                            mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.cameraPosition!!.zoom-1))



                        }catch (e: Exception){
                            Toast.makeText(requireContext()!!,e.message!!,Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
    }

    private fun createGeoFireDestinationLocation(key: String?, destination: LatLng) {

        val ref = FirebaseDatabase.getInstance().getReference(Common.TRIP_DESTINATION_LOCATION_REF)
        destinationGeoFire = GeoFire(ref)
        destinationGeoFire!!.setLocation(key!!,
            GeoLocation(destination.latitude,destination.longitude),{key1,error ->

            })
    }

    private fun init() {
        iGoogleAPI= RetrofitClient.instance!!.create(IGoogleAPI::class.java)
        onlineRef=FirebaseDatabase.getInstance().getReference().child(".info/connected")

        //If permission not allowed, Don't init it.
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Snackbar.make(root_layout,getString(R.string.permission_require),Snackbar.LENGTH_LONG).show()
            return
        }


        buildLocationRequest()

        buildLocationCallBack()

        updateLocation()


    }

    private fun updateLocation() {
        if(fusedLocationProviderClient==null)
        {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Snackbar.make(root_layout,getString(R.string.permission_require),Snackbar.LENGTH_LONG).show()
                return
            }
            fusedLocationProviderClient!!.requestLocationUpdates(locationRequest!!,locationCallback!!, Looper.myLooper())
        }
    }

    private fun buildLocationCallBack() {
       if(locationCallback==null)
       {
           locationCallback = object : LocationCallback() {
               override fun onLocationResult(locationResult: LocationResult) {
                   super.onLocationResult(locationResult)

                   val newPos = LatLng(locationResult!!.lastLocation!!.latitude,locationResult!!.lastLocation!!.longitude)

                   if(pickupGeoFire!=null)
                   {
                       pickupGeoQuery=
                           pickupGeoFire!!.queryAtLocation(GeoLocation(locationResult.lastLocation!!.latitude,
                               locationResult.lastLocation!!.longitude),Common.MIN_RANGE_PICKUP_IN_KM)
                       pickupGeoQuery!!.addGeoQueryEventListener(pickupGeoQueryListener)
                   }

                   if(destinationGeoFire !=null)
                   {
                       destinationGeoQuery =
                           destinationGeoFire!!.queryAtLocation(GeoLocation(locationResult.lastLocation!!.latitude,
                               locationResult.lastLocation!!.longitude),Common.MIN_RANGE_PICKUP_IN_KM)
                       destinationGeoQuery!!.addGeoQueryEventListener(destinationGeoQueryListener)
                   }

                   mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos,18f))

                   if(!isTripStart) {
                       makeDriverOnline(locationResult.lastLocation!!)

                   }else{
                       if(!TextUtils.isEmpty(tripNumberId)){
                           val update_date = HashMap<String,Any>()
                           update_date["currentLat"] = locationResult.lastLocation!!.latitude
                           update_date["currentLng"] = locationResult.lastLocation!!.longitude

                           FirebaseDatabase.getInstance().getReference(Common.TRIP)
                               .child(tripNumberId!!)
                               .updateChildren(update_date)
                               .addOnFailureListener{e->
                                   Snackbar.make(mapFragment.requireView(),e.message!!,Snackbar.LENGTH_LONG).show()
                               }
                               .addOnCanceledListener {  }
                       }
                   }
               }
           }
       }
    }

    private fun makeDriverOnline(location: Location) {
        val geoCoder = Geocoder(requireContext(), Locale.getDefault())
        val addressList: List<Address>?
        try {
            addressList = geoCoder.getFromLocation(
                location.latitude,
                location.longitude, 1
            )
            val cityName = addressList[0].locality
            driversLocationRef = FirebaseDatabase.getInstance()
                .getReference(Common.DRIVER_LOCATION_REFERENCES)
                .child(cityName)
            currentUserRef = driversLocationRef.child(
                FirebaseAuth.getInstance().currentUser!!.uid
            )
            geoFire = GeoFire(driversLocationRef)
            //updated Location
            geoFire.setLocation(
                FirebaseAuth.getInstance().currentUser!!.uid,
                GeoLocation(
                    location.latitude,
                    location.longitude
                )

            ) { key: String?, error: DatabaseError? ->
                if (error != null)
                    Snackbar.make(
                        mapFragment.requireView(),
                        error.message,
                        Snackbar.LENGTH_LONG
                    ).show()


            }
            registerOnlineSysten()
        } catch (e: IOException) {
            Snackbar.make(requireView(), e.message!!, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun buildLocationRequest() {
        if(locationRequest==null)
        {
            locationRequest = LocationRequest()
            locationRequest!!.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            locationRequest!!.setFastestInterval(15000)
            locationRequest!!.interval = 10000
            locationRequest!!.setSmallestDisplacement(50f)

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap!!
        //Request Permission
        Dexter.withContext(requireContext()!!)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object: PermissionListener{
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    //Enable button first
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        Snackbar.make(root_layout,getString(R.string.permission_require),Snackbar.LENGTH_LONG).show()
                        return
                    }
                    mMap.isMyLocationEnabled = true
                    mMap.uiSettings.isMyLocationButtonEnabled=true
                    mMap.setOnMyLocationClickListener {



                        fusedLocationProviderClient!!.lastLocation
                            .addOnFailureListener{ e->
                                Toast.makeText(context!!,e.message,Toast.LENGTH_SHORT).show()

                            }.addOnSuccessListener { location ->
                                val userLatLng=LatLng(location.latitude,location.longitude)
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng,18f))
                            }
                        true
                    }
                    //layout

                    val locationButton=(mapFragment.requireView()!!
                        .findViewById<View>("1".toInt())!!
                        .parent!! as View).findViewById<View>("2".toInt())
                    val params=locationButton.layoutParams as RelativeLayout.LayoutParams
                    params.addRule(RelativeLayout.ALIGN_PARENT_TOP,0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE)
                    params.bottomMargin=50

                    //Location
                    buildLocationRequest()

                    buildLocationCallBack()

                    updateLocation()

                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {

                }
                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(context!!,"Permission "+p0!!.permissionName+" was denied",Toast.LENGTH_SHORT).show()
                }

            }).check()
        try{
            val success = googleMap.setMapStyle(context?.let {
                MapStyleOptions.loadRawResourceStyle(
                    it,R.raw.uber_maps_style)
            })
            if(!success)
                Log.e("EDMT_ERROR","Style parsing message")
        }catch (e:Resources.NotFoundException){
           Log.e("EDMT_ERROR", e.message.toString())
        }
        Snackbar.make(mapFragment.requireView(),"You are online!!!",Snackbar.LENGTH_SHORT).show()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public fun onDriverRequestReceived(event:DriverRequestReceived)
    {
        driverRequestReceived = event
        if (ActivityCompat.checkSelfPermission(
                requireContext()!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext()!!,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Snackbar.make(  requireView(),R.string.permission_require,Snackbar.LENGTH_LONG).show()
            return
        }
        fusedLocationProviderClient!!.lastLocation
            .addOnFailureListener{ e->
                Snackbar.make(requireView(),e.message!!,Snackbar.LENGTH_LONG).show()
            }
            .addOnSuccessListener { location->
                compositeDisposable.add(iGoogleAPI.getDirection("driving" ,
                    "less_driving",
                    StringBuilder()
                        .append(location.latitude)
                        .append(",")
                        .append(location.longitude)
                        .toString(),event.pickupLocation,
                    "AIzaSyCDz6LWeUuWz7f4OYPeKkFIJy5QCgxyW84")
                !!.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe{ returnResult->
                        Log.d("API_RETURN",returnResult)
                        try{
                            val jsonObject = JSONObject(returnResult)
                            val jsonArray=jsonObject.getJSONArray("routes");
                            for(i in 0 until jsonArray.length())
                            {
                                val route=jsonArray.getJSONObject(i)
                                val poly=route.getJSONObject("overview_polyline")
                                val polyline=poly.getString("points")
                                polylineList=Common.decodePoly(polyline)

                            }

                            polyLineOptions= PolylineOptions()

                            polyLineOptions!!.color(Color.GRAY)
                            polyLineOptions!!.width(12f)

                            polyLineOptions!!.startCap(SquareCap())
                            polyLineOptions!!.jointType(JointType.ROUND)
                            polyLineOptions!!.addAll(polylineList!!)
                            grayPolyLine=mMap.addPolyline(polyLineOptions!!)

                            blackPolylineOptions= PolylineOptions()
                            blackPolylineOptions!!.color(Color.BLACK)
                            blackPolylineOptions!!.width(5f)
                            blackPolylineOptions!!.startCap(SquareCap())
                            blackPolylineOptions!!.jointType(JointType.ROUND)
                            blackPolylineOptions!!.addAll(polylineList!!)
                            blackPolyLine=mMap.addPolyline(blackPolylineOptions!!)

                            //Animator
                            val valueAnimator= ValueAnimator.ofInt(0,100)
                            valueAnimator.duration=1100
                            valueAnimator.repeatCount= ValueAnimator.INFINITE
                            valueAnimator.interpolator= LinearInterpolator()
                            valueAnimator.addUpdateListener { value->
                                val points=grayPolyLine!!.points
                                val percentValue=value.animatedValue.toString().toInt()
                                val size=points.size
                                val newpoints=(size*(percentValue/100.0f)).toInt()
                                val p=points.subList(0,newpoints)
                                blackPolyLine!!.points=(p)
                            }
                            valueAnimator.start()

                            val origin=LatLng(location.latitude,location.longitude)
                            val destination =LatLng(event.pickupLocation!!.split(",")[0].toDouble(),
                                event.pickupLocation!!.split(",")[1].toDouble())

                            val latLngBound= LatLngBounds.Builder().include(origin)
                                .include(destination)
                                .build()

                            //Add car icon for origin
                            val objects=jsonArray.getJSONObject(0)
                            val legs=objects.getJSONArray("legs")
                            val legsObject=legs.getJSONObject(0)

                            val time=legsObject.getJSONObject("duration")
                            val duration =time.getString("text")

                            val distanceEstimate=legsObject.getJSONObject("distance")
                            val distance =distanceEstimate.getString("text")


                            txt_estimate_time.setText(duration)
                            txt_estimate_distance.setText(distance)

                            mMap.addMarker( MarkerOptions().position(destination).icon(BitmapDescriptorFactory.defaultMarker())
                                .title( "Pickup Location"))

                            createGeoFirePickupLocation(event.key,destination)

                            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBound,160))
                            mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.cameraPosition!!.zoom-1))


                            //Display Layout
                            chip_decline.visibility=View.VISIBLE
                            layout_accept.visibility=View.VISIBLE

                            //CountDown
                            countDownEvent = Observable.interval(100,TimeUnit.MILLISECONDS)
                                .observeOn(AndroidSchedulers.mainThread())
                                .doOnNext{x->
                                    circularProgressBar.progress +=1f
                                }
                                .takeUntil{aLong->aLong=="100".toLong()}//10 sec
                                .doOnComplete {
                                    createTripPlan(event,duration,distance)
                                }.subscribe()


                        }catch (e: Exception){
                            Toast.makeText(requireContext()!!,e.message!!,Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
    }

    private fun createGeoFirePickupLocation(key: String?, destination: LatLng) {
        val ref=FirebaseDatabase.getInstance()
            .getReference(Common.TRIP_PICKUP_REF)
        pickupGeoFire= GeoFire(ref)
        pickupGeoFire!!.setLocation(key, GeoLocation(destination.latitude,destination.longitude),
            {key1,error->
                if(error!=null)
                    Snackbar.make(root_layout,error.message,Snackbar.LENGTH_LONG).show()
                else
                    Log.d("Janbahon",key1+" was create success")
            })

    }

    private fun createTripPlan(event: DriverRequestReceived, duration: String, distance: String) {
        setLayoutProcess(true)

        FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset")
            .addListenerForSingleValueEvent(object :ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val timeOffset = snapshot.getValue(Long::class.java)
                    val estimateTimeInMS=System.currentTimeMillis()+timeOffset!!
                    var timeText=SimpleDateFormat("dd/MM/yyyy HH:mm aa")
                        .format(estimateTimeInMS)

                    //Loading rider info
                    FirebaseDatabase.getInstance()
                        .getReference(Common.RIDER_INFO)
                        .child(event!!.key!!)
                        .addListenerForSingleValueEvent(object :ValueEventListener{
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if(snapshot.exists()){
                                    val riderModel = snapshot.getValue(RiderModel::class.java)

                                    if (ActivityCompat.checkSelfPermission(
                                            requireContext(),
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                                            requireContext(),
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        Snackbar.make(mapFragment.requireView()!!,requireContext().getString(R.string.permission_require),Snackbar.LENGTH_LONG).show()
                                        return
                                    }
                                    fusedLocationProviderClient!!.lastLocation
                                        .addOnFailureListener { e->
                                            Snackbar.make(mapFragment.requireView()!!,e.message!!,Snackbar.LENGTH_LONG).show()
                                        }
                                        .addOnSuccessListener { location->
                                            val tripPlanModel = TripPlanModel()
                                            tripPlanModel.driver = FirebaseAuth.getInstance().currentUser!!.uid
                                            tripPlanModel.rider = event!!.key
                                            tripPlanModel.driverInfoModel = Common.currentUser
                                            tripPlanModel.riderModel = riderModel
                                            tripPlanModel.origin = event.pickupLocation
                                            tripPlanModel.originString = event.pickupLocationString
                                            tripPlanModel.destination = event.destinationLocation
                                            tripPlanModel.destinationString = event.destinationLocationString
                                            tripPlanModel.distancePickup = distance
                                            tripPlanModel.durationPickup = duration
                                            tripPlanModel.currentLat = location.latitude
                                            tripPlanModel.currentLng = location.longitude

                                            //new info
                                            tripPlanModel.timeText=timeText
                                            tripPlanModel.distanceText=event.distanceText!!
                                            tripPlanModel.durationText=event.durationText!!
                                            tripPlanModel.distanceValue=event.distanceValue
                                            tripPlanModel.durationValue=event.durationValue
                                            tripPlanModel.totalFee=event.totalFee


                                            tripNumberId = Common.createUniqueTripNumber(timeOffset)

                                            FirebaseDatabase.getInstance().getReference(Common.TRIP)
                                                .child(tripNumberId!!)
                                                .setValue(tripPlanModel)
                                                .addOnFailureListener { e ->
                                                    Snackbar.make(mapFragment.requireView()!!,e!!.message!!,Snackbar.LENGTH_LONG).show()
                                                }
                                                .addOnSuccessListener { aVoid ->
                                                    txt_rider_name.setText(riderModel!!.firstName)
                                                    txt_rider_id.setText(riderModel!!.id)
                                                    txt_rider_number.setText(riderModel!!.phoneNumber)
                                                    txt_start_uber_estimate_distance.setText(distance)
                                                    txt_start_uber_estimate_time.setText(duration)

                                                    setOfflineModelForDriver(event,duration,distance)
                                                }
                                        }
                                }else{
                                    Snackbar.make(mapFragment.requireView()!!,requireContext().getString(R.string.rider_not_found)+" "+event!!.key!!,Snackbar.LENGTH_LONG).show()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Snackbar.make(mapFragment.requireView()!!,error.message,Snackbar.LENGTH_LONG).show()
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Snackbar.make(mapFragment.requireView()!!,error.message,Snackbar.LENGTH_LONG).show()
                }
            })
    }

    private fun setOfflineModelForDriver(
        event: DriverRequestReceived,
        duration: String,
        distance: String
    ) {

        UserUtils.sendAcceptRequestToRider(mapFragment.view,requireContext(),event.key!!,tripNumberId)

        if(currentUserRef != null)currentUserRef!!.removeValue()

        setLayoutProcess(false)
        layout_accept.visibility = View.GONE
        layout_start_uber.visibility = View.VISIBLE

        isTripStart = true

    }

    private fun setLayoutProcess(process: Boolean) {
        var color  = -1
        if(process){
            color = ContextCompat.getColor(requireContext(),R.color.dark_grey)
            circularProgressBar.indeterminateMode = true
            txt_rating.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.ic_baseline_star_24_dark_grey,0)
        }else{
            color = ContextCompat.getColor(requireContext(),android.R.color.white)
            circularProgressBar.indeterminateMode = false
            circularProgressBar.progress = 0f
            txt_rating.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.ic_baseline_star_24,0)
        }

        txt_estimate_time.setTextColor(color)
        txt_estimate_distance.setTextColor(color)
        txt_rating.setTextColor(color)
        txt_type_uber.setTextColor(color)
        ImageViewCompat.setImageTintList(img_round, ColorStateList.valueOf(color))
    }


    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onNotifyRider(event: NotifyRiderEvent) {
        layout_notify_rider!!.visibility = View.VISIBLE
        progress_notify.max = Common.WAIT_TIME_IN_MIN * 60
        val countDownTimer=object : CountDownTimer((progress_notify.max*1000).toLong(),1000){
            override fun onTick(l: Long) {

                progress_notify.progress +=1

                txt_notify_rider.text= String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(1)-TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(1)),
                    TimeUnit.MILLISECONDS.toSeconds(1)-TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(1)))

            }

            override fun onFinish() {
                Snackbar.make(root_layout,getString(R.string.time_over),Snackbar.LENGTH_LONG).show()
            }

        }.start()
    }


}