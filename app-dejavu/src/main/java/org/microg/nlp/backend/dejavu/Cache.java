package org.microg.nlp.backend.dejavu;
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

import android.content.Context;
import android.util.Log;

import org.microg.nlp.backend.dejavu.database.BoundingBox;
import org.microg.nlp.backend.dejavu.database.Database;
import org.microg.nlp.backend.dejavu.database.RfEmitter;
import org.microg.nlp.backend.dejavu.database.RfIdentification;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * All access to the database is done through this cache:
 *
 * When a RF emitter is seen a get() call is made to the cache. If we have a cache hit
 * the information is directly returned. If we have a cache miss we create a new record
 * and populate it with either default information or information from the flash based
 * database (if it exists in the database).
 *
 * Periodically we are asked to sync any new or changed RF emitter information to the
 * database. When that occurs we group all the changes in one database transaction for
 * speed.
 *
 * If an emitter has not been used for a while we will remove it from the cache (only
 * immediately after a sync() operation so the record will be clean). If the cache grows
 * too large we will clear it to conservery RAM (this should never happen). Again the
 * clear operation will only occur after a sync() so any dirty records will be flushed
 * to the database.
 *
 * Operations on the cache are thread safe. However the underlying RF emitter objects
 * that are returned by the cache are not thread safe. So all work on them should be
 * performed either in a single thread or with synchronization.
 */
class Cache {
    private static final int MAX_WORKING_SET_SIZE = 200;
    private static final int MAX_AGE = 30;

    private static final String TAG="DejaVu Cache";

    /**
     * Map (since they all must have different identifications) of
     * all the emitters we are working with.
     */
    private final Map<String, RfEmitter> workingSet = new HashMap<>();
    private Database db;

    Cache(Context context) {
        db = new Database(context);
    }

    /**
     * Release all resources associated with the cache. If the cache is
     * dirty, then it is sync'd to the on flash database.
     */
    public void close() {
        synchronized (this) {
            this.sync();
            this.clear();
            db.close();
            db = null;
        }
    }

    /**
     * Queries the cache with the given RfIdentification.
     *
     * If the emitter does not exist in the cache, it is
     * added (from the database if known or a new "unknown"
     * entry is created).
     *
     * @param id
     * @return the emitter
     *
     */
    public RfEmitter get(RfIdentification id) {
        if (id == null)
            return null;

        synchronized (this) {
            if (db == null)
                return null;
            String key = id.toString();
            RfEmitter rslt = workingSet.get(key);
            if (rslt == null) {
                rslt = db.getEmitter(id);
                if (rslt == null)
                    rslt = new RfEmitter(id);
                workingSet.put(key, rslt);
                Log.i(TAG,"get('"+key+"') - Added to cache.");
            }
            rslt.resetAge();
            return rslt;
        }
    }

    /**
     * Remove all entries from the cache.
     */
    private void clear() {
        synchronized (this) {
            workingSet.clear();
            Log.i(TAG, "clear() - entry");
        }
    }

    /**
     * Updates the database entry for any new or changed emitters.
     * Once the database has been synchronized, cull infrequently used
     * entries. If our cache is still to big after culling, we reset
     * our cache.
     */
    public void sync() {
        synchronized (this) {
            if (db == null)
                return;
            boolean doSync = false;

            // Scan all of our emitters to see
            // 1. If any have dirty data to sync to the flash database
            // 2. If any have been unused long enough to remove from cache

            Set<RfIdentification> agedSet = new HashSet<>();
            for (Map.Entry<String, RfEmitter> e : workingSet.entrySet()) {
                RfEmitter rfE = e.getValue();
                doSync |= rfE.syncNeeded();

                Log.i(TAG,"sync('"+rfE.getRfIdent()+"') - Age: " + rfE.getAge());
                if (rfE.getAge() >= MAX_AGE)
                    agedSet.add(rfE.getRfIdent());
                rfE.incrementAge();
            }

            if (doSync) {
                db.beginTransaction();
                for (Map.Entry<String, RfEmitter> e : workingSet.entrySet()) {
                    e.getValue().sync(db);
                }
                db.endTransaction();
            }

            // Remove aged out items from cache
            for (RfIdentification id : agedSet) {
                String key = id.toString();
                Log.i(TAG,"sync('"+key+"') - Aged out, removed from cache.");
                workingSet.remove(key);
            }

            if (workingSet.size() > MAX_WORKING_SET_SIZE) {
                Log.i(TAG, "sync() - Clearing working set.");
                workingSet.clear();
            }
        }
    }

    public HashSet<RfIdentification> getEmitters(RfEmitter.EmitterType rfType, BoundingBox bb) {
        synchronized (this) {
            if (db == null)
                return null;
            return db.getEmitters(rfType, bb);
        }
    }
}
