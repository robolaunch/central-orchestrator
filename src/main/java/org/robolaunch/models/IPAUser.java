package org.robolaunch.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class IPAUser implements Serializable {
    private String givenname;
    private String sn;
    private String version;
    private String userpassword;
    private String organization;
    private HashMap<String, String> krbpasswordexpiration;
    private ArrayList<String> mail;
    private String street;

    private String krbprincipalname;

    public IPAUser() {
    }

    public ArrayList<String> getMail() {
        return mail;
    }

    public void setMail(ArrayList<String> mail) {
        this.mail = mail;
    }

    public String getUserpassword() {
        return userpassword;
    }

    public void setUserpassword(String userpassword) {
        this.userpassword = userpassword;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public HashMap<String, String> getKrbpasswordexpiration() {
        return krbpasswordexpiration;
    }

    public void setKrbpasswordexpiration(HashMap<String, String> krbpasswordexpiration) {
        this.krbpasswordexpiration = krbpasswordexpiration;
    }

    public void setPassExpirationDefault() {
        this.krbpasswordexpiration = new HashMap<>();
        this.krbpasswordexpiration.put("__datetime__", "20690724193208Z");
    }

    public String getGivenname() {
        return this.givenname;
    }

    public void setGivenname(String givenname) {
        this.givenname = givenname;
    }

    public String getSn() {
        return this.sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getKrbprincipalname() {
        return krbprincipalname;
    }

    public void setKrbprincipalname(String krbprincipalname) {
        this.krbprincipalname = krbprincipalname;
    }

}
