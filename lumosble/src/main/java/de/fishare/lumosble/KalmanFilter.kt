package de.fishare.lumosble
//
// Created by yaoyu on 2019-03-07
// Copyright (c) 2019 fishare. All rights reserved.
//

 /**
* Create 1-dimensional kalman filter
* @param  {Number} options.R Process noise
* @param  {Number} options.Q Measurement noise
* @param  {Number} options.A State vector
* @param  {Number} options.B Control vector
* @param  {Number} options.C Measurement vector
* @return {KalmanFilter}
*/
public class KalmanFilter{
    var R:Double = 1.0
    var Q:Double = 1.0

    var A:Double = 1.0
    var B:Double = 0.0
    var C:Double = 1.0

    var cov:Double = Double.NaN
    var x:Double = Double.NaN

    constructor()

    constructor(R:Double = 1.0, Q:Double = 1.0){
        this.R = R
        this.Q = Q
    }
    constructor(R:Double = 1.0 , Q:Double = 1.0, A:Double, B:Double, C:Double){
        this.R = R
        this.Q = Q
        this.A = A
        this.B = B
        this.C = C
    }
/**
* Filter a new value
* @param  {Number} z Measurement
* @param  {Number} u Control
* @return {Number}
*/
fun filter(z:Double, u:Double = 0.0) : Double {
    if (x.isNaN()){
        x = (1 / C) * z
        cov = (1 / C) * Q * (1 / C)
    }
    else {
        // Compute prediction
        val predX = predict(u)
        val predCov = uncertainty()

        // Kalman gain
        val K = predCov * C * (1 / ((C * predCov * C) + Q))

        // Correction
        x = predX + K * (z - (C * predX))
        cov = predCov - (K * C * predCov)
    }

    return x
}

/**
* Predict next value
* @param  {Number} [u] Control
* @return {Number}
*/
fun predict(u:Double = 0.0) : Double {
    return (A * x) + (B * u)
}

/**
* Return uncertainty of filter
* @return {Number}
*/
fun uncertainty() : Double {
    return ((A * cov) * A) + R
}

/**
* Return the last filtered measurement
* @return {Number}
*/
fun lastMeasurement() : Double {
    return x
}

/**
* Set measurement noise Q
* @param {Number} noise
*/
fun setMeasurementNoise(noise:Double){
    Q = noise
}

/**
* Set the process noise R
* @param {Number} noise
*/
fun setProcessNoise(noise:Double){
    R = noise
}

}
