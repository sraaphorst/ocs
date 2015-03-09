package edu.gemini.itc.shared;

import edu.gemini.itc.flamingos2.Flamingos2;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.parameters.*;
import edu.gemini.spModel.core.Site;

/**
 * This class encapsulates the process of creating a Spectral Energy
 * Distribution (SED).  (e.g. from a data file)
 * As written it demands a certain format to the data file.
 * Each row must contain two doubles separated by whitespace or comma,
 * the first is a wavelength in nanometers, the second is the energy in
 * arbitrary units.  Since a SED will be normalized before it is used,
 * the scale is arbitrary.
 * <p/>
 * Programmer's note: There is no need for a factory.  A factory is for
 * creating something when the client does not know which concrete type
 * to create.  Since we don't have different types of SEDs at this point,
 * we could directly create a SED.
 * Maybe this is for future support of data files in different units.
 */
public class SEDFactory {
    /**
     * Returns a SED constructed with specified values.
     */
    public static VisitableSampledSpectrum getSED(double[] flux, double wavelengthStart, double wavelengthInterval) {
        return new DefaultSampledSpectrum(flux, wavelengthStart, wavelengthInterval);
    }

    /**
     * Returns a SED read from specified data file.
     * The format of the file is as follows:
     * Lines containing two doubles separated by whitespace or commas.
     * The first is wavelength in nm.  The second is flux in arbitrary units.  e.g.
     * <pre>
     * # The data, wavelengths are in nm, flux units unknown
     *  115.0  0.181751
     *  115.5  0.203323
     *  116.0  0.142062
     *  ...
     * </pre>
     */
    public static VisitableSampledSpectrum getSED(String fileName, double wavelengthInterval) {
        // values <= 0 used to trigger different behavior in an older version but seems not be used anymore
        assert wavelengthInterval > 0.0;
        final DefaultArraySpectrum as = new DefaultArraySpectrum(fileName);
        return new DefaultSampledSpectrum(as, wavelengthInterval);
    }


    /**
     * Returns a SED read from a user submitted Data file.
     * The format of the file is as follows:
     * A line containing a double specifying the wavelength interval
     * followed by lines containing two doubles
     * separated by whitespace or commas.  The first is wavelength
     * in nm.  The second is flux in arbitrary units.  e.g.
     * <pre>
     * # Wavelength sampling size in nm
     * 0.5
     * # The data, wavelengths are in nm, flux units unknown
     *  115.0  0.181751
     *  115.5  0.203323
     *  116.0  0.142062
     *  ...
     * </pre>
     */
    public static VisitableSampledSpectrum getSED(String fileName, String userSED, double wavelengthInterval) {
        // values <= 0 used to trigger different behavior in an older version but seems not be used anymore
        assert wavelengthInterval > 0.0;
        final DefaultArraySpectrum as = DefaultArraySpectrum.fromUserSpectrum(userSED);
        return new DefaultSampledSpectrum(as, wavelengthInterval);
    }


    public static VisitableSampledSpectrum getSED(SourceDefinitionParameters sdp, Instrument instrument) {

        switch (sdp.getDistributionType()) {
            case BBODY:
                return new BlackBodySpectrum(sdp.getBBTemp(),
                        instrument.getSampling(),
                        sdp.getSourceNormalization(),
                        sdp.getUnits(),
                        sdp.getNormBand(),
                        sdp.getRedshift());

            case ELINE:
                return new EmissionLineSpectrum(sdp.getELineWavelength(),
                        sdp.getELineWidth(),
                        sdp.getELineFlux(),
                        sdp.getELineContinuumFlux(),
                        sdp.getELineFluxUnits(),
                        sdp.getELineContinuumFluxUnits(),
                        sdp.getRedshift(),
                        instrument.getSampling());

            case PLAW:
                return new PowerLawSpectrum(sdp.getPowerLawIndex(),
                        instrument.getObservingStart(),
                        instrument.getObservingEnd(),
                        instrument.getSampling(),
                        sdp.getRedshift());

            default:
                final VisitableSampledSpectrum temp;
                if (sdp.isSedUserDefined()) {
                    temp = getSED(sdp.getSpectrumResource(),
                            sdp.getUserDefinedSpectrum(),
                            instrument.getSampling());
                } else {
                    temp = getSED(sdp.getSpectrumResource(),
                            instrument.getSampling());
                }
                temp.applyWavelengthCorrection();

                return temp;
        }
    }


    //Added to allow creation of an SED spanning more than one filter for NICI
    public static VisitableSampledSpectrum getSED(SourceDefinitionParameters sdp, double sampling, double observingStart, double observingEnd) {

        switch (sdp.getDistributionType()) {
            case BBODY:
                return new BlackBodySpectrum(sdp.getBBTemp(),
                        sampling,
                        sdp.getSourceNormalization(),
                        sdp.getUnits(),
                        sdp.getNormBand(),
                        sdp.getRedshift());

            case ELINE:
                return new EmissionLineSpectrum(sdp.getELineWavelength(),
                        sdp.getELineWidth(),
                        sdp.getELineFlux(),
                        sdp.getELineContinuumFlux(),
                        sdp.getELineFluxUnits(),
                        sdp.getELineContinuumFluxUnits(),
                        sdp.getRedshift(),
                        sampling);

            case PLAW:
                return new PowerLawSpectrum(sdp.getPowerLawIndex(),
                        observingStart,
                        observingEnd,
                        sampling,
                        sdp.getRedshift());

            default:
                final VisitableSampledSpectrum temp;
                if (sdp.getDistributionType() == SourceDefinitionParameters.Distribution.USER_DEFINED) {
                    temp = getSED(sdp.getSpectrumResource(),
                            sdp.getUserDefinedSpectrum(),
                            sampling);
                } else {
                    temp = getSED(sdp.getSpectrumResource(),
                            sampling);
                }
                temp.applyWavelengthCorrection();
                return temp;
        }
    }

    // TODO: site and band could be moved to instrument(?)
    public static SourceResult calculate(final Instrument instrument, final Site site, final String bandStr, final SourceDefinitionParameters sdp, final ObservingConditionParameters odp, final TeleParameters tp, final PlottingDetailsParameters pdp) {
        // Module 1b
        // Define the source energy (as function of wavelength).
        //
        // inputs: instrument, SED
        // calculates: redshifted SED
        // output: redshifteed SED

        final VisitableSampledSpectrum sed = SEDFactory.getSED(sdp, instrument);
        final SampledSpectrumVisitor redshift = new RedshiftVisitor(sdp.getRedshift());
        sed.accept(redshift);

        // Must check to see if the redshift has moved the spectrum beyond
        // useful range. The shifted spectrum must completely overlap
        // both the normalization waveband and the observation waveband
        // (filter region).

        final WavebandDefinition band = sdp.getNormBand();
        final double start = band.getStart();
        final double end = band.getEnd();

        // any sed except BBODY and ELINE have normalization regions
        switch (sdp.getDistributionType()) {
            case ELINE:
            case BBODY:
                break;
            default:
                if (sed.getStart() > start || sed.getEnd() < end) {
                    throw new IllegalArgumentException("Shifted spectrum lies outside of specified normalisation waveband.");
                }
        }

// TODO: This is only relevant for writeOutput() need to factor this out somehow!!
        if (pdp != null && pdp.getPlotLimits().equals(PlottingDetailsParameters.PlotLimits.USER)) {
            if (pdp.getPlotWaveL() > instrument.getObservingEnd() || pdp.getPlotWaveU() < instrument.getObservingStart()) {
                throw new IllegalArgumentException("User limits for plotting do not overlap with filter.");
            }
        }
// TODO: END

        // Module 2
        // Convert input into standard internally-used units.
        //
        // inputs: instrument,redshifted SED, waveband, normalization flux,
        // units
        // calculates: normalized SED, resampled SED, SED adjusted for aperture
        // output: SED in common internal units
        if (!sdp.getDistributionType().equals(SourceDefinitionParameters.Distribution.ELINE)) {
            final SampledSpectrumVisitor norm = new NormalizeVisitor(
                    sdp.getNormBand(),
                    sdp.getSourceNormalization(),
                    sdp.getUnits());
            sed.accept(norm);
        }

        final SampledSpectrumVisitor tel = new TelescopeApertureVisitor();
        sed.accept(tel);

        // SED is now in units of photons/s/nm

        // Module 3b
        // The atmosphere and telescope modify the spectrum and
        // produce a background spectrum.
        //
        // inputs: SED, AIRMASS, sky emmision file, mirror configuration,
        // output: SED and sky background as they arrive at instruments

        final SampledSpectrumVisitor clouds = CloudTransmissionVisitor.create(odp.getSkyTransparencyCloud());
        sed.accept(clouds);

        final SampledSpectrumVisitor water = WaterTransmissionVisitor.create(
                odp.getSkyTransparencyWater(),
                odp.getAirmass(),
                getWater(bandStr),
                instrument instanceof Flamingos2 ? Site.GN : site, // TODO: GN is **wrong** for F2, fix this and update regression test baseline!
                bandStr);
        sed.accept(water);

        // Background spectrum is introduced here.
        final VisitableSampledSpectrum sky = SEDFactory.getSED(getSky(instrument, bandStr, site, odp), instrument.getSampling());
        if (instrument instanceof Flamingos2) {
            // TODO: F2 differs slightly from GMOS, GNIRS, Michelle, TRecs and Nifs in this (order of operations)
            // TODO: check with science if we can change this and adapt baseline for regression tests accordingly
            final SampledSpectrumVisitor tb = new TelescopeBackgroundVisitor(tp, Site.GS, ITCConstants.NEAR_IR);
            sky.accept(tb);
            final SampledSpectrumVisitor t = TelescopeTransmissionVisitor.create(tp);
            sed.accept(t);
            sky.accept(t);
            sky.accept(tel);
        } else {
            // Apply telescope transmission to both sed and sky
            final SampledSpectrumVisitor t = TelescopeTransmissionVisitor.create(tp);
            sed.accept(t);
            sky.accept(t);
            // Create and Add background for the telescope.
            final SampledSpectrumVisitor tb = new TelescopeBackgroundVisitor(tp, site, bandStr);
            sky.accept(tb);
            sky.accept(tel);
        }

        // Add instrument background to sky background for a total background.
        // At this point "sky" is not the right name.
        instrument.addBackground(sky);

        // Module 4 AO module not implemented
        // The AO module affects source and background SEDs.

        // Module 5b
        // The instrument with its detectors modifies the source and
        // background spectra.
        // input: instrument, source and background SED
        // output: total flux of source and background.
        instrument.convolveComponents(sed);
        instrument.convolveComponents(sky);

        // End of the Spectral energy distribution portion of the ITC.
        return new SourceResult(sed, sky);
    }

