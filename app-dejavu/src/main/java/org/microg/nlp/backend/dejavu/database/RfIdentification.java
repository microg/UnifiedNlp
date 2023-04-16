package org.microg.nlp.backend.dejavu.database;

/*
 *    DejaVu - A location provider backend for microG/UnifiedNlp
 *
 *    Copyright (C) 2017 Tod Fitch
 *
 *    This program is Free Software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as
 *    published by the Free Software Foundation, either version 3 of the
 *    License, or (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Created by tfitch on 10/4/17.
 */

import java.math.BigInteger;
import java.security.*;
import android.util.Log;

/**
 * This class forms a complete identification for a RF emitter.
 *
 * All it has are two fields: A rfID string that must be unique within a type
 * or class of emitters. And a rtType value that indicates the type of RF
 * emitter we are dealing with.
 */

public class RfIdentification implements Comparable<RfIdentification>{
    private static final String TAG = "DejaVu RfIdent";

    private final String rfId;
    private final RfEmitter.EmitterType rfType;
    private final String uniqueId;

    RfIdentification(String id, RfEmitter.EmitterType t) {
        rfId = id;
        rfType = t;
        uniqueId = genUniqueId(rfType, rfId);
    }

    public int compareTo(RfIdentification o) {
        return uniqueId.compareTo(o.uniqueId);
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if ( !(o instanceof RfIdentification))
            return false;

        RfIdentification that = (RfIdentification)o;
        return (uniqueId.equals(that.uniqueId));
    }

    public String getRfId() {
        return rfId;
    }

    public RfEmitter.EmitterType getRfType() {
        return rfType;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Return a hash code for Android to determine if we are like
     * some other object. Since we already have a unique ID computed
     * for our database records, use that but turn it into the int
     * expected by Android.
     *
     * @return Int Android hash code
     */
    public int hashCode() {
        return uniqueId.hashCode();
    }

    public String toString() {
        return "rfId=" + rfId + ", rfType=" + rfType;
    }

    /**
     * Generate a unique string for our RF identification. Using MD5 as it
     * ought not have collisions but is relatively cheap to compute. Since
     * we aren't doing cryptography here we need not worry about it being
     * a secure hash.
     *
     * @param rfType The type of emitter
     * @param rfIdent The ID string unique to the type of emitter
     * @return String A unique identification string
     */
    private String genUniqueId(RfEmitter.EmitterType rfType, String rfIdent) {
        String hashtext = rfType + ":" + rfIdent;
        try {
            byte[] bytes = hashtext.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(bytes);
            BigInteger bigInt = new BigInteger(1,digest);
            hashtext = bigInt.toString(16);
            while(hashtext.length() < 32 ){
                hashtext = "0"+hashtext;
            }
        } catch (Exception e) {
            Log.d(TAG, "genUniqueId(): Exception" + e.getMessage());
        }
        return hashtext;
    }

}
