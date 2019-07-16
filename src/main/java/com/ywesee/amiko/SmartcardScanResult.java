package com.ywesee.amiko;

import android.database.SQLException;
import java.io.Serializable;

public class SmartcardScanResult implements Serializable {
    public String familyName;
    public String givenName;
    public String birthDate;
    // KEY_AMK_PAT_GENDER_M or KEY_AMK_PAT_GENDER_F
    public String gender;

    public Patient incompletePatient() {
        Patient p = new Patient();
        p.familyname = this.familyName;
        p.givenname = this.givenName;
        p.birthdate = this.birthDate;
        p.gender = this.gender;
        return p;
    }

    public Patient findExistingPatient(PatientDBAdapter db) throws SQLException {
        String uid = this.incompletePatient().hashValue();
        return db.getPatientWithUniqueId(uid);
    }
}
