package de.hallo5000.datatypes;

import org.kapott.hbci.GV_Result.GVRKUms;

public class Transaction {

    public enum Service{
        EWASH,
        PRINTER,
        MEMBERSHIP,
        OTHER
    }

    private final String customerId;
    private final GVRKUms.UmsLine transaction;
    private final Service service;
    private boolean checked;

    public Transaction(String customerId, GVRKUms.UmsLine transaction, Service service){
        this.customerId = customerId;
        this.transaction = transaction;
        this.service = service;
        checked = false;
    }

    public String getCustomerId() {
        return customerId;
    }

    public GVRKUms.UmsLine getTransaction() {
        return transaction;
    }

    public Service getService() {
        return service;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
