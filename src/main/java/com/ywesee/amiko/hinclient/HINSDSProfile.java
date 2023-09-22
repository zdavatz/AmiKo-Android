package com.ywesee.amiko.hinclient;

import com.ywesee.amiko.DoctorStore;
import com.ywesee.amiko.Operator;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class HINSDSProfile {
    public String loginName;
    public String email;
    public String firstName;
    public String middleName;
    public String lastName;
    public String gender; // "M" / "F"
    public Date dateOfBirth;
    public String address;
    public String postalCode;
    public String city;
    public String countryCode;
    public String phoneNr;
    public String gln;
    public String verificationLevel;

    public HINSDSProfile(JSONObject json) throws JSONException, ParseException {
        this.loginName = json.getString("loginName");
        this.email = json.getString("email");
        JSONObject contact = json.getJSONObject("contactId");
        this.firstName = contact.getString("firstName");
        this.middleName = contact.getString("middleName");
        this.lastName = contact.getString("lastName");
        this.gender = contact.getString("gender"); // "M" / "F"
        String dobString = contact.getString("dateOfBirth");

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(tz);
        this.dateOfBirth = df.parse(dobString);
        this.address = contact.getString("address");
        this.postalCode = contact.getString("postalCode");
        this.city = contact.getString("city");
        this.countryCode = contact.getString("countryCode");
        this.phoneNr = contact.getString("phoneNr");
        this.gln = contact.getString("gln");
        this.verificationLevel = contact.getString("verificationLevel");
    }

    public void mergeToOperator(DoctorStore doctorStore) {
        if (doctorStore.email == null || doctorStore.email.length() == 0) {
            doctorStore.email = this.email;
        }
        if (doctorStore.surname == null || doctorStore.surname.length() == 0) {
            doctorStore.surname = this.lastName;
        }
        if (doctorStore.name == null || doctorStore.name.length() == 0) {
            doctorStore.name = this.firstName;
        }
        if (doctorStore.street == null || doctorStore.street.length() == 0) {
            doctorStore.street = this.address;
        }
        if (doctorStore.zip == null || doctorStore.zip.length() == 0) {
            doctorStore.zip = this.postalCode;
        }
        if (doctorStore.city == null || doctorStore.city.length() == 0) {
            doctorStore.city = this.city;
        }
        if (doctorStore.phone == null || doctorStore.phone.length() == 0) {
            doctorStore.phone = this.phoneNr;
        }
        if (doctorStore.gln == null || doctorStore.gln.length() == 0) {
            doctorStore.gln = this.gln;
        }
    }
}
