package de.hallo5000.datatypes;

import org.kapott.hbci.GV_Result.GVRKUms;

public class Transaction {

    private final GVRKUms.UmsLine transaction;
    private final String service;
    private boolean checked;

    public Transaction(String customerId, GVRKUms.UmsLine transaction, String service){
        this.transaction = transaction;
        this.service = service;
        checked = false;
    }

    public GVRKUms.UmsLine getTransaction() {
        return transaction;
    }

    public String getService() {
        return service;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
