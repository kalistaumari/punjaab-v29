package com.andinazn.sensordetectionv2;

/**
 * Created by Andinazn on 05-May-18.
 */

public class CreateHRState {
    public CreateHRState()
    {}

    public String hrstate;
    public String Date;
    public String Time;
    public String Timestamp;
    public int tmpHr;


    public CreateHRState(int tmpHr, String hrstate, String Date, String Time, String Timestamp)
    {
        this.tmpHr = tmpHr;
        this.hrstate = hrstate;
        this.Date = Date;
        this.Time = Time;
        this.Timestamp = Timestamp;
    }
}
