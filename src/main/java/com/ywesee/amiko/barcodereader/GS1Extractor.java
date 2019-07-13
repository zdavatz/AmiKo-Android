package com.ywesee.amiko.barcodereader;

import com.ywesee.amiko.Utilities;

import java.io.Serializable;

public class GS1Extractor implements Serializable {

    public Result extract(String input) {
        if (input.length() == 0) {
            return null;
        }
        char firstChar = input.charAt(0);
        if (Utilities.isCharacterNumber(firstChar)) {
            return parsePayload(input);
        }

        Result r = new Result();
        for (String substring : input.substring(1).split(new String(new char[]{firstChar}))) {
            r.merge(parsePayload(substring));
        }
        return r;
    }

    public Result parsePayload(String input) {
        if (input.length() < 2) {
            return null;
        }
        Result r = new Result();
        while (input.length() > 2) {
            String first2characters = input.substring(0, 2);
            String rest = input.substring(2);
            if (first2characters.equals("01")) {
                r.gtin = trimZeroPadding(rest.substring(0, 14));
                input = rest.substring(14);
            } else if (first2characters.equals("11")) {
                r.productionDate = convertStringForDate(rest.substring(0, 6));
                input = rest.substring(6);
            } else if (first2characters.equals("15")) {
                r.bestBeforeDate = convertStringForDate(rest.substring(0, 6));
                input = rest.substring(6);
            } else if (first2characters.equals("17")) {
                r.expiryDate = convertStringForDate(rest.substring(0, 6));
                input = rest.substring(6);
            } else if (first2characters.equals("10")) {
                int length = Math.min(rest.length(), 20);
                r.batchOrLotNumber = rest.substring(0, length);
                input = rest.substring(length);
            } else if (first2characters.equals("21")) {
                int length = Math.min(rest.length(), 20);
                r.specialNumber = rest.substring(0, length);
                input = rest.substring(length);
            } else if (first2characters.equals("30")) {
                int length = Math.min(rest.length(), 8);
                r.amount = rest.substring(0, length);
                input = rest.substring(length);
            } else {
                break;
            }
        }
        return r;
    }

    String convertStringForDate(String input) {
        // convert date string from YYMMDD to MM.YYYY
        return input.substring(2, 4) + ".20" + input.substring(0, 2);
    }

    String trimZeroPadding(String input) {
        while (input.length() > 0 && input.charAt(0) == '0') {
            input = input.substring(1);
        }
        return input;
    }

    public class Result implements Serializable {
        public String gtin;
        public String productionDate;
        public String bestBeforeDate;
        public String expiryDate;
        public String batchOrLotNumber;
        public String specialNumber;
        public String amount;

        void merge(Result r) {
            if (r == null) {
                return;
            }
            if (gtin == null) {
                gtin = r.gtin;
            }
            if (productionDate == null) {
                productionDate = r.productionDate;
            }
            if (bestBeforeDate == null) {
                bestBeforeDate = r.bestBeforeDate;
            }
            if (expiryDate == null) {
                expiryDate = r.expiryDate;
            }
            if (batchOrLotNumber == null) {
                batchOrLotNumber = r.batchOrLotNumber;
            }
            if (specialNumber == null) {
                specialNumber = r.specialNumber;
            }
            if (amount == null) {
                amount = r.amount;
            }
        }
    }
}
