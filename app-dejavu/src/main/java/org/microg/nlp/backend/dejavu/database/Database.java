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
 * Created by tfitch on 9/1/17.
 */

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.util.HashSet;

/**
 * Interface to our on flash SQL database. Note that these methods are not
 * thread safe. However all access to the database is through the Cache object
 * which is thread safe.
 */
public class Database extends SQLiteOpenHelper {
    private static final String TAG = "DejaVu DB";

    private static final int VERSION = 3;
    private static final String NAME = "rf.db";

    private static final String TABLE_SAMPLES = "emitters";

    private static final String COL_HASH = "rfHash";        // v3 of database
    private static final String COL_TYPE = "rfType";
    private static final String COL_RFID = "rfID";
    private static final String COL_TRUST = "trust";
    private static final String COL_LAT = "latitude";
    private static final String COL_LON = "longitude";
    private static final String COL_RAD = "radius";          // v1 of database
    private static final String COL_RAD_NS = "radius_ns";    // v2 of database
    private static final String COL_RAD_EW = "radius_ew";    // v2 of database
    private static final String COL_NOTE = "note";

    private SQLiteDatabase database;
    private boolean withinTransaction;
    private boolean updatesMade;

    private SQLiteStatement sqlSampleInsert;
    private SQLiteStatement sqlSampleUpdate;
    private SQLiteStatement sqlAPdrop;

    public class EmitterInfo {
        public double latitude;
        public double longitude;
        public float radius_ns;
        public float radius_ew;
        public long trust;
        public String note;
    }

    public Database(Context context) {
        super(context, NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        database = db;
        withinTransaction = false;
        // Always create version 1 of database, then update the schema
        // in the same order it might occur "in the wild". Avoids having
        // to check to see if the table exists (may be old version)
        // or not (can be new version).
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SAMPLES + "(" +
                COL_RFID + " STRING PRIMARY KEY, " +
                COL_TYPE + " STRING, " +
                COL_TRUST + " INTEGER, " +
                COL_LAT + " REAL, " +
                COL_LON + " REAL, " +
                COL_RAD + " REAL, " +
                COL_NOTE + " STRING);");

        onUpgrade(db, 1, VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2)
            upGradeToVersion2(db);
        if (oldVersion < 3)
            upGradeToVersion3(db);
    }

    private void upGradeToVersion2(SQLiteDatabase db) {
        Log.i(TAG, "upGradeToVersion2(): Entry");
        // Sqlite3 does not support dropping columns so we create a new table with our
        // current fields and copy the old data into it.
        db.execSQL("BEGIN TRANSACTION;");
        db.execSQL("ALTER TABLE " + TABLE_SAMPLES + " RENAME TO " + TABLE_SAMPLES + "_old;");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SAMPLES + "(" +
                COL_RFID + " STRING PRIMARY KEY, " +
                COL_TYPE + " STRING, " +
                COL_TRUST + " INTEGER, " +
                COL_LAT + " REAL, " +
                COL_LON + " REAL, " +
                COL_RAD_NS + " REAL, " +
                COL_RAD_EW + " REAL, " +
                COL_NOTE + " STRING);");

