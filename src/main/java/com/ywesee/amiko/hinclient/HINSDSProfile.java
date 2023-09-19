package com.ywesee.amiko.hinclient;

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
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
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

    public void mergeToOperator(Operator operator) {
        if (operator.emailAddress == null || operator.emailAddress.length() == 0) {
            operator.emailAddress = this.email;
        }
        if (operator.familyName == null || operator.familyName.length() == 0) {
            operator.familyName = this.lastName;
        }
        if (operator.givenName == null || operator.givenName.length() == 0) {
            operator.givenName = this.firstName;
        }
        if (operator.postalAddress == null || operator.postalAddress.length() == 0) {
            operator.postalAddress = this.address;
        }
        if (operator.zipCode == null || operator.zipCode.length() == 0) {
            operator.zipCode = this.postalCode;
        }
        if (operator.city == null || operator.city.length() == 0) {
            operator.city = this.city;
        }
        if (operator.phoneNumber == null || operator.phoneNumber.length() == 0) {
            operator.phoneNumber = this.phoneNr;
        }
        if (operator.gln == null || operator.gln.length() == 0) {
            operator.gln = this.gln;
        }
    }
}
