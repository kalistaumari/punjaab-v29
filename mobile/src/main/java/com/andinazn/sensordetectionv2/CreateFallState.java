package com.andinazn.sensordetectionv2;

/**
 * Created by Andinazn on 05-May-18.
 */

public class CreateFallState {
    public CreateFallState()
    {}

    public String fallstate;
    public String Date;
    public String Time;
    public String Timestamp;

    public CreateFallState(String fallstate, String Date, String Time, String Timestamp)
    {

        this.fallstate = fallstate;
        this.Date = Date;
        this.Time=Time;
        this.Timestamp=Timestamp;
    }

}
