/*
 * SPDX-FileCopyrightText: 2013 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.api;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.telephony.CellIdentity;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.READ_PHONE_STATE;

/**
 * Utility class to support backends that use Cells for geolocation.
 * <p/>
 * Due to changes in APIs for cell retrieval, this class will only work on Android 4.2+
 * Support for earlier Android versions might be added later...
 */
@SuppressWarnings({"JavaReflectionMemberAccess", "unused", "WeakerAccess", "deprecation"})
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
@SuppressLint("MissingPermission")
public class CellBackendHelper extends AbstractBackendHelper {
    private final Listener listener;
    private final TelephonyManager telephonyManager;
    private Set<Cell> cells = new HashSet<>();
    private PhoneStateListener phoneStateListener;
    private boolean supportsCellInfoChanged = true;

    public static final int MIN_UPDATE_INTERVAL = 30 * 1000;
    public static final int FALLBACK_UPDATE_INTERVAL = 5 * 60 * 1000;
    private final static int MAX_AGE = 300000;
    private long lastScan = 0;
    private boolean forceNextUpdate = false;

    /**
     * Create a new instance of {@link CellBackendHelper}. Call this in
     * {@link LocationBackendService#onCreate()}.
     *
     * @throws IllegalArgumentException if either context or listener is null.
     * @throws IllegalStateException    if android version is below 4.2
     */
    public CellBackendHelper(Context context, Listener listener) {
        super(context);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
            throw new IllegalStateException("Requires Android 4.2+");
        if (listener == null)
            throw new IllegalArgumentException("listener must not be null");
        this.listener = listener;
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    private int getMcc() {
        try {
            return Integer.parseInt(telephonyManager.getNetworkOperator().substring(0, 3));
        } catch (Exception e) {
            return -1;
        }
    }

    private int getMnc() {
        try {
            return Integer.parseInt(telephonyManager.getNetworkOperator().substring(3));
        } catch (Exception e) {
            return -1;
        }
    }

    private static Cell.CellType getCellType(int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return Cell.CellType.GSM;
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return Cell.CellType.UMTS;
            case TelephonyManager.NETWORK_TYPE_LTE:
                return Cell.CellType.LTE;
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return Cell.CellType.CDMA;
        }
        return null;
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    private Cell parseCellInfo(CellInfo info) {
        try {
            if (info instanceof CellInfoGsm) {
                CellIdentityGsm identity = ((CellInfoGsm) info).getCellIdentity();
                if (identity.getMcc() == Integer.MAX_VALUE) return null;
                CellSignalStrengthGsm strength = ((CellInfoGsm) info).getCellSignalStrength();
                return new Cell(Cell.CellType.GSM, identity.getMcc(), identity.getMnc(),
                        identity.getLac(), identity.getCid(), -1, strength.getDbm());
            } else if (info instanceof CellInfoCdma) {
                CellIdentityCdma identity = ((CellInfoCdma) info).getCellIdentity();
                CellSignalStrengthCdma strength = ((CellInfoCdma) info).getCellSignalStrength();
                return new Cell(Cell.CellType.CDMA, getMcc(), identity.getSystemId(),
                        identity.getNetworkId(), identity.getBasestationId(), -1, strength.getDbm());
            } else {
                return parceCellInfo18(info);
            }
        } catch (Exception ignored) {
            Log.d(TAG, "Failed to parse cell info " + info, ignored);
        }
        return null;
    }

    @SuppressWarnings({"ChainOfInstanceofChecks", "deprecation"})
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private Cell parceCellInfo18(CellInfo info) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) return null;
        if (info instanceof CellInfoWcdma) {
            CellIdentityWcdma identity = ((CellInfoWcdma) info).getCellIdentity();
            if (identity.getMcc() == Integer.MAX_VALUE) return null;
            CellSignalStrengthWcdma strength = ((CellInfoWcdma) info).getCellSignalStrength();
            return new Cell(Cell.CellType.UMTS, identity.getMcc(), identity.getMnc(),
                    identity.getLac(), identity.getCid(), identity.getPsc(), strength.getDbm());
        } else if (info instanceof CellInfoLte) {
            CellIdentityLte identity = ((CellInfoLte) info).getCellIdentity();
            if (identity.getMcc() == Integer.MAX_VALUE) return null;
            CellSignalStrengthLte strength = ((CellInfoLte) info).getCellSignalStrength();
            return new Cell(Cell.CellType.LTE, identity.getMcc(), identity.getMnc(),
                    identity.getTac(), identity.getCi(), identity.getPci(), strength.getDbm());
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private Cell parseCellInfo(android.telephony.NeighboringCellInfo info) {
        try {
            if (getCellType(info.getNetworkType()) != Cell.CellType.GSM) return null;
            return new Cell(Cell.CellType.GSM, getMcc(), getMnc(), info.getLac(), info.getCid(),
                    info.getPsc(), info.getRssi());
        } catch (Exception ignored) {
        }
        return null;
    }

    private void onCellsChanged(List<CellInfo> cellInfo) {
        lastScan = System.currentTimeMillis();
        if (loadCells(cellInfo)) {
            listener.onCellsChanged(getCells());
        } else {
            Log.d(TAG, "No change in Cell networks");
        }
    }

    /**
     * This will fix empty MNC since Android 9 with 0-prefixed MNCs.
     * Issue: https://issuetracker.google.com/issues/113560852
     */
    private void fixEmptyMnc(Set<Cell> cells) {
        if (Build.VERSION.SDK_INT < 28 || cells == null) {
            return;
        }

        String networkOperator = telephonyManager.getNetworkOperator();

        if (networkOperator.length() < 5 || networkOperator.charAt(3) != '0') {
            return;
        }

        String mnc = networkOperator.substring(3);

        for (Cell cell : cells) {
            if (!cell.info.isRegistered()) {
                continue;
            }

            Object identity = null;

            if (cell.info instanceof CellInfoGsm) {
                identity = ((CellInfoGsm) cell.info).getCellIdentity();
            } else if (cell.info instanceof CellInfoWcdma) {
                identity = ((CellInfoWcdma) cell.info).getCellIdentity();
            } else if (cell.info instanceof CellInfoLte) {
                identity = ((CellInfoLte) cell.info).getCellIdentity();
            }

            if (identity == null) {
                continue;
            }

            String mncString = null;
            if (identity instanceof CellIdentityGsm) {
                mncString = ((CellIdentityGsm) identity).getMncString();
            } else if (identity instanceof CellIdentityWcdma) {
                mncString = ((CellIdentityWcdma) identity).getMncString();
            } else if (identity instanceof CellIdentityLte) {
                mncString = ((CellIdentityLte) identity).getMncString();
            }

            if (mncString == null) {
                cell.mnc = Integer.parseInt(mnc);
            }
        }
    }

    /**
     * This will fix values returned by {@link TelephonyManager#getAllCellInfo()} as described
     * here: https://github.com/mozilla/ichnaea/issues/340
     */
    private void fixShortMncBug(Set<Cell> cells) {
        if (cells == null) return;
        String networkOperator = telephonyManager.getNetworkOperator();
        if (networkOperator.length() != 5) return;
        int realMnc = Integer.parseInt(networkOperator.substring(3));
        boolean theBug = false;
        for (Cell cell : cells) {
            if (cell.info instanceof CellInfoCdma) return;
            if (cell.info.isRegistered()) {
                int infoMnc = cell.getMnc();
                if (infoMnc == (realMnc * 10 + 15)) {
                    theBug = true;
                }
            }
        }
        if (theBug) {
            for (Cell cell : cells) {
                Object identity = null;
                if (cell.info instanceof CellInfoGsm)
                    identity = ((CellInfoGsm) cell.info).getCellIdentity();
                else if (cell.info instanceof CellInfoLte)
                    identity = ((CellInfoLte) cell.info).getCellIdentity();
                else if (Build.VERSION.SDK_INT >= 18 && cell.info instanceof CellInfoWcdma)
                    identity = ((CellInfoWcdma) cell.info).getCellIdentity();
                if (identity == null) continue;
                int mnc = -1;
                if (identity instanceof CellIdentityGsm) {
                    mnc = ((CellIdentityGsm) identity).getMnc();
                } else if (Build.VERSION.SDK_INT >= 18 && identity instanceof CellIdentityWcdma) {
                    mnc = ((CellIdentityWcdma) identity).getMnc();
                } else if (identity instanceof CellIdentityLte) {
                    mnc = ((CellIdentityLte) identity).getMnc();
                }
                if (mnc >= 25 && mnc <= 1005) {
                    cell.mnc = (mnc - 15) / 10;
                }
            }
        }
    }

    private boolean hasCid(long cid) {
        for (Cell cell : cells) {
            if (cell.getCid() == cid) return true;
        }
        return false;
    }

    /**
     * This is to support some broken implementations that do not support {@link TelephonyManager#getAllCellInfo()}
     */
    @SuppressWarnings("ChainOfInstanceofChecks")
    private CellInfo fromCellLocation(CellLocation cellLocation) {
        try {
            if (cellLocation instanceof GsmCellLocation) {
                GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
                CellIdentityGsm identity = CellIdentityGsm.class.getConstructor(int.class, int.class, int.class, int.class)
                        .newInstance(getMcc(), getMnc(), gsmCellLocation.getLac(), gsmCellLocation.getCid());
                CellSignalStrengthGsm strength = CellSignalStrengthGsm.class.newInstance();
                CellInfoGsm info = CellInfoGsm.class.newInstance();
                CellInfoGsm.class.getMethod("setCellIdentity", CellIdentityGsm.class).invoke(info, identity);
                CellInfoGsm.class.getMethod("setCellSignalStrength", CellSignalStrengthGsm.class).invoke(info, strength);
                return info;
            }
            if (cellLocation instanceof CdmaCellLocation) {
                CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) cellLocation;
                CellIdentityCdma identity = CellIdentityCdma.class.getConstructor(int.class, int.class, int.class, int.class, int.class)
                        .newInstance(cdmaCellLocation.getNetworkId(), cdmaCellLocation.getSystemId(), cdmaCellLocation.getBaseStationId(),
                                cdmaCellLocation.getBaseStationLongitude(), cdmaCellLocation.getBaseStationLatitude());
                CellSignalStrengthCdma strength = CellSignalStrengthCdma.class.newInstance();
                CellInfoCdma info = CellInfoCdma.class.newInstance();
                CellInfoCdma.class.getMethod("setCellIdentity", CellIdentityCdma.class).invoke(info, identity);
                CellInfoCdma.class.getMethod("setCellSignalStrength", CellSignalStrengthCdma.class).invoke(info, strength);
                return info;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @SuppressLint({"DiscouragedPrivateApi", "deprecation"})
    private synchronized boolean loadCells(List<CellInfo> cellInfo) {
        Set<Cell> cells = new HashSet<>();
        try {
            if (cellInfo != null) {
                for (CellInfo info : cellInfo) {
                    Cell cell = parseCellInfo(info);
                    if (cell == null) continue;
                    cells.add(cell);
                }
                fixEmptyMnc(cells);
                fixShortMncBug(cells);
            }
            Method getNeighboringCellInfo = TelephonyManager.class.getDeclaredMethod("getNeighboringCellInfo");
            List<android.telephony.NeighboringCellInfo> neighboringCellInfo = (List<android.telephony.NeighboringCellInfo>) getNeighboringCellInfo.invoke(telephonyManager);
            if (neighboringCellInfo != null) {
                for (android.telephony.NeighboringCellInfo info : neighboringCellInfo) {
                    if (!hasCid(info.getCid())) {
                        Cell cell = parseCellInfo(info);
                        if (cell == null) continue;
                        cells.add(cell);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (state == State.DISABLING)
            state = State.DISABLED;
        if (!cells.equals(this.cells) || lastUpdate == 0 || forceNextUpdate) {
            this.cells = cells;
            lastUpdate = System.currentTimeMillis();
            currentDataUsed = false;
            forceNextUpdate = false;
            if (state == State.SCANNING) {
                state = State.WAITING;
            }
            return state != State.DISABLED;
        }
        return false;
    }

    public synchronized Set<Cell> getCells() {
        currentDataUsed = true;
        return new HashSet<>(cells);
    }

    /**
     * Call this in {@link org.microg.nlp.api.LocationBackendService#onOpen()}.
     */
    @Override
    public synchronized void onOpen() {
        super.onOpen();

        if (phoneStateListener == null) {
            Handler mainHandler = new Handler(context.getMainLooper());
            mainHandler.post(() -> {
                phoneStateListener = new PhoneStateListener() {

                    @Override
                    public void onCellInfoChanged(List<CellInfo> cellInfo) {
                        if (cellInfo != null && !cellInfo.isEmpty()) {
                            forceNextUpdate = true;
                            onCellsChanged(cellInfo);
                        } else if (supportsCellInfoChanged) {
                            supportsCellInfoChanged = false;
                            onSignalStrengthsChanged(null);
                        }
                    }

                    @Override
                    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                        if (!supportsCellInfoChanged) {
                            doScan();
                        }
                    }
                };
                registerPhoneStateListener();
            });
        } else {
            registerPhoneStateListener();
        }
    }

    private synchronized void doScan() {
        if (lastScan + MIN_UPDATE_INTERVAL > System.currentTimeMillis()) return;
        if (Build.VERSION.SDK_INT >= 29) {
            telephonyManager.requestCellInfoUpdate(command -> {
                Handler mainHandler = new Handler(context.getMainLooper());
                mainHandler.post(command);
            }, new TelephonyManager.CellInfoCallback() {
                @Override
                public void onCellInfo(List<CellInfo> cellInfo) {
                    forceNextUpdate = true;
                    handleAllCellInfo(cellInfo);
                }
            });
        } else {
            handleAllCellInfo(telephonyManager.getAllCellInfo());
        }
    }

    private void handleAllCellInfo(List<CellInfo> allCellInfo) {
        if ((allCellInfo == null || allCellInfo.isEmpty()) && telephonyManager.getNetworkType() > 0) {
            allCellInfo = new ArrayList<>();
            CellLocation cellLocation = telephonyManager.getCellLocation();
            CellInfo cellInfo = fromCellLocation(cellLocation);
            if (cellInfo != null) allCellInfo.add(cellInfo);
        }
        onCellsChanged(allCellInfo);
    }

    private synchronized void registerPhoneStateListener() {
        try {
            telephonyManager.listen(phoneStateListener,
                    PhoneStateListener.LISTEN_CELL_INFO
                            | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        } catch (Exception e) {
            // Can't listen
            phoneStateListener = null;
        }
    }

    /**
     * Call this in {@link org.microg.nlp.api.LocationBackendService#onClose()}.
     */
    @Override
    public synchronized void onClose() {
        super.onClose();
        if (phoneStateListener != null)
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    @Override
    public synchronized void onUpdate() {
        if (!currentDataUsed && System.currentTimeMillis() - lastUpdate < MAX_AGE) {
            listener.onCellsChanged(getCells());
        } else {
            state = State.SCANNING;
            if (lastScan + FALLBACK_UPDATE_INTERVAL < System.currentTimeMillis()) {
                doScan();
            }
        }
    }

    @Override
    public String[] getRequiredPermissions() {
        return new String[]{READ_PHONE_STATE, ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION};
    }

    public interface Listener {
        void onCellsChanged(Set<Cell> cells);
    }

    public static class Cell {
        private CellInfo info;
        private CellType type;
        private int mcc;
        private int mnc;
        private int lac;
        private long cid;
        private int psc;
        private int signal;

        private Cell(CellInfo info, CellType type, int mcc, int mnc, int lac, long cid, int psc, int signal) {
            this(type, mcc, mnc, lac, cid, psc, signal);
            this.info = info;
        }

        public Cell(CellType type, int mcc, int mnc, int lac, long cid, int psc, int signal) {
            if (type == null)
                throw new IllegalArgumentException("Each cell has an type!");
            this.type = type;
            boolean cdma = type == CellType.CDMA;
            if (mcc < 0 || mcc > 999)
                throw new IllegalArgumentException("Invalid MCC: " + mcc);
            this.mcc = mcc;
            if (cdma ? (mnc < 1 || mnc > 32767) : (mnc < 0 || mnc > 999))
                throw new IllegalArgumentException("Invalid MNC: " + mnc);
            this.mnc = mnc;
            if (lac < 1 || lac > (cdma ? 65534 : 65533))
                throw new IllegalArgumentException("Invalid LAC: " + lac);
            this.lac = lac;
            if (cid < 0)
                throw new IllegalArgumentException("Invalid CID: " + cid);
            this.cid = cid;
            this.psc = psc;
            this.signal = signal;
        }

        /**
         * @return RSCP for UMTS, RSRP for LTE, RSSI for GSM and CDMA
         */
        public int getSignal() {
            return signal;
        }

        public CellType getType() {
            return type;
        }

        public int getMcc() {
            return mcc;
        }

        public int getMnc() {
            return mnc;
        }

        public int getLac() {
            return lac;
        }

        public long getCid() {
            return cid;
        }

        public int getPsc() {
            return psc;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Cell cell = (Cell) o;

            if (cid != cell.cid) return false;
            if (lac != cell.lac) return false;
            if (mcc != cell.mcc) return false;
            if (mnc != cell.mnc) return false;
            if (psc != cell.psc) return false;
            if (signal != cell.signal) return false;
            if (type != cell.type) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + mcc;
            result = 31 * result + mnc;
            result = 31 * result + lac;
            result = 31 * result + (int) (cid ^ (cid >>> 32));
            result = 31 * result + psc;
            result = 31 * result + signal;
            return result;
        }

        @Override
        public String toString() {
            return "Cell{" +
                    "type=" + type +
                    ", mcc=" + mcc +
                    ", mnc=" + mnc +
                    ", lac=" + lac +
                    ", cid=" + cid +
                    (psc != -1 ? (", psc=" + psc) : "") +
                    ", signal=" + signal +
                    '}';
        }

        public enum CellType {GSM, UMTS, LTE, CDMA}
    }
}
