package edu.gemini.spModel.gemini.gnirs;

import edu.gemini.spModel.gemini.gnirs.GNIRSParams.ReadMode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class GnirsReadoutTime {
    private static final double DHS_WRITE_TIME = 8.5;

    //Maps from the read mode to overhead per coadd
    private static final Map<GNIRSParams.ReadMode, Double> map;

    static{
        final Map<ReadMode, Double> tmp = new HashMap<>();
        tmp.put(ReadMode.VERY_BRIGHT, 0.19);
        tmp.put(ReadMode.BRIGHT, 0.69);
        tmp.put(ReadMode.FAINT, 11.14);
        tmp.put(ReadMode.VERY_FAINT, 22.31);
        map = Collections.unmodifiableMap(tmp);
    }

    public static double getDhsWriteTime () {
        return DHS_WRITE_TIME;
    }

    public static double getReadoutOverhead(final ReadMode readMode, final int coadds) {
        return map.get(readMode) * coadds;
    }

    public static double getReadoutOverheadPerCoadd(final ReadMode readMode) {
        return map.get(readMode);
    }

}