        db.execSQL("INSERT INTO " + TABLE_SAMPLES + "(" +
                COL_RFID + ", " +
                COL_TYPE + ", " +
                COL_TRUST + ", " +
                COL_LAT + ", " +
                COL_LON + ", " +
                COL_RAD_NS + ", " +
                COL_RAD_EW + ", " +
                COL_NOTE +
                ") SELECT " +
                COL_RFID + ", " +
                COL_TYPE + ", " +
                COL_TRUST + ", " +
                COL_LAT + ", " +
                COL_LON + ", " +
                COL_RAD + ", " +
                COL_RAD + ", " +
                COL_NOTE +
                " FROM " + TABLE_SAMPLES + "_old;");
        db.execSQL("DROP TABLE " + TABLE_SAMPLES + "_old;");
        db.execSQL("COMMIT;");
    }

    public void clearDatabase() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_SAMPLES, null, null);
        db.close();
    }

    public long getRowsCount() {
        SQLiteDatabase db = getReadableDatabase();
        long count = DatabaseUtils.queryNumEntries(db, TABLE_SAMPLES);
        db.close();
        return count;
    }

    private void upGradeToVersion3(SQLiteDatabase db) {
        Log.i(TAG, "upGradeToVersion3(): Entry");

        // We are changing our key field to a new text field that contains a hash of
        // of the ID and type. In addition, we are dealing with a Lint complaint about
        // using a string field where we ought to be using a text field.

        db.execSQL("BEGIN TRANSACTION;");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SAMPLES + "_new (" +
                COL_HASH + " TEXT PRIMARY KEY, " +
                COL_RFID + " TEXT, " +
                COL_TYPE + " TEXT, " +
                COL_TRUST + " INTEGER, " +
                COL_LAT + " REAL, " +
                COL_LON + " REAL, " +
                COL_RAD_NS + " REAL, " +
                COL_RAD_EW + " REAL, " +
                COL_NOTE + " TEXT);");

        SQLiteStatement insert = db.compileStatement("INSERT INTO " +
                TABLE_SAMPLES + "_new("+
                COL_HASH + ", " +
                COL_RFID + ", " +
                COL_TYPE + ", " +
                COL_TRUST + ", " +
                COL_LAT + ", " +
                COL_LON + ", " +
                COL_RAD_NS + ", " +
                COL_RAD_EW + ", " +
                COL_NOTE + ") " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);");

        String query = "SELECT " +
                COL_RFID+","+COL_TYPE+","+COL_TRUST+","+COL_LAT+","+COL_LON+","+COL_RAD_NS+","+COL_RAD_EW+","+COL_NOTE+" "+
                "FROM " + TABLE_SAMPLES + ";";

        Cursor cursor = db.rawQuery(query, null);
        try {
            if (cursor.moveToFirst()) {
                do {
                    String rfId = cursor.getString(0);
                    String rftype = cursor.getString(1);
                    if (rftype.equals("WLAN"))
                        rftype = RfEmitter.EmitterType.WLAN_24GHZ.toString();
                    RfIdentification rfid = new RfIdentification(rfId, RfEmitter.typeOf(rftype));
                    String hash = rfid.getUniqueId();

                    // Log.i(TAG,"upGradeToVersion2(): Updating '"+rfId.toString()+"'");

                    insert.bindString(1, hash);
                    insert.bindString(2, rfId);
                    insert.bindString(3, rftype);
                    insert.bindString(4, cursor.getString(2));
                    insert.bindString(5, cursor.getString(3));
                    insert.bindString(6, cursor.getString(4));
                    insert.bindString(7, cursor.getString(5));
                    insert.bindString(8, cursor.getString(6));
                    insert.bindString(9, cursor.getString(7));

                    insert.executeInsert();
                    insert.clearBindings();
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        db.execSQL("DROP TABLE " + TABLE_SAMPLES + ";");
        db.execSQL("ALTER TABLE " + TABLE_SAMPLES + "_new RENAME TO " + TABLE_SAMPLES + ";");
        db.execSQL("COMMIT;");
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
    }

    /**
     * Start an update operation.
     *
     * We make sure we are not already in a transaction, make sure
     * our database is writeable, compile the insert, update and drop
     * statements that are likely to be used, etc. Then we actually
     * start the transaction on the underlying SQL database.
     */
    public void beginTransaction() {
        //Log.i(TAG,"beginTransaction()");
        if (withinTransaction) {
            Log.i(TAG,"beginTransaction() - Already in a transaction?");
            return;
        }
        withinTransaction = true;
        updatesMade = false;
        database = getWritableDatabase();

        sqlSampleInsert = database.compileStatement("INSERT INTO " +
                TABLE_SAMPLES + "("+
                COL_HASH + ", " +
                COL_RFID + ", " +
                COL_TYPE + ", " +
                COL_TRUST + ", " +
                COL_LAT + ", " +
                COL_LON + ", " +
                COL_RAD_NS + ", " +
                COL_RAD_EW + ", " +
                COL_NOTE + ") " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);");

        sqlSampleUpdate = database.compileStatement("UPDATE " +
                TABLE_SAMPLES + " SET "+
                COL_TRUST + "=?, " +
                COL_LAT + "=?, " +
                COL_LON + "=?, " +
                COL_RAD_NS + "=?, " +
                COL_RAD_EW + "=?, " +
                COL_NOTE + "=? " +
                "WHERE " + COL_HASH + "=?;");

        sqlAPdrop = database.compileStatement("DELETE FROM " +
                TABLE_SAMPLES +
                " WHERE " + COL_HASH + "=?;");

        database.beginTransaction();
    }

    /**
     * End a transaction. If we actually made any changes then we mark
     * the transaction as successful. Once marked as successful we
     * end the transaction with the underlying SQL database.
     */
    public void endTransaction() {
        //Log.i(TAG,"endTransaction()");
        if (!withinTransaction) {
            Log.i(TAG,"Asked to end transaction but we are not in one???");
        }

        if (updatesMade) {
            //Log.i(TAG,"endTransaction() - Setting transaction successful.");
            database.setTransactionSuccessful();
        }
        updatesMade = false;
        database.endTransaction();
        withinTransaction = false;
    }

    /**
     * Drop an RF emitter from the database.
     *
     * @param emitter The emitter to be dropped.
     */
    public void drop(RfEmitter emitter) {
        //Log.i(TAG, "Dropping " + emitter.logString() + " from db");

        sqlAPdrop.bindString(1, emitter.getUniqueId());
        sqlAPdrop.executeInsert();
        sqlAPdrop.clearBindings();
        updatesMade = true;
    }

    /**
     * Insert a new RF emitter into the database.
     *
     * @param emitter The emitter to be added.
     */
    public void insert(RfEmitter emitter) {
        Log.i(TAG, "Inserting " + emitter.logString() + " into db");
        sqlSampleInsert.bindString(1, emitter.getUniqueId());
        sqlSampleInsert.bindString(2, emitter.getId());
        sqlSampleInsert.bindString(3, String.valueOf(emitter.getType()));
        sqlSampleInsert.bindString(4, String.valueOf(emitter.getTrust()));
        sqlSampleInsert.bindString(5, String.valueOf(emitter.getLat()));
        sqlSampleInsert.bindString(6, String.valueOf(emitter.getLon()));
        sqlSampleInsert.bindString(7, String.valueOf(emitter.getRadiusNS()));
        sqlSampleInsert.bindString(8, String.valueOf(emitter.getRadiusEW()));
        sqlSampleInsert.bindString(9, emitter.getNote());

        sqlSampleInsert.executeInsert();
        sqlSampleInsert.clearBindings();
        updatesMade = true;
    }

    /**
     * Update information about an emitter already existing in the database
     *
     * @param emitter The emitter to be updated
     */
    public void update(RfEmitter emitter) {
        //Log.i(TAG, "Updating " + emitter.logString() + " in db");

        // the data fields
        sqlSampleUpdate.bindString(1, String.valueOf(emitter.getTrust()));
        sqlSampleUpdate.bindString(2, String.valueOf(emitter.getLat()));
        sqlSampleUpdate.bindString(3, String.valueOf(emitter.getLon()));
        sqlSampleUpdate.bindString(4, String.valueOf(emitter.getRadiusNS()));
        sqlSampleUpdate.bindString(5, String.valueOf(emitter.getRadiusEW()));
        sqlSampleUpdate.bindString(6, emitter.getNote());

        // the Where fields
        sqlSampleUpdate.bindString(7, emitter.getUniqueId());
        sqlSampleUpdate.executeInsert();
        sqlSampleUpdate.clearBindings();
        updatesMade = true;
    }

    /**
     * Return a list of all emitters of a specified type within a bounding box.
     *
     * @param rfType The type of emitter the caller is interested in
     * @param bb The lat,lon bounding box.
     * @return A collection of RF emitter identifications
     */
    public HashSet<RfIdentification> getEmitters(RfEmitter.EmitterType rfType, BoundingBox bb) {
        HashSet<RfIdentification> rslt = new HashSet<>();
        String query = "SELECT " +
                COL_RFID + " " +
                " FROM " + TABLE_SAMPLES +
                " WHERE " + COL_TYPE + "='" + rfType +
                "' AND " + COL_LAT + ">='" + bb.getSouth() +
                "' AND " + COL_LAT + "<='" + bb.getNorth() +
                "' AND " + COL_LON + ">='" + bb.getWest() +
                "' AND " + COL_LON + "<='" + bb.getEast() + "';";

        //Log.i(TAG, "getEmitters(): query='"+query+"'");
        Cursor cursor = getReadableDatabase().rawQuery(query, null);
        try {
            if (cursor.moveToFirst()) {
                do {
                    RfIdentification e = new RfIdentification(cursor.getString(0), rfType);
                    rslt.add(e);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return rslt;
    }

    /**
     * Get all the information we have on an RF emitter
     *
     * @param ident The identification of the emitter caller wants
     * @return A emitter object with all the information we have. Or null if we have nothing.
     */
    public RfEmitter getEmitter(RfIdentification ident) {
        RfEmitter rslt = null;

        String query = "SELECT " +
                COL_TYPE + ", " +
                COL_TRUST + ", " +
                COL_LAT + ", " +
                COL_LON + ", " +
                COL_RAD_NS+ ", " +
                COL_RAD_EW+ ", " +
                COL_NOTE + " " +
                " FROM " + TABLE_SAMPLES +
                " WHERE " + COL_HASH + "='" + ident.getUniqueId() + "';";

        // Log.i(TAG, "getEmitter(): query='"+query+"'");
        Cursor cursor = getReadableDatabase().rawQuery(query, null);
        try {
            if (cursor.moveToFirst()) {
                rslt = new RfEmitter(ident);
                EmitterInfo ei = new EmitterInfo();
                ei.trust = (int) cursor.getLong(1);
                ei.latitude = cursor.getDouble(2);
                ei.longitude = cursor.getDouble(3);
                ei.radius_ns = (float) cursor.getDouble(4);
                ei.radius_ew = (float) cursor.getDouble(5);
                ei.note = cursor.getString(6);
                if (ei.note == null)
                    ei.note = "";
                rslt.updateInfo(ei);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return rslt;
    }
}
