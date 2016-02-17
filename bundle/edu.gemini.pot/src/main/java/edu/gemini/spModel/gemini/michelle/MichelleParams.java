// Copyright 2002
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: MichelleParams.java 38186 2011-10-24 13:21:33Z swalker $
//

package edu.gemini.spModel.gemini.michelle;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import static edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_KEY;

import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.type.*;

/**
 * This class provides data types for the Michelle components.
 */
public final class MichelleParams {

    // Make the constructor private.
    private MichelleParams() {}

    /**
     * Chop mode.
     */
    public static enum ChopMode implements SequenceableSpType, DisplayableSpType {
        CHOP("Chop"),
        NDSTARE("ND Stare"),
        STARE("Stare"),
        ;

        private final String displayValue;

        ChopMode(String displayValue) {
            this.displayValue = displayValue;
        }
        public String sequenceValue() {
            return name();
        }

        public String displayValue() {
            return displayValue;
        }

        public String toString() {
            return displayValue;
        }

        public static ChopMode valueOf(String name, ChopMode nvalue) {
            return SpTypeUtil.oldValueOf(ChopMode.class, name, nvalue);
        }

        public static Option<ChopMode> valueOf(String name, Option<ChopMode> nvalue) {
            ChopMode def = nvalue.isEmpty() ? null : nvalue.getValue();
            ChopMode val = SpTypeUtil.oldValueOf(ChopMode.class, name, def);
            Option<ChopMode> none = None.instance();
            return val == null ? none : new Some<ChopMode>(val);
        }
    }

    /**
     * Chop waveform.
     */
    public static enum ChopWaveform implements SequenceableSpType, DisplayableSpType {
        DEEP_WELL("Deep Well"),
        SHALLOW_WELL_NDR("Shallow Well NDR"),
        ;

        private final String displayValue;

        ChopWaveform(String displayValue) {
            this.displayValue = displayValue;
        }
        public String sequenceValue() {
            return name();
        }

        public String displayValue() {
            return displayValue;
        }

        public String toString() {
            return displayValue;
        }

        public static ChopWaveform valueOf(String name, ChopWaveform nvalue) {
            return SpTypeUtil.oldValueOf(ChopWaveform.class, name, nvalue);
        }

        public static Option<ChopWaveform> valueOf(String name, Option<ChopWaveform> nvalue) {
            ChopWaveform def = nvalue.isEmpty() ? null : nvalue.getValue();
            ChopWaveform val = SpTypeUtil.oldValueOf(ChopWaveform.class, name, def);
            Option<ChopWaveform> none = None.instance();
            return val == null ? none : new Some<ChopWaveform>(val);
        }
    }

    /**
     * Grating Order (engineering param).
     */
    public static enum DisperserOrder implements SequenceableSpType, DisplayableSpType {
        ORDER0("0"),
        ;

        private final String displayValue;

        DisperserOrder(String displayValue) {
            this.displayValue = displayValue;
        }
        public String sequenceValue() {
            return name();
        }

        public String displayValue() {
            return displayValue;
        }

        public String toString() {
            return displayValue;
        }

        public static DisperserOrder valueOf(String name, DisperserOrder nvalue) {
            return SpTypeUtil.oldValueOf(DisperserOrder.class, name, nvalue);
        }

        public static Option<DisperserOrder> valueOf(String name, Option<DisperserOrder> nvalue) {
            DisperserOrder def = nvalue.isEmpty() ? null : nvalue.getValue();
            DisperserOrder val = SpTypeUtil.oldValueOf(DisperserOrder.class, name, def);
            Option<DisperserOrder> none = None.instance();
            return val == null ? none : new Some<DisperserOrder>(val);
        }
    }

    /**
     * Mode in which the disperser is used with respect to chopping.
     */
    public static enum DisperserMode {
        CHOP,
        NOD
    }


