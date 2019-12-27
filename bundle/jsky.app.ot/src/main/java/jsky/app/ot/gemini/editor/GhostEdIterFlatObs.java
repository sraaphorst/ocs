package jsky.app.ot.gemini.editor;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.gemini.calunit.CalUnitParams;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatFlatObs;
import edu.gemini.spModel.obsclass.ObsClass;
import jsky.app.ot.OTOptions;
import jsky.app.ot.editor.OtItemEditor;
import jsky.app.ot.editor.SpinnerEditor;
import jsky.app.ot.editor.type.SpTypeUIUtil;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.TextBoxWidget;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GhostEdIterFlatObs extends OtItemEditor<ISPSeqComponent, GhostSeqRepeatFlatObs>
        implements jsky.util.gui.TextBoxWidgetWatcher {

    // the GUI layout panel
    private final IterFlatObsForm _w = new IterFlatObsForm();

    // If true, ignore action events
    private boolean ignoreActions = false;

    private static final String LAMP_PROPERTY = "Lamp";

    private final ActionListener lampListener = e -> _lampSelected();

    private final ItemListener arcListener = e -> _arcSelected();

    private final SpinnerEditor sped;

    /**
     * The constructor initializes the user interface.
     */
    public EdIterFlatObs() {
        _w.repeatSpinner.setModel(new SpinnerNumberModel(1, 1, null, 1));
        sped = new SpinnerEditor(_w.repeatSpinner, new SpinnerEditor.Functions() {
            @Override public int getValue() {
                return getDataObject().getStepCount();
            }
            @Override public void setValue(int newValue) {
                if (!ignoreActions) getDataObject().setStepCount(newValue);
            }
        });

        final List<CalUnitParams.Lamp> flatLamps = CalUnitParams.Lamp.flatLamps();
        for (int i = 0; i < _w.lamps.length; i++) {
            final CalUnitParams.Lamp l = flatLamps.get(i);
            _w.lamps[i].putClientProperty(LAMP_PROPERTY, l);
            _w.lamps[i].setText(l.displayValue());
            _w.lamps[i].addActionListener(lampListener);
        }
        final List<CalUnitParams.Lamp> arcLamps = CalUnitParams.Lamp.arcLamps();
        for (int i = 0; i < _w.arcs.length; i++) {
            final CalUnitParams.Lamp l = arcLamps.get(i);
            _w.arcs[i].putClientProperty(LAMP_PROPERTY, l);
            _w.arcs[i].setText(l.displayValue());
            _w.arcs[i].addItemListener(arcListener);
        }

        _w.shutter.setChoices(CalUnitParams.Shutter.values());

        // Set up the filter editor.
        SpTypeUIUtil.initListBox(_w.filter, CalUnitParams.Filter.class,
                e -> getDataObject().setFilter((CalUnitParams.Filter) _w.filter.getSelectedItem()));

        _w.diffuser.setChoices(CalUnitParams.Diffuser.values());
        _w.obsClass.setChoices(ObsClass.values());

        _w.exposureTime.addWatcher(this);
        _w.coadds.addWatcher(this);
        _w.shutter.addWatcher((ddlbwe, index, val) -> {
            getDataObject().setShutter(CalUnitParams.Shutter.getShutterByIndex(index));
            _updateEnabledStates();
        });

        _w.diffuser.addWatcher((ddlbwe, index, val) -> {
            getDataObject().setDiffuser(CalUnitParams.Diffuser.getDiffuserByIndex(index));
            _updateEnabledStates();
        });
        _w.obsClass.addWatcher((ddlbwe, index, val) -> {
            getDataObject().setObsClass(ObsClass.values()[index]);
            _updateEnabledStates();
        });
    }

    /**
     * Return the window containing the editor
     */
    @Override
    public JPanel getWindow() {
        return _w;
    }

    @Override
    public void init() {
        _update();
        sped.init();
    }

    @Override
    public void cleanup() {
        sped.cleanup();
    }

    // Update the widgets to reflect the model settings
    private void _update() {
        ignoreActions = true;
        try {
            _showLamps();
            _w.shutter.setValue(getDataObject().getShutter());

            // Set the selected item directly on the model to allow for
            // obsolete types to be displayed. If set on the widget itself,
            // it will not be displayed in the combo box.
            _w.filter.getModel().setSelectedItem(getDataObject().getFilter());

            _w.diffuser.setValue(getDataObject().getDiffuser());
            _w.obsClass.setValue(getDataObject().getObsClass());
            _w.exposureTime.setValue(getDataObject().getExposureTime());
            _w.coadds.setValue(getDataObject().getCoaddsCount());

            _updateEnabledStates();
        } catch (Exception e) {
            DialogUtil.error(e);
        }
        ignoreActions = false;
    }

    // update the lamp display to reflect the data object
    private void _showLamps() {
        final Set<CalUnitParams.Lamp> lamps = getDataObject().getLamps();
        for (JCheckBox b : _w.arcs) {
            final CalUnitParams.Lamp l = (CalUnitParams.Lamp) b.getClientProperty(LAMP_PROPERTY);
            b.removeItemListener(arcListener);
            b.setSelected(lamps.contains(l));
            b.addItemListener(arcListener);
        }
        for (JRadioButton b : _w.lamps) {
            final CalUnitParams.Lamp l = (CalUnitParams.Lamp) b.getClientProperty(LAMP_PROPERTY);
            b.removeActionListener(lampListener);
            b.setSelected(lamps.contains(l));
            b.addActionListener(lampListener);
        }
    }

    /**
     * Watch changes to text box widgets.
     */
    @Override
    public void textBoxKeyPress(TextBoxWidget tbwe) {
        if (tbwe == _w.exposureTime) {
            getDataObject().setExposureTime(tbwe.getDoubleValue(1.));
        } else if (tbwe == _w.coadds) {
            getDataObject().setCoaddsCount(tbwe.getIntegerValue(1));
        }
    }

    private boolean isIrGreyBody() {
        final Set<CalUnitParams.Lamp> lamps = getDataObject().getLamps();
        return lamps.contains(CalUnitParams.Lamp.IR_GREY_BODY_HIGH) || lamps.contains(CalUnitParams.Lamp.IR_GREY_BODY_LOW);
    }

    /**
     * Update the enabled states of the widgets based on the current values
     */
    private void _updateEnabledStates() {
        // disable lamp radiobuttons if an arc was selected
        final boolean isLamp = !getDataObject().isArc();
        for (final JRadioButton _lampButton : _w.lamps) {
            _lampButton.setEnabled(isLamp);
        }

        final boolean editable = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
        _w.shutter.setEnabled(isIrGreyBody() && editable);
    }

    // Called when one of the lamp radiobuttons is selected
    private void _lampSelected() {
        if (ignoreActions) {
            return;
        }
        for (int i = 0; i < _w.lamps.length; i++) {
            if (_w.lamps[i].isSelected()) {
                final CalUnitParams.Lamp lamp = CalUnitParams.Lamp.flatLamps().get(i);
                getDataObject().setLamp(lamp);
                getDataObject().setDiffuser(lamp == CalUnitParams.Lamp.QUARTZ ? CalUnitParams.Diffuser.VISIBLE : CalUnitParams.Diffuser.IR);  // See OT-426
                final boolean b = (lamp == CalUnitParams.Lamp.IR_GREY_BODY_HIGH ||
                        lamp == CalUnitParams.Lamp.IR_GREY_BODY_LOW);
                if (b) {
                    getDataObject().setShutter(CalUnitParams.Shutter.OPEN);
                } else {
                    getDataObject().setShutter(CalUnitParams.Shutter.CLOSED);
                }
                getDataObject().setObsClass(getDataObject().getDefaultObsClass());
                _update();
                break;
            }
        }
        _update();
    }

    // Called when one of the arc checkboxes changes state
    private void _arcSelected() {
        if (ignoreActions) {
            return;
        }
        final ArrayList<CalUnitParams.Lamp> arcs = new ArrayList<>(_w.arcs.length);
        boolean foundCuAR = false;  // See OT-426
        for (int i = 0; i < _w.arcs.length; i++) {
            if (_w.arcs[i].isSelected()) {
                final CalUnitParams.Lamp lamp = CalUnitParams.Lamp.arcLamps().get(i);
                arcs.add(lamp);
                foundCuAR |= lamp == CalUnitParams.Lamp.CUAR_ARC;  // See OT-426
            }
        }
        if (arcs.size() != 0) {
            getDataObject().setLamps(arcs);
            getDataObject().setShutter(CalUnitParams.Shutter.CLOSED);
            getDataObject().setObsClass(getDataObject().getDefaultObsClass());
            getDataObject().setDiffuser(foundCuAR ? CalUnitParams.Diffuser.VISIBLE : CalUnitParams.Diffuser.IR); // See OT-426
        } else {
            _lampSelected();
        }
        _update();
    }
}

{
}
