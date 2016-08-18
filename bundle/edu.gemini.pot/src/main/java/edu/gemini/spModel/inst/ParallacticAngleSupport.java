package edu.gemini.spModel.inst;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.skycalc.Angle;

/**
 * Support for parallactic angle calculations.
 *
 * Instruments that support the parallactic angle feature must implement this in order to contain the necessary
 * properties for configuration by the ParallacticAnglePanel, or must inherit from the concrete implementation
 * in ParallacticAngleSupportInst.
 */
public interface ParallacticAngleSupport {

    /**
     * Perform the parallactic angle computation for the observation.
     */
    Option<Angle> calculateParallacticAngle(ISPObservation obs);

    /**
     * Determine if the current instrument configuration is compatible or not with parallactic angle support.
     */
    boolean isCompatibleWithMeanParallacticAngleMode();
}
