package de.hallo5000.datatypes;

import java.util.HashMap;

public class Job {

    private final String type;
    private final HashMap<String, String> params;

    public Job(String type, HashMap<String, String> params){
        this.type = type;
        this.params = params;
    }

    public void setParam(String key, String value){
        params.put(key, value);
    }

    public HashMap<String, String> getParams(){
        return params;
    }

    public String getType() {
        return type;
    }
}
