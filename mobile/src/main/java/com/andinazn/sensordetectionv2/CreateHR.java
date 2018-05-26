package com.andinazn.sensordetectionv2;

import java.util.Date;

/**
 * Created by Amira Maulina on 4/7/2018.
 */

public class CreateHR {
    public CreateHR()
    {}

    public int tmpHR;
    public String Date;
    public String Time, Timestamp;

    public CreateHR(int tmpHR, String Date, String Time, String Timestamp)
    {
        this.tmpHR = tmpHR;
        this.Date = Date;
        this. Time = Time;
        this.Timestamp = Timestamp;
    }
}