package com.example.myapplication


import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import com.example.myapplication.Model.DriverInfoModel
import com.example.myapplication.Utils.UserUtils
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.sql.Driver
import java.util.*
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.activity_splash_screen.*

class LoginActivity : AppCompatActivity() {
    companion object{
        private val LOGIN_REQUEST_CODE = 7171
    }

    private lateinit var providers: List<AuthUI.IdpConfig>
    private lateinit var firebaseAuth : FirebaseAuth
    private lateinit var listener : FirebaseAuth.AuthStateListener

    private lateinit var database:FirebaseDatabase
    private lateinit var driverInfoRef:DatabaseReference


    override fun onStart() {
        super.onStart()
        delaySplashSreen();
    }

    override fun onStop() {
        if(firebaseAuth != null && listener != null )firebaseAuth.removeAuthStateListener(listener)
        super.onStop()
    }


    private fun delaySplashSreen() {
        Completable.timer(3,TimeUnit.SECONDS,AndroidSchedulers.mainThread())
            .subscribe({
                firebaseAuth.addAuthStateListener(listener)
            })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        init()
    }

    private fun init(){
        database = FirebaseDatabase.getInstance()
        driverInfoRef = database.getReference(Common.DRIVER_INFO_REFERENCE)
        providers = Arrays.asList(
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        firebaseAuth = FirebaseAuth.getInstance()
        listener = FirebaseAuth.AuthStateListener { myFirebaseAuth ->
            val user = myFirebaseAuth.currentUser

            if(user != null){
                FirebaseMessaging.getInstance().token
                    .addOnFailureListener { e-> Toast.makeText(this@LoginActivity,e.message,Toast.LENGTH_LONG).show() }
                    .addOnSuccessListener { instanceIdResult ->
                        Log.d("TOKEN",instanceIdResult.toString())
                        UserUtils.updateToken(this@LoginActivity,instanceIdResult.toString())
                    }
                checkUserFromFirebase()
            }
            else{
                showloginLayout()
            }

        }

    }

    private fun checkUserFromFirebase() {
        driverInfoRef
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object :ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {
                    Toast.makeText(this@LoginActivity,p0.message,Toast.LENGTH_SHORT).show()
                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if(dataSnapshot.exists()){
                        val model = dataSnapshot.getValue(DriverInfoModel::class.java)
                        goToHomeActivity(model)
                    //Toast.makeText(this@LoginActivity,"User already register",Toast.LENGTH_SHORT).show()
                    }else{
                        showRegisterLayout()
                    }
                }
            })
    }

    private fun goToHomeActivity(model: DriverInfoModel?) {
        Common.currentUser = model
        startActivity(Intent(this,DriverHomeActivity::class.java))
        finish()
    }

    private fun showRegisterLayout() {
        val builder = AlertDialog.Builder(this,R.style.DialogTheme)
        val itemView = LayoutInflater.from(this).inflate(R.layout.layout_register,null)

        val edt_first_name = itemView.findViewById<View>(R.id.edt_first_name) as TextInputEditText
        val edt_last_name = itemView.findViewById<View>(R.id.edt_last_name) as TextInputEditText
        val edt_phone_number = itemView.findViewById<View>(R.id.edt_phone_number) as TextInputEditText
        val edt_vehicle_type = itemView.findViewById<View>(R.id.edt_vehicle_type) as TextInputEditText
        val edt_vehicle_name = itemView.findViewById<View>(R.id.edt_vehicle_name) as TextInputEditText
        val edt_vehicle_model = itemView.findViewById<View>(R.id.edt_vehicle_model) as TextInputEditText


        val btn_continue = itemView.findViewById<View>(R.id.btn_register) as Button

        if(FirebaseAuth.getInstance().currentUser!!.phoneNumber != null &&
                !TextUtils.isDigitsOnly(FirebaseAuth.getInstance().currentUser!!.phoneNumber))
            edt_phone_number.setText(FirebaseAuth.getInstance().currentUser!!.phoneNumber)

        builder.setView(itemView)
        val dialog = builder.create()
        dialog.show()

        btn_continue.setOnClickListener {
            if(TextUtils.isDigitsOnly(edt_first_name.text.toString())){
                Toast.makeText(this@LoginActivity,"Enter First Name",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else if(TextUtils.isDigitsOnly(edt_last_name.text.toString())){
                Toast.makeText(this@LoginActivity,"Enter Last Name",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else if(TextUtils.isDigitsOnly(edt_phone_number.text.toString())){
                Toast.makeText(this@LoginActivity,"Enter phone number",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else if(TextUtils.isDigitsOnly(edt_vehicle_type.text.toString())){
                Toast.makeText(this@LoginActivity,"Enter vehicle type",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else if(TextUtils.isDigitsOnly(edt_vehicle_name.text.toString())){
                Toast.makeText(this@LoginActivity,"Enter vehicle name",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else if(TextUtils.isDigitsOnly(edt_vehicle_model.text.toString())){
                Toast.makeText(this@LoginActivity,"Enter vehicle model",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else{
                val model = DriverInfoModel()
                model.firstName = edt_first_name.text.toString()
                model.lastName = edt_last_name.text.toString()
                model.phoneNumber = edt_phone_number.text.toString()
                model.vehicle_name = edt_vehicle_name.text.toString()
                model.vehicle_type = edt_vehicle_type.text.toString()
                model.vehicle_model = edt_vehicle_model.text.toString()
                model.rating = 0.0
                model.id = FirebaseAuth.getInstance().currentUser!!.uid
                driverInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
                    .setValue(model)
                    .addOnFailureListener{ e ->
                        Toast.makeText(this@LoginActivity,""+e.message,Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        progress_bar.visibility = View.GONE
                    }
                    .addOnSuccessListener {
                        Toast.makeText(this@LoginActivity,"Register Done",Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        goToHomeActivity(model)
                        progress_bar.visibility = View.GONE
                    }
            }
        }
    }


    private fun showloginLayout() {
        val authMethodPickerLayout = AuthMethodPickerLayout.Builder(R.layout.activity_login)
            .setPhoneButtonId(R.id.btn_phone_sign_in)
            .setGoogleButtonId(R.id.btn_google_sign_in)
            .build();

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(authMethodPickerLayout)
                .setTheme(R.style.Theme_MyApplication)
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .build(),
            LOGIN_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == LOGIN_REQUEST_CODE){
            val response = IdpResponse.fromResultIntent(data)
            if(resultCode == Activity.RESULT_OK){
                val user = FirebaseAuth.getInstance().currentUser
            }else{
                Toast.makeText(this@LoginActivity,""+response!!.error!!.message,Toast.LENGTH_SHORT).show()
            }
        }
    }
}