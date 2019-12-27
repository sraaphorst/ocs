package edu.gemini.spModel.gemini.seqcomp;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.config.AbstractSeqComponentCB;
import edu.gemini.spModel.data.config.*;
import edu.gemini.spModel.dataflow.GsaSequenceEditor;
import edu.gemini.spModel.gemini.calunit.CalUnitConstants;
import edu.gemini.spModel.gemini.calunit.CalUnitParams.*;
import edu.gemini.spModel.gemini.calunit.calibration.CalConfigBuilderUtil;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.seqcomp.SeqRepeatCbOptions;

import java.util.Map;

/**
 * A configuration builder for the Gemini CalUnit sequence component for GHOST.
 */
public class GhostSeqRepeatFlatObsCB extends AbstractSeqComponentCB {
    private static final long serialVersionUID = 1L;

    private transient int _curCount;
    private transient int _max;
    private transient int _limit;
    private transient Map<String, Object> _options;

    private transient String _obsClass;

    public GhostSeqRepeatFlatObsCB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

    public Object clone() {
        final GhostSeqRepeatFlatObsCB result = (GhostSeqRepeatFlatObsCB) super.clone();
        result._curCount = 0;
        result._max      = 0;
        result._limit    = 0;
        result._options  = null;
        result._obsClass = null;
        return result;
    }

    @Override
    protected void thisReset(Map<String, Object> options) {
        _curCount = 0;
        final GhostSeqRepeatFlatObs c = (GhostSeqRepeatFlatObs) getDataObject();
        _max = c.getStepCount();
        _limit = SeqRepeatCbOptions.getCollapseRepeat(options) ? 1 : _max;

        _obsClass = c.getObsClass().sequenceValue();
        _options = options;
    }

    protected boolean thisHasNext() {
        return _curCount < _limit;
    }

    protected void thisApplyNext(IConfig config, IConfig prevFull) {
        ++_curCount;

        // Remove any executed smartcal data placed in the config by the
        // GemObservationCB.  This can happen when converting a smart cal to
        // a manual calibration for executed or partially executed sequences.
        CalConfigBuilderUtil.clear(config);

        final GhostSeqRepeatFlatObs c = (GhostSeqRepeatFlatObs) getDataObject();
        config.putParameter(SeqConfigNames.OBSERVE_CONFIG_NAME,
                DefaultParameter.getInstance(CalUnitConstants.LAMP_PROP, Lamp.write(c.getLamps())));
        config.putParameter(SeqConfigNames.OBSERVE_CONFIG_NAME,
                DefaultParameter.getInstance(CalUnitConstants.SHUTTER_PROP, c.getShutter().sequenceValue()));
        config.putParameter(SeqConfigNames.OBSERVE_CONFIG_NAME,
                DefaultParameter.getInstance(CalUnitConstants.FILTER_PROP, c.getFilter().sequenceValue()));
        config.putParameter(SeqConfigNames.OBSERVE_CONFIG_NAME,
                DefaultParameter.getInstance(CalUnitConstants.DIFFUSER_PROP, c.getDiffuser().sequenceValue()));
        config.putParameter(SeqConfigNames.OBSERVE_CONFIG_NAME,
                StringParameter.getInstance(InstConstants.OBS_CLASS_PROP,
                        c.getObsClass().sequenceValue()));

        GsaSequenceEditor.instance.addProprietaryPeriod(config, getSeqComponent().getProgram(), c.getObsClass());

        if (SeqRepeatCbOptions.getAddObsCount(_options)) {
            ISysConfig obs = getObsSysConfig(config);
            obs.putParameter(
                    DefaultParameter.getInstance(InstConstants.REPEAT_COUNT_PROP, _max));
        }
    }

    private ISysConfig getObsSysConfig(IConfig config) {
        ISysConfig sys = config.getSysConfig(SeqConfigNames.OBSERVE_CONFIG_NAME);
        if (sys == null) {
            sys = new DefaultSysConfig(SeqConfigNames.OBSERVE_CONFIG_NAME);
            config.appendSysConfig(sys);
        }
        return sys;
    }
}