    /**
     * Michelle Dispersers.
     */
    public static enum Disperser implements StandardSpType {



//        Add the following to the Disperser menu to match the public web page:
//        Imaging
//        Lo-Res 10 microns
//        Med-Res 10 microns
//        Hi-Res 10 microns
//        Lo-Res 20 microns
//        Echelle  <---- is there a way to make this inactive for the general user, but active in the observing
//        database? We will want to commission this later in the semester, but we don't want PIs using it yet.

//        More changes:
//        Low Res 10um Grating -----> Low Res 10um Grating (lowN)
//        Low Res 20um Grating -----> Low Res 20um Grating (lowQ)
//        Med Res 10um Grating -----> Med Res Grating (medN1)
//        High Res 10um Grating -----> Med Res Grating (medN2)
//        Echelle 1500 km/sec -----> Echelle 15 km/sec

// OT-510:
//        Disperser = needs Efficiency =
//
//        Mirror 0.30
//        Low Res 10 Grating (low N) 0.24
//        Low Res 20 Grating (low Q) 0.24
//        Med Res Grating (medN1) 0.90
//        Med Res Grating (medN2) 0.90
//        Echelle 15 km/sec 0.90
//
// Total time needed = Total On-Source Time / Efficiency

        MIRROR(    "Mirror",                      DisperserMode.CHOP, 0.25,    0,     0,  0), //SCT-128 - OT-537
        LOW_RES_10("Low Res 10um Grating (lowN)", DisperserMode.CHOP, 0.24, 4752,  5016,  9.5),
        LOW_RES_20("Low Res 20um Grating (lowQ)", DisperserMode.CHOP, 0.24, 5431,  5339, 19.8),
        MED_RES(   "Med Res Grating (medN1)",     DisperserMode.NOD,  0.9,  4891,  6396, 10.5),
        HIGH_RES(  "Med Res Grating (medN2)",     DisperserMode.NOD,  0.9,  4882,  9430, 10.5),
        ECHELLE(   "Echelle 15 km/sec",           DisperserMode.NOD,  0.9,  4873, 38964, 10.5),
        ;

        /** The default Disperser value **/
        public static final Disperser DEFAULT = MIRROR;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "disperser");

        private final String _displayValue;
        private final DisperserMode _mode;
        private final double _efficiency;
        private final int    _order0Steps;
        private final int    _order1Steps;
        private final double _lamda;

        private Disperser(String displayValue, DisperserMode mode, double efficiency, int order0Steps, int order1Steps, double lamda) {
            _displayValue = displayValue;
            _mode         = mode;
            _efficiency   = efficiency;
            _order0Steps  = order0Steps;
            _order1Steps  = order1Steps;
            _lamda        = lamda;
        }

        public DisperserMode getMode() {
            return _mode;
        }

        public double getEfficiency() {
            return _efficiency;
        }

        public double getLamda() {
            return _lamda;
        }

        public int getOrder0Steps() {
            return _order0Steps;
        }

        public int getOrder1Steps() {
            return _order1Steps;
        }

        public int getSteps(DisperserOrder order) {
            return (order == DisperserOrder.ORDER0) ? _order0Steps : _order1Steps;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String logValue()  {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public String description() {
            return "";
        }

        public String toString() {
            return _displayValue;
        }

        /** Return a Disperser by index **/
        static public Disperser getDisperserByIndex(int index) {
            return SpTypeUtil.valueOf(Disperser.class, index, DEFAULT);
        }

        /** Return a Disperser by name **/
        static public Disperser getDisperser(String name) {
            return getDisperser(name, DEFAULT);
        }

        /** Return a Disperser by name giving a value to return upon error **/
        static public Disperser getDisperser(String name, Disperser nvalue) {
            if ("Low Res 10um Grating".equals(name)) {
                return LOW_RES_10;
            } else if ("High Res 10um Grating".equals(name)) {
                return HIGH_RES;
            }
            return SpTypeUtil.oldValueOf(Disperser.class, name, nvalue);
        }
    }

    /**
     * InjectorPosition (engineering param).
     */
    public static enum Position implements SequenceableSpType, DisplayableSpType {
        IMAGING("Imaging"),
        PARK("Park"),
        SPECTROSCOPY("Spectroscopy"),
        ;

        private final String displayValue;

        Position(String displayValue) { this.displayValue = displayValue; }
        public String sequenceValue() { return name(); }
        public String displayValue() { return displayValue; }
        public String toString() { return displayValue; }

        public static Position valueOf(String name, Position nvalue) {
            return SpTypeUtil.oldValueOf(Position.class, name, nvalue);
        }

        public static Option<Position> valueOf(String name, Option<Position> nvalue) {
            Position def = nvalue.isEmpty() ? null : nvalue.getValue();
            Position val = SpTypeUtil.oldValueOf(Position.class, name, def);
            Option<Position> none = None.instance();
            return val == null ? none : new Some<Position>(val);
        }
    }

    /**
     * Engineering Focal Plane Mask options.
     */
    public static enum EngMask implements StandardSpType {
        CLEAR("Clear Slit Mask", "clear", 21,     48),
        PINHOLE("Pinhole Mask", "pinhole", 0.402, 45),
        ;

        private String displayValue;
        private String logValue;
        private double width;  // arcsec
        private double height; // arcsec

        private EngMask(String displayValue, String log, double width, double height) {
            this.displayValue = displayValue;
            this.logValue     = log;
            this.width        = width;
            this.height       = height;
        }

        public String description() {
            return "";
        }

        public String displayValue() {
            return displayValue;
        }

        // Return the width of the mask in arcsec.
        public double getWidth() {
            return width;
        }

        // Return the height (length) of the mask in arcsec.
        public double getHeight() {
            return height;
        }

        public String logValue() {
            return logValue;
        }

        public String sequenceValue() {
            return name();
        }

        public String toString() {
            return displayValue;
        }

        public static EngMask valueOf(String name, EngMask nvalue) {
            return SpTypeUtil.oldValueOf(EngMask.class, name, nvalue);
        }

        public static Option<EngMask> valueOf(String name, Option<EngMask> nvalue) {
            EngMask def = nvalue.isEmpty() ? null : nvalue.getValue();
            EngMask val = SpTypeUtil.oldValueOf(EngMask.class, name, def);
            Option<EngMask> none = None.instance();
            return val == null ? none : new Some<EngMask>(val);
        }
    }

    /**
     * Masks
     */
    public static enum Mask implements StandardSpType {
//        Focal Plane Mask:
//        1_pixel
//        2_pixels
//        3-Pixels
//        4_pixels
//        6_pixels
//        8_pixels
//
//        - For the naming of the slits.... can we do something like the filters above? The pixel
//          scale is 0.099 arcsec/pixel so perhaps we could make the list read:
//
//        2 pixel (0.198")
//        4 pixel (0.396")
//        etc.
//      OT-548.  Slit  length to 45, all masks slightly wider
        MASK_IMAGING("Imaging", 32.0, 24.0),
        //MASK_1("1 pixel (0.099\")", 0.099, 25.0),
        MASK_1("1 pixel (0.201\")", 0.201, 45.0),
        //MASK_2("2 pixels (0.198\")", 0.198, 25.0),
        MASK_2("2 pixels (0.402\")", 0.402, 45.0),
        //MASK_3("3 pixels (0.297\")", 0.297, 25.0),
        MASK_3("3 pixels (0.603\")", 0.603, 45.0),
        //MASK_4("4 pixels (0.396\")", 0.396, 25.0),
        MASK_4("4 pixels (0.804\")", 0.804, 45.0),
        //MASK_6("6 pixels (0.594\")", 0.594, 25.0),
        MASK_6("6 pixels (1.21\")", 1.21, 45.0),
        //MASK_8("8 pixels (0.792\")", 0.792, 25.0),
        MASK_8("8 pixels (1.61\")", 1.61, 45.0),
        ;

        /** The default Mask value **/
        public static final Mask DEFAULT = MASK_IMAGING;

        private String _displayValue;

        // The width of the mask in arcsec
        private double _width;

        // The height (length) of the mask in arcsec
        private double _height;

        private Mask(String displayValue, double width, double height) {
            _displayValue = displayValue;
            _width = width;
            _height = height;
        }

        // Return the width of the mask in arcsec.
        public double getWidth() {
            return _width;
        }

        // Return the height (length) of the mask in arcsec.
        public double getHeight() {
            return _height;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String logValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public String description() {
            return "";
        }

        public String toString() {
            return _displayValue;
        }

        /** Return a Mask by index **/
        static public Mask getMaskByIndex(int index) {
            return SpTypeUtil.valueOf(Mask.class, index, DEFAULT);
        }

        /**
         * Return a Mask by name
         */
        static public Mask getMask(String name) {
            return getMask(name, DEFAULT);
        }

        /** Return a Mask by name giving a value to return upon error **/
        static public Mask getMask(String name, Mask nvalue) {
            // backward compat after label change - remove in 2007A
            if (name.equals("1 pixel (0.099\")")) return MASK_1;
            if (name.equals("2 pixels (0.198\")")) return MASK_2;
            if (name.equals("3 pixels (0.297\")")) return MASK_3;
            if (name.equals("4 pixels (0.396\")")) return MASK_4;
            if (name.equals("6 pixels (0.594\")")) return MASK_6;
            if (name.equals("8 pixels (0.792\")")) return MASK_8;
            return SpTypeUtil.oldValueOf(Mask.class, name, nvalue);
        }
    }

    /**
     * Filter Wheel A (engineering) parameter values.
     */
    public static enum FilterWheelA implements SequenceableSpType, DisplayableSpType {
        CLEARA1("ClearA1"),
        F22B15,
        F34B9,
        F47B5,
        F198B27,
        F209B42L,
        CLEARA7("ClearA7"),
        F103B10,
        CLEARA9("ClearA9"),
        F116B9,
        F125B9,
        CLEAR_A("Clear_A"),
        BLANK_A("Blank_A"),
        F79B10,
        F88B10,
        F97B10,
        F112B21,
        F66LA,
        F185B9B,
        F161L,
        CLEARA24("ClearA24"),
        ;

        private final String displayValue;

        FilterWheelA() {
            this(null);
        }
        FilterWheelA(String displayValue) {
            if (displayValue == null) displayValue = name();
            this.displayValue = displayValue;
        }
        public String sequenceValue() {
            return name();
        }

        public String displayValue() {
            return displayValue;
        }

        public String toString() {
            return displayValue;
        }

        public static FilterWheelA valueOf(String name, FilterWheelA nvalue) {
            return SpTypeUtil.oldValueOf(FilterWheelA.class, name, nvalue);
        }

        public static Option<FilterWheelA> valueOf(String name, Option<FilterWheelA> nvalue) {
            FilterWheelA def = nvalue.isEmpty() ? null : nvalue.getValue();
            FilterWheelA val = SpTypeUtil.oldValueOf(FilterWheelA.class, name, def);
            Option<FilterWheelA> none = None.instance();
            return val == null ? none : new Some<FilterWheelA>(val);
        }
    }

    /**
     * Filter Wheel B (engineering) parameter values.
     */
    public static enum FilterWheelB implements SequenceableSpType, DisplayableSpType {
        F86B2,
        CLEAR_B("Clear_B"),
        GRID_T("Grid_T"),
        GRID_R("Grid_R"),
        ETALON_N("Etalon_N"),
        POLY("Poly"),
        F209B42S,
        F66LB,
        F185B9C,
        F14SA,
        F14SB,
        F105B53,
        F107B4,
        F128B2,
        ;

        private final String displayValue;

        FilterWheelB() {
            this(null);
        }
        FilterWheelB(String displayValue) {
            if (displayValue == null) displayValue = name();
            this.displayValue = displayValue;
        }
        public String sequenceValue() {
            return name();
        }

        public String displayValue() {
            return displayValue;
        }

        public String toString() {
            return displayValue;
        }

        public static FilterWheelB valueOf(String name, FilterWheelB nvalue) {
            return SpTypeUtil.oldValueOf(FilterWheelB.class, name, nvalue);
        }

        public static Option<FilterWheelB> valueOf(String name, Option<FilterWheelB> nvalue) {
            FilterWheelB def = nvalue.isEmpty() ? null : nvalue.getValue();
            FilterWheelB val = SpTypeUtil.oldValueOf(FilterWheelB.class, name, def);
            Option<FilterWheelB> none = None.instance();
            return val == null ? none : new Some<FilterWheelB>(val);
        }
    }

    /**
     * User Filters
     */
    public static enum Filter implements StandardSpType, ObsoletableSpType {

        NONE("None", "none"),
        F86B2("F86B2 8.5um", "8.5"),
        F107B4("F107B4 10.7um", "10.7"),
        F128B2("NeII 12.8um", "12.8"),
        N_PRIME("N' 11.2um", "11.2"),
        SI_1("Si-1 7.7um", "7.7"),
        SI_2("Si-2 8.7um", "8.7"),
        SI_3("Si-3 9.7um", "9.7"),
        SI_4("Si-4 10.4um", "10.4"),
        SI_5("Si-5 11.7um", "11.7"),
        SI_6("Si-6 12.3um", "12.3"),
        N_PRIME_SEMI_BROAD("N' (semi-broad)", "11.2", true),
        Q("Q' (semi-broad)", "19.8", true),
        Q_BROAD("Q (broad)", "20.9", true),
        QA("Qa 18.1um", "18.1"),
        N10("N 10.4um", "10.4", true),
        Q20("Q 20.9um", "20.9", true),
        Q19("Q' 19.8um", "20.9"),
        ;

        /** The default Filter value **/
        public static Filter DEFAULT = N_PRIME;
        public static ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "filter");

        private String _wavelength;  // in µm
        private String _displayValue;
        private boolean _isObsolete;

        private Filter(String displayValue, String wavelength) {
            this(displayValue, wavelength, false);
        }

        private Filter(String displayValue, String wavelength, boolean isObsolete) {
            _displayValue = displayValue;
            _wavelength = wavelength;
            _isObsolete = isObsolete;
        }


        /** Return the filter's effective wavelength **/
        public String getWavelength() {
            return _wavelength;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return name();
        }

        public String logValue() {
            return _displayValue;
        }

        public String description() {
            return "";
        }

        public String toString() {
            return _displayValue;
        }

        public boolean isObsolete() {
            return _isObsolete;
        }

        /** Return a Filter by name **/
        static public Filter getFilter(String name) {
            return getFilter(name, DEFAULT);
        }

        /** Return a User filter by index **/
        static public Filter getFilterByIndex(int index) {
            return SpTypeUtil.valueOf(Filter.class, index, DEFAULT);
        }

        /** Return a Filter by name giving a value to return upon error **/
        static public Filter getFilter(String name, Filter nvalue) {
            if ("N (broad)".equals(name)) {
                return N10;
            } else if ("Qa (18.5 um)".equals(name)) {
                return QA;
            }
            return SpTypeUtil.oldValueOf(Filter.class, name, nvalue);
        }
    }


    /**
     * AutoConfigure values (yes/no).
     */
    public static enum AutoConfigure implements SequenceableSpType, DisplayableSpType {

        NO("No"),
        YES("Yes"),
        ;

        /** The default AutoConfigure value **/
        public static AutoConfigure DEFAULT = YES;

        private String _displayValue;

        private AutoConfigure(String displayValue) {
            _displayValue = displayValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public String toString() {
            return _displayValue;
        }

        /** Return a AutoConfigure by index **/
        static public AutoConfigure getAutoConfigureByIndex(int index) {
            return SpTypeUtil.valueOf(AutoConfigure.class, index, DEFAULT);
        }

        /** Return a AutoConfigure by name giving a value to return upon error **/
        static public AutoConfigure getAutoConfigure(String name, AutoConfigure nvalue) {
            return SpTypeUtil.oldValueOf(AutoConfigure.class, name, nvalue);
        }

        static public AutoConfigure getAutoConfigure(String name) {
            return getAutoConfigure(name, DEFAULT);
        }
    }

    /**
     * Nod orientation values.
     */
    public static enum NodOrientation implements SequenceableSpType, DisplayableSpType {

        PARALLEL("Parallel to Chop"),
        ORTHOGONAL("Orthogonal to Chop"),
        ;

        /** The default NodOrientation value **/
        public static NodOrientation DEFAULT = PARALLEL;
        private String _displayValue;

        private NodOrientation(String displayValue) {
            _displayValue = displayValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        /** Return a NodOrientation by index **/
        static public NodOrientation getNodOrientationByIndex(int index) {
            return SpTypeUtil.valueOf(NodOrientation.class, index, DEFAULT);
        }

        /** Return a NodOrientation by name **/
        static public NodOrientation getNodOrientation(String name) {
            return getNodOrientation(name, DEFAULT);
        }

        /** Return a NodOrientation by name giving a value to return upon error **/
        static public NodOrientation getNodOrientation(String name, NodOrientation nvalue) {
            return SpTypeUtil.oldValueOf(NodOrientation.class, name, nvalue);
        }
    }

}