    private static String getWater(final String band) {
        switch (band) {
            case ITCConstants.VISIBLE:  return "skytrans_";
            case ITCConstants.NEAR_IR:  return "nearIR_trans_";
            case ITCConstants.MID_IR:   return "midIR_trans_";
            default:                    throw new Error("invalid band");
        }
    }

    private static String getSky(final Instrument instrument, final String band, final Site site, final ObservingConditionParameters ocp) {
        // TODO: F2 uses a peculiar path (?), fix this and update regression test baseline!
        if (instrument instanceof Flamingos2) {
            return ITCConstants.SKY_BACKGROUND_LIB + "/"
                        + ITCConstants.NEAR_IR_SKY_BACKGROUND_FILENAME_BASE
                        + "_"
                        + ocp.getSkyTransparencyWaterCategory() // REL-557
                        + "_" + ocp.getAirmassCategory()
                        + ITCConstants.DATA_SUFFIX;
        }
        // TODO: this is how all instruments should work:
        switch (band) {
            case ITCConstants.VISIBLE:
                return ITCConstants.SKY_BACKGROUND_LIB + "/"
                        + ITCConstants.OPTICAL_SKY_BACKGROUND_FILENAME_BASE
                        + "_"
                        + ocp.getSkyBackgroundCategory()
                        + "_" + ocp.getAirmassCategory()
                        + ITCConstants.DATA_SUFFIX;
            case ITCConstants.NEAR_IR:
                return "/"
                        + ITCConstants.HI_RES + (site.equals(Site.GN) ? "/mk" : "/cp")
                        + ITCConstants.NEAR_IR + ITCConstants.SKY_BACKGROUND_LIB + "/"
                        + ITCConstants.NEAR_IR_SKY_BACKGROUND_FILENAME_BASE + "_"
                        + ocp.getSkyTransparencyWaterCategory() + "_"
                        + ocp.getAirmassCategory()
                        + ITCConstants.DATA_SUFFIX;
            case ITCConstants.MID_IR:
                return "/"
                        + ITCConstants.HI_RES + (site.equals(Site.GN) ? "/mk" : "/cp")
                        + ITCConstants.MID_IR +ITCConstants.SKY_BACKGROUND_LIB + "/"
                        + ITCConstants.MID_IR_SKY_BACKGROUND_FILENAME_BASE + "_"
                        + ocp.getSkyTransparencyWaterCategory() + "_"
                        + ocp.getAirmassCategory()
                        + ITCConstants.DATA_SUFFIX;
            default:
                throw new Error("invalid band");
        }
    }

    public static final class SourceResult {
        public final VisitableSampledSpectrum sed;
        public final VisitableSampledSpectrum sky;
        public SourceResult(final VisitableSampledSpectrum sed, final VisitableSampledSpectrum sky) {
            this.sed                = sed;
            this.sky                = sky;
        }
    }


}
