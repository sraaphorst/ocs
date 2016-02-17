package jsky.app.ot.tpe.feat;

import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.obsComp.TargetSelection;
import jsky.app.ot.tpe.*;
import jsky.app.ot.util.BasicPropertyList;
import jsky.app.ot.util.PropertyWatcher;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Iterator;

public class TpeTargetPosFeature extends TpePositionFeature
        implements TpeCreatableFeature, PropertyWatcher {

    private static final BasicPropertyList _props = new BasicPropertyList(TpeTargetPosFeature.class.getName());
    private static final String PROP_SHOW_TAGS = "Show Tags";
    static {
        _props.registerBooleanProperty(PROP_SHOW_TAGS, true);
    }

    /**
     * Construct the feature with its name and description.
     */
    public TpeTargetPosFeature() {
        super("Target", "Show the locations of target positions.");
    }


    public void reinit(TpeImageWidget iw, TpeImageInfo tii) {
        super.reinit(iw, tii);

        _props.addWatcher(this);

        // Tell the position map that the target positions are visible.
        TpePositionMap pm = TpePositionMap.getMap(iw);
        pm.setFindUserTarget(true);
    }

    public void unloaded() {
        // Tell the position map that the target positions are not visible.
        TpePositionMap pm = TpePositionMap.getExistingMap();
        if (pm != null) pm.setFindUserTarget(false);

        _props.deleteWatcher(this);

        super.unloaded();
    }

    /**
     * A property has changed.
     *
     * @see PropertyWatcher
     */
    public void propertyChange(String propName) {
        _iw.repaint();
    }

    /**
     * Override getProperties to return the properties supported by this
     * feature.
     */
    public BasicPropertyList getProperties() {
        return _props;
    }

    /**
     * Turn on/off the drawing of position tags.
     */
    public void setDrawTags(boolean drawTags) {
        _props.setBoolean(PROP_SHOW_TAGS, drawTags);
    }

    /**
     * Get the "draw position tags" property.
     */
    public boolean getDrawTags() {
        return _props.getBoolean(PROP_SHOW_TAGS, true);
    }

    private final TpeCreatableItem[] createableItems = new TpeCreatableItem[] {
        new TpeCreatableItem() {
            public String getLabel() {
                return "Target";
            }

            public Type getType() {
                return Type.userTarget;
            }

            public boolean isEnabled(TpeContext ctx) {
                return ctx.targets().isDefined();
            }

            public void create(TpeMouseEvent tme, TpeImageInfo tii) {
                TargetObsComp obsComp = getTargetObsComp();
                if (obsComp == null) return;

                double ra  = tme.pos.getRaDeg();
                double dec = tme.pos.getDecDeg();
                SPTarget userPos = new SPTarget(ra, dec);

                TargetEnvironment env = obsComp.getTargetEnvironment();
                ImList<SPTarget> userList = env.getUserTargets().append(userPos);
                obsComp.setTargetEnvironment(env.setUserTargets(userList));
                _iw.getContext().targets().commit();
            }
        }
    };

    /**
     */
    public TpeCreatableItem[] getCreatableItems() {
        return createableItems;
    }

    /**
     */
    public boolean erase(final TpeMouseEvent tme) {
        final TargetObsComp obsComp = getTargetObsComp();
        if (obsComp == null) return false;

        final TpePositionMap pm = TpePositionMap.getMap(_iw);

        final Iterator<PosMapEntry<SPTarget>> it = pm.getAllPositionMapEntries();
        while (it.hasNext()) {
            final PosMapEntry<SPTarget> pme = it.next();
            final SPTarget tp = pme.taggedPos;

            final TargetEnvironment env = obsComp.getTargetEnvironment();
            if (!env.getUserTargets().contains(tp)) continue;

            if (positionIsClose(pme, tme.xWidget, tme.yWidget)) {
                ImList<SPTarget> userList = env.getUserTargets().remove(tp);
                obsComp.setTargetEnvironment(env.setUserTargets(userList));
                _iw.getContext().targets().commit();
                return true;
            }
        }
        return false;
    }

    /**
     * @see jsky.app.ot.tpe.TpeSelectableFeature
     */
    public Object select(TpeMouseEvent tme) {
        TargetObsComp obsComp = getTargetObsComp();
        if (obsComp == null) return null;

        TpePositionMap pm = TpePositionMap.getMap(_iw);
        SPTarget tp = (SPTarget) pm.locatePos(tme.xWidget, tme.yWidget);
        if (tp == null) return null;

        TargetEnvironment env = obsComp.getTargetEnvironment();
        if (!env.getUserTargets().contains(tp)) return null;

        TargetSelection.setTargetForNode(env, getContext().targets().shell().get(), tp);
        return tp;
    }

    /**
     */
    public void draw(Graphics g, TpeImageInfo tii) {
        TargetObsComp obsComp = getTargetObsComp();
        if (obsComp == null) return;

        TargetEnvironment env = obsComp.getTargetEnvironment();

        TpePositionMap pm = TpePositionMap.getMap(_iw);

        g.setColor(Color.yellow);

        boolean drawTags = getDrawTags();
        if (drawTags) g.setFont(FONT);

        int index = 1;
        for (SPTarget target : env.getUserTargets()) {
            PosMapEntry<SPTarget> pme = pm.getPositionMapEntry(target);
            if (pme == null) continue;

            String tag = String.format("User (%d)", index++);

            Point2D.Double p = pme.screenPos;
            if (p == null) continue;

            g.drawLine((int) p.x, (int) (p.y - MARKER_SIZE), (int) p.x, (int) (p.y + MARKER_SIZE));
            g.drawLine((int) (p.x - MARKER_SIZE), (int) p.y, (int) (p.x + MARKER_SIZE), (int) p.y);

            if (drawTags) {
                // Draw the tag--should use font metrics to position the tag
                g.drawString(tag, (int) (p.x + MARKER_SIZE + 2), (int) (p.y + MARKER_SIZE * 2));
            }
        }
    }

    /**
     */
    public Option<Object> dragStart(TpeMouseEvent tme, TpeImageInfo tii) {
        TargetObsComp obsComp = getTargetObsComp();
        if (obsComp == null) return None.instance();

        TargetEnvironment env = obsComp.getTargetEnvironment();

        TpePositionMap pm = TpePositionMap.getMap(_iw);

        Iterator<PosMapEntry<SPTarget>> it = pm.getAllPositionMapEntries();
        while (it.hasNext()) {
            PosMapEntry<SPTarget> pme = it.next();

            if (positionIsClose(pme, tme.xWidget, tme.yWidget) &&
                    env.isUserPosition(pme.taggedPos)) {

                _dragObject = pme;
                return new Some<>(pme.taggedPos);
            }
        }
        return None.instance();
    }
}

