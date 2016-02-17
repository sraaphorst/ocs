package jsky.app.ot.tpe;

import edu.gemini.catalog.image.ImageCatalog;
import edu.gemini.catalog.ui.tpe.ImageCatalogLoader;
import edu.gemini.pot.ModelConverters;
import edu.gemini.pot.sp.*;
import edu.gemini.shared.skyobject.coords.HmsDegCoordinates;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.skycalc.Angle;
import edu.gemini.spModel.core.Coordinates;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.gemini.nici.SeqRepeatNiciOffset;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffset;
import edu.gemini.spModel.guide.GuideProbeAvailabilityVolatileDataObject;
import edu.gemini.spModel.obs.SchedulingBlock;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.obsComp.TargetSelection;
import edu.gemini.spModel.target.offset.OffsetPosSelection;
import edu.gemini.spModel.target.system.ITarget;
import jsky.app.jskycat.JSkyCat;
import jsky.app.ot.OT;
import jsky.app.ot.ags.*;
import jsky.app.ot.util.BasicPropertyList;
import jsky.catalog.CatalogException;
import jsky.image.gui.ImageDisplayControlFrame;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implements a telescope position editor for the Gemini telescope
 * and instruments. This class displays an image and/or graphics overlays for guide
 * stars and instruments and allows the user to edit the positions to use
 * for an observation.
 *
 * @author Allan Brighton (based on code from original OT version)
 * @version $Revision: 46768 $ $Date: 2012-07-16 14:58:53 -0400 (Mon, 16 Jul 2012) $
 */
public class TelescopePosEditor extends JSkyCat implements TpeMouseObserver {

    /**
     * All feature class names
     */
    private static final List<Class<?>> _featureClasses = new ArrayList<>();

    /**
     * List of all features
     */
    private final List<TpeImageFeature> _allFeatures;

    private TpeContext _ctx = TpeContext.empty();

    /**
     * The image widget
     */
    private TpeImageWidget _iw;

    /**
     * Tool button helper
     */
    private final TpeEditorTools _editorTools;

    /**
     * Object managing the image features
     */
    private final TpeFeatureManager _featureMan;

    /**
     * Toolbar (on the side with toggle buttons)
     */
    private final TpeToolBar _tpeToolBar;

    /**
     * Menu bar
     */
    private final TpeImageDisplayMenuBar _tpeMenuBar;

    private final AgsContextPublisher _agsPub = new AgsContextPublisher();

    /**
     * Create the application class and display the contents of the
     * given image file or URL, if not null.
     */
    public TelescopePosEditor() {
        super(null);

        // This is terrible, the constructor of JSkyCat sets _iw indirectly calling makeNavigatorImageDisplayFrame
        _iw.setTitle("Position Editor");

        // get the TPE toolbar handle
        final Component parent = _iw.getParentFrame();
        _tpeToolBar = ((TpeImageDisplayFrame) parent).getTpeToolBar();
        _tpeMenuBar = (TpeImageDisplayMenuBar) ((TpeImageDisplayFrame) parent).getJMenuBar();

        _tpeToolBar.getGuiderSelector().addSelectionListener(strategy -> AgsStrategyUtil.setSelection(_ctx.obsShellOrNull(), strategy));

        _editorTools = new TpeEditorTools(this);

        //
        // Create and add the standard TpeImageFeatures
        //

        _iw.addMouseObserver(this);
        _featureMan = new TpeFeatureManager(this, _iw);

        // Get the list of all features and add them to the user
        // interface in the order in which they were registered
        _allFeatures = _createFeatures();
        _allFeatures.forEach(this::_addFeature);

        // Select the "Browse" tool.
        _editorTools.gotoBrowseMode();

        // Update the GUI when the OT editable state changes
        OT.addEditableStateListener(new OT.EditableStateListener() {
            @Override public ISPNode getEditedNode() { return _ctx.nodeOrNull(); }
            @Override public void updateEditableState() { _editorTools.updateAvailableOptions(_allFeatures); }
        });

        _agsPub.subscribe((obs, oldOptions, newOptions) -> _tpeToolBar.getGuiderSelector().setAgsOptions(newOptions));
    }

    /**
     * Return true if the given SP node is one of the nodes managed by
     * this class and used by the tpe. These are the nodes that need
     * to be shared between the SP viewer and the position editor,
     * since they both display data based on them. This includes the
     * target env, offset and instrument nodes.
     */
    private static boolean isTpeObject(final ISPNode node) {
        final SPComponentType type;
        if (node instanceof ISPObsComponent) {
            type = ((ISPObsComponent) node).getType();
        } else if (node instanceof ISPSeqComponent) {
            type = ((ISPSeqComponent) node).getType();
        } else {
            type = null;
        }

        if (type != null) {
            final SPComponentBroadType broadType = type.broadType;
            return (broadType.equals(TargetObsComp.SP_TYPE.broadType) ||
                    broadType.equals(SPComponentBroadType.INSTRUMENT) ||
                    type.equals(SeqRepeatOffset.SP_TYPE) ||
                    type.equals(SeqRepeatNiciOffset.SP_TYPE) ||
                    type.equals(SPSiteQuality.SP_TYPE));
        }
        return false;
    }

    /**
     * Make and return a frame for displaying the given image (may be null).
     *
     * @param imageFileOrUrl specifies the image file or URL to display
     */
    @Override
    protected ImageDisplayControlFrame makeNavigatorImageDisplayFrame(final String imageFileOrUrl) {
        final TpeImageDisplayFrame frame = new  TpeImageDisplayFrame(imageFileOrUrl);
        // Very ugly: this sets _iw indirectly on the constructor.
        _iw = (TpeImageWidget) frame.getImageDisplayControl().getImageDisplay();
        return frame;
    }

    /**
     * Return the telescope position editor toolbar (the one with the toggle buttons)
     */
    public TpeToolBar getTpeToolBar() {
        return _tpeToolBar;
    }

    /**
     * Register an independent feature.  These features will be
     * created when a TelescopePositionEditor is created.
     */
    public static void registerFeature(final Class<?> clazz) {
        _featureClasses.add(clazz);
    }

    /**
     * Return a reference to the first registered TPE image feature
     * corresponding to the given class, or null if not found.
     */
    public TpeImageFeature getFeature(final Class<?> c) {
        return _allFeatures.stream().filter(f -> f.getClass().equals(c)).findFirst().orElseGet(null);
    }

    public Set<TpeImageFeature> getFeatures() {
        return new HashSet<>(_allFeatures);
    }

    /**
     * Instantiate all the TpeImageFeatures indicated in the given Vector.
     */
    private static List<TpeImageFeature> _createFeatures() {
        return TelescopePosEditor._featureClasses.stream().map(TpeImageFeature::createFeature).collect(Collectors.toList());
    }

    /**
     * Do the work of adding a feature without worrying about whether
     * it should be visible or not.
     */
    private void _addFeature(final TpeImageFeature tif) {
        if (_featureMan.isFeaturePresent(tif)) return; // already being displayed

        _featureMan.addFeature(tif);
        _editorTools.addFeature(tif);

        // If this feature has properties, show them in the "View" menu.
        final BasicPropertyList pl = tif.getProperties();
        if (pl != null) {
            _tpeMenuBar.addPropertyConfigMenuItem(tif.getName(), pl);
        }
    }

    public void selectFeature(final TpeImageFeature tif) {
        _featureMan.setSelected(tif, true);
    }

    /**
     * Load a cached image for the base position, or generate a blank image with
     * world coordinate support and the base position at the center.
     */
    private void loadImage() {
        final double ra;
        final double dec;
        SPTarget tp = _ctx.targets().baseOrNull();
        if (tp != null) {
            // Get the RA and Dec from the pos list.
            final ITarget target = tp.getTarget();
            final Option<Long> when = _ctx.schedulingBlockJava().map(SchedulingBlock::start);
            ra = target.getRaDegrees(when).getOrElse(0.0);
            dec = target.getDecDegrees(when).getOrElse(0.0);
        } else {
            ra  = 0.0;
            dec = 0.0;
        }

        _iw.loadCachedImage(ra, dec);
    }

    private boolean basePosUpdated(final TpeContext oldCtx, final TpeContext newCtx) {
        final SPTarget oldBasePos = oldCtx.targets().baseOrNull();
        final SPTarget newBasePos = newCtx.targets().baseOrNull();
        if (newBasePos == null) return oldBasePos != null;
        if (oldBasePos == null) return true;

        final ITarget oldBase = oldBasePos.getTarget();
        final ITarget newBase = newBasePos.getTarget();

        final Option<Long> oldWhen = oldCtx.schedulingBlockJava().map(SchedulingBlock::start);
        final Option<Long> newWhen = newCtx.schedulingBlockJava().map(SchedulingBlock::start);

        return !(oldBase.getRaDegrees(oldWhen).equals(newBase.getRaDegrees(newWhen)) &&
                 oldBase.getDecDegrees(oldWhen).equals(newBase.getDecDegrees(newWhen)));
    }

    private final PropertyChangeListener obsListener = evt -> {
        if (!(evt.getSource() instanceof ISPNode)) return;
        if (!evt.getPropertyName().equals(SPUtil.getDataObjectPropertyName()) &&
                !(evt instanceof SPStructureChange)) return;

        final ISPNode src = (ISPNode) evt.getSource();
        final ISPDataObject obj = src.getDataObject();
        if (isTpeObject(src) || (obj instanceof GuideProbeAvailabilityVolatileDataObject)) {
            if (_ctx.node().isDefined()) {
                reset(_ctx.node().get());
            }
        }
    };
    
    private final PropertyChangeListener selListener = evt -> {
        reset((ISPNode) evt.getSource());
    };

    /**
     * Reset the position editor based on the currently selected science program
     * tree node.
     */
    public void reset(final ISPNode node) {
        ImOption.fromScalaOpt(_ctx.obsShell()).foreach(obs -> {
            obs.removeCompositeChangeListener(obsListener);
            obs.removeStructureChangeListener(obsListener);
        });

        ImOption.fromScalaOpt(_ctx.targets().shell()).foreach(targetComp -> TargetSelection.deafTo(targetComp, selListener));
        _ctx.offsets().allJava().forEach(off -> OffsetPosSelection.deafTo(off.shell().get(), selListener));

        final TpeContext oldCtx = _ctx;
        _ctx = TpeContext.apply(node);
        _iw.reset(_ctx);

        // Reset the position map with the new position list to work from.
        final TpePositionMap pm = TpePositionMap.getMap(_iw);
        pm.reset(_ctx);

        if (_ctx.isEmpty()) {
            _iw.clear();
        } else {
            if (basePosUpdated(oldCtx, _ctx)) {
                loadImage();
            } else if (_ctx.targets().baseOrNull() == null) {
                _iw.clear();
            }
        }

        _featureMan.updateAvailableOptions(_allFeatures, _ctx);
        _editorTools.updateAvailableOptions(_allFeatures);

        _editorTools.updateEnabledStates();

        // update selected guiders in toolbar
        final Option<ISPObservation> obsShell = ImOption.fromScalaOpt(_ctx.obsShell());
        _agsPub.watch(obsShell);
        _tpeToolBar.getGuiderSelector().setAgsOptions(_agsPub.getAgsContext());
        obsShell.foreach(obs -> {
            obs.addCompositeChangeListener(obsListener);
            obs.addStructureChangeListener(obsListener);
        });

        ImOption.fromScalaOpt(_ctx.targets().shell()).foreach(targetComp -> TargetSelection.listenTo(targetComp, selListener));
        _ctx.offsets().allJava().forEach(off -> OffsetPosSelection.listenTo(off.shell().get(), selListener));
    }

    /**
     * Get a Sky Image based on the properties set by the user
     *
     * @throws IOException      If a problem happens reading from the catalog
     * @throws CatalogException if a Catalog Problem is found
     */
    public void getSkyImage(final TpeContext ctx) throws IOException, CatalogException {
        final SPTarget _baseTarget = ctx.targets().baseOrNull();
        if (_baseTarget == null) return;

        final ImageCatalog imageCatalog = ImageCatalog.instance().user();

        final Option<Long> when = ctx.schedulingBlockJava().map(SchedulingBlock::start);

        _baseTarget.getTarget().getRaDegrees(when).flatMap(ra ->
            _baseTarget.getTarget().getDecDegrees(when).flatMap( dec -> {
                final HmsDegCoordinates hmsDegCoordinates = new HmsDegCoordinates.Builder(new Angle(ra, Angle.Unit.DEGREES), new Angle(dec, Angle.Unit.DEGREES)).build();
                final Coordinates coordinates = ModelConverters.toCoordinates(hmsDegCoordinates);
                final URL queryUrl = imageCatalog.queryUrl(coordinates);
                ImageCatalogLoader.instance().display4Java(getImageWidget(), queryUrl);
                return null;
            }));
    }


    /**
     * Receive a mouse event in the image widget.
     */
    @Override
    public void tpeMouseEvent(final TpeImageWidget iw, final TpeMouseEvent mouseEvent) {
        // only interested in button 1 here
        if (mouseEvent.mouseEvent.getButton() > 1)
            return;

        if (mouseEvent.id == MouseEvent.MOUSE_PRESSED) {
            switch (_editorTools.getMode()) {
                case BROWSE:
                    break;

                case ERASE:
                    _iw.erase(mouseEvent);
                    break;

                case DRAG:
                    _iw.dragStart(mouseEvent);
                    break;
            }
            return;
        }

        if (mouseEvent.id == MouseEvent.MOUSE_CLICKED) {
            switch (_editorTools.getMode()) {
                case BROWSE:
                    if (mouseEvent.mouseEvent.getClickCount() == 2) {
                        _iw.action(mouseEvent);
                    }
                    break;

                case ERASE:
                    _iw.erase(mouseEvent);
                    break;

                case CREATE:
                    _iw.create(mouseEvent, _editorTools.getCurrentCreatableItem());
                    break;
            }
            return;
        }

        if (mouseEvent.id == MouseEvent.MOUSE_DRAGGED) {
            switch (_editorTools.getMode()) {
                case DRAG: {
                    _iw.drag(mouseEvent);
                    break;
                }
                case ERASE: {
                    _iw.erase(mouseEvent);
                    break;
                }
            }
            return;
        }

        if (mouseEvent.id == MouseEvent.MOUSE_RELEASED) {
            if (_editorTools.getMode() == TpeMode.DRAG) {
                _iw.dragStop(mouseEvent);
            }
            return;
        }

        if (mouseEvent.id == MouseEvent.MOUSE_MOVED) {
            TpeCursor c = TpeCursor.browse;
            switch (_editorTools.getMode()) {
                case ERASE:
                    c = TpeCursor.erase;
                    break;
                case DRAG:
                    c = TpeCursor.drag;
                    break;
                case CREATE:
                    c = TpeCursor.add;
                    break;
            }
            ((TpeImageDisplayFrame) getImageFrame()).getImageDisplayControl().setCursor(c.get());
        }
    }


    /**
     * Return the image widget
     */
    public TpeImageWidget getImageWidget() {
        return _iw;
    }
}
