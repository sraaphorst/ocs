package edu.gemini.spModel.gemini.seqcomp;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.seqcomp.GhostIExpSeqComponent;
import edu.gemini.spModel.seqcomp.GhostSeqRepeatExp;

/**
 * The GHOST flat iterator, and equivalent of SeqRepeatFlatObs.
 */
final public class GhostSeqRepeatFlatObs extends GhostSeqRepeatExp
    implements GhostIExpSeqComponent {
    private static final long serialVersionUID = 1L;

    public static final SPComponentType SP_TYPE = SPComponentType.OBSERVER_GHOST_GEMFLAT;

    public static final ISPNodeInitializer<ISPSeqComponent, GhostSeqRepeatFlatObs> NI =
            new ComponentNodeInitializer<>(SP_TYPE, GhostSeqRepeatFlatObs::new, GhostSeqRepeatFla)

}
