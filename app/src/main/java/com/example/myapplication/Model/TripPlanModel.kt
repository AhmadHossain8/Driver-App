package com.example.myapplication.Model

class TripPlanModel {
    var rider:String?=null
    var driver:String?=null
    var driverInfoModel:DriverInfoModel?=null
    var riderModel:RiderModel?=null
    var origin:String?=null
    var originString:String?=null
    var destination:String?=null
    var destinationString:String?=null
    var distancePickup:String?=null
    var durationPickup:String?=null
    var distanceDestination:String?=null
    var durationDestination:String?=null
    var currentLat:Double = -1.0
    var currentLng:Double = -1.0
    var isDone = false
    var isCancel = false
    //new data
    var totalFee=0.0
    var distanceValue=0
    var durationValue=0
    var distanceText:String=""
    var durationText:String=""
    var timeText:String=""


}