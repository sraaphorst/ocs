package edu.gemini.spModel.gemini.ghost

import java.beans.PropertyDescriptor
import java.util.{Collections, List => JList, Map => JMap, Set => JSet}

import edu.gemini.pot.sp._
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.data.config.{DefaultParameter, DefaultSysConfig, ISysConfig, StringParameter}
import edu.gemini.spModel.data.property.{PropertyProvider, PropertySupport}
import edu.gemini.spModel.gemini.init.{ComponentNodeInitializer, ObservationNI}
import edu.gemini.spModel.obs.SPObservation
import edu.gemini.spModel.obscomp.{InstConfigInfo, InstConstants, SPInstObsComp}
import edu.gemini.spModel.pio.{ParamSet, Pio, PioFactory}
import edu.gemini.spModel.seqcomp.SeqConfigNames
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.spModel.telescope.{IssPort, IssPortProvider}

import scala.collection.immutable.TreeMap
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}


/** The GHOST instrument SP model.
  * Note that we do not override clone since private variables are immutable.
  */
final class Ghost extends SPInstObsComp(GhostMixin.SP_TYPE) with PropertyProvider with GhostMixin with IssPortProvider {
  override def getSite: JSet[Site] = {
    Site.SET_GS
  }

  override def getPhaseIResourceName: String = {
    "gemGHOST"
  }

  override def getProperties: JMap[String, PropertyDescriptor] = {
    Ghost.PropertyMap
  }

  /** ParamSet methods.
    *
    * We don't have to override these, as SPInstObsComp handles our only property
    * right now, i.e. POS_ANGLE_PROP, but we do so anyway as we will eventually need
    * to add additional properties.
    */
  override def getParamSet(factory: PioFactory): ParamSet = {
    val paramSet = super.getParamSet(factory)
    Pio.addParam(factory, paramSet, Ghost.PORT_PROP, port.name)
    Pio.addBooleanParam(factory, paramSet, Ghost.ENABLE_FIBER_AGITATOR_PROP.getName, enableFiberAgitator)
    Pio.addDoubleParam(factory, paramSet, Ghost.RED_EXPOSURE_TIME_PROP.getName, redExposureTime)
    Pio.addParam(factory, paramSet, Ghost.RED_SPECTRAL_BINNING_PROP, redSpectralBinning.name)
    Pio.addParam(factory, paramSet, Ghost.RED_SPATIAL_BINNING_PROP, redSpatialBinning.name)
    Pio.addDoubleParam(factory, paramSet, Ghost.BLUE_EXPOSURE_TIME_PROP.getName, blueExposureTime)
    Pio.addParam(factory, paramSet, Ghost.BLUE_SPECTRAL_BINNING_PROP, blueSpectralBinning.name)
    Pio.addParam(factory, paramSet, Ghost.BLUE_SPATIAL_BINNING_PROP, blueSpatialBinning.name)
    Pio.addParam(factory, paramSet, Ghost.BLUE_READ_NOISE_GAIN_PROP, blueReadNoiseGain.name)
    paramSet
  }

  override def setParamSet(paramSet: ParamSet): Unit = {
    super.setParamSet(paramSet)
    Option(Pio.getValue(paramSet, Ghost.PORT_PROP)).map(IssPort.valueOf).foreach(setIssPort)
    setEnableFiberAgitator(Pio.getBooleanValue(paramSet, Ghost.ENABLE_FIBER_AGITATOR_PROP.getName,true))
    setRedExposureTime(Pio.getDoubleValue(paramSet, Ghost.RED_EXPOSURE_TIME_PROP.getName, InstConstants.DEF_EXPOSURE_TIME))
    Option(Pio.getValue(paramSet, Ghost.RED_SPECTRAL_BINNING_PROP)).map(GhostSpectralBinning.valueOf).foreach(setRedSpectralBinning)
    Option(Pio.getValue(paramSet, Ghost.RED_SPATIAL_BINNING_PROP)).map(GhostSpatialBinning.valueOf).foreach(setRedSpatialBinning)
    setBlueExposureTime(Pio.getDoubleValue(paramSet, Ghost.BLUE_EXPOSURE_TIME_PROP.getName, InstConstants.DEF_EXPOSURE_TIME))
    Option(Pio.getValue(paramSet, Ghost.BLUE_SPECTRAL_BINNING_PROP)).map(GhostSpectralBinning.valueOf).foreach(setBlueSpectralBinning)
    Option(Pio.getValue(paramSet, Ghost.BLUE_SPATIAL_BINNING_PROP)).map(GhostSpatialBinning.valueOf).foreach(setBlueSpatialBinning)
    Option(Pio.getValue(paramSet, Ghost.BLUE_READ_NOISE_GAIN_PROP)).map(GhostReadNoiseGain.valueOf).foreach(setBlueReadNoiseGain)
  }

  override def getSysConfig: ISysConfig = {
    val sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME)
    sc.putParameter(StringParameter.getInstance(ISPDataObject.VERSION_PROP, getVersion))
    sc.putParameter(DefaultParameter.getInstance(Ghost.POS_ANGLE_PROP, getPosAngle))
    sc.putParameter(DefaultParameter.getInstance(Ghost.PORT_PROP, getIssPort))
    sc.putParameter(DefaultParameter.getInstance(Ghost.ENABLE_FIBER_AGITATOR_PROP.getName, isEnableFiberAgitator))
    sc.putParameter(DefaultParameter.getInstance(Ghost.RED_EXPOSURE_TIME_PROP.getName, getRedExposureTime))
    sc.putParameter(DefaultParameter.getInstance(Ghost.RED_SPECTRAL_BINNING_PROP.getName, getRedSpectralBinning))
    sc.putParameter(DefaultParameter.getInstance(Ghost.RED_SPATIAL_BINNING_PROP.getName, getRedSpatialBinning))
    sc.putParameter(DefaultParameter.getInstance(Ghost.BLUE_EXPOSURE_TIME_PROP.getName, getBlueExposureTime))
    sc.putParameter(DefaultParameter.getInstance(Ghost.BLUE_SPECTRAL_BINNING_PROP.getName, getBlueSpectralBinning))
    sc.putParameter(DefaultParameter.getInstance(Ghost.BLUE_SPATIAL_BINNING_PROP.getName, getBlueSpatialBinning))
    sc.putParameter(DefaultParameter.getInstance(Ghost.BLUE_READ_NOISE_GAIN_PROP, getBlueReadNoiseGain))
    sc
  }

  /**
   * ISS Port
   */
  private var port: IssPort = IssPort.DEFAULT
  override def getIssPort: IssPort = port
  override def setIssPort(newValue: IssPort): Unit = {
    val oldValue = getIssPort
    if (oldValue != newValue) {
      port = newValue
      firePropertyChange(Ghost.PORT_PROP, oldValue, newValue)
    }
  }

  /**
   * Fiber agitator Default is enabled.
   */
  private var enableFiberAgitator: Boolean = true
  def isEnableFiberAgitator: Boolean = enableFiberAgitator
  def setEnableFiberAgitator(newValue: Boolean): Unit = {
    val oldValue = isEnableFiberAgitator
    if (oldValue != newValue) {
      enableFiberAgitator = newValue
      firePropertyChange(Ghost.ENABLE_FIBER_AGITATOR_PROP, oldValue, newValue)
    }
  }

  /**
   * Detectors.
   */
  private var redExposureTime: Double = InstConstants.DEF_EXPOSURE_TIME
  def getRedExposureTime: Double = redExposureTime
  def setRedExposureTime(newValue: Double): Unit = {
    val oldValue = getRedExposureTime
    if (oldValue != newValue) {
      redExposureTime = newValue
      firePropertyChange(Ghost.RED_EXPOSURE_TIME_PROP, oldValue, newValue)
    }
  }

  private var redSpectralBinning: GhostSpectralBinning = GhostSpectralBinning.DEFAULT
  def getRedSpectralBinning: GhostSpectralBinning = redSpectralBinning
  def setRedSpectralBinning(newValue: GhostSpectralBinning): Unit = {
    val oldValue = getRedSpectralBinning
    if (oldValue != newValue) {
      redSpectralBinning = newValue
      firePropertyChange(Ghost.RED_SPECTRAL_BINNING_PROP, oldValue, newValue)
    }
  }

  private var redSpatialBinning: GhostSpatialBinning = GhostSpatialBinning.DEFAULT
  def getRedSpatialBinning: GhostSpatialBinning = redSpatialBinning
  def setRedSpatialBinning(newValue: GhostSpatialBinning): Unit = {
    val oldValue = getRedSpatialBinning
    if (oldValue != newValue) {
      redSpatialBinning = newValue
      firePropertyChange(Ghost.RED_SPATIAL_BINNING_PROP, oldValue, newValue)
    }
  }

  private var blueExposureTime: Double = InstConstants.DEF_EXPOSURE_TIME
  def getBlueExposureTime: Double = blueExposureTime
  def setBlueExposureTime(newValue: Double): Unit = {
    val oldValue = getBlueExposureTime
    if (oldValue != newValue) {
      blueExposureTime = newValue
      firePropertyChange(Ghost.BLUE_EXPOSURE_TIME_PROP, oldValue, newValue)
    }
  }

  private var blueSpectralBinning: GhostSpectralBinning = GhostSpectralBinning.DEFAULT
  def getBlueSpectralBinning: GhostSpectralBinning = blueSpectralBinning
  def setBlueSpectralBinning(newValue: GhostSpectralBinning): Unit = {
    val oldValue = getBlueSpectralBinning
    if (oldValue != newValue) {
      blueSpectralBinning = newValue
      firePropertyChange(Ghost.BLUE_SPECTRAL_BINNING_PROP, oldValue, newValue)
    }
  }

  private var blueSpatialBinning: GhostSpatialBinning = GhostSpatialBinning.DEFAULT
  def getBlueSpatialBinning: GhostSpatialBinning = blueSpatialBinning
  def setBlueSpatialBinning(newValue: GhostSpatialBinning): Unit = {
    val oldValue = getBlueSpatialBinning
    if (oldValue != newValue) {
      blueSpatialBinning = newValue
      firePropertyChange(Ghost.BLUE_SPATIAL_BINNING_PROP, oldValue, newValue)
    }
  }

  private var blueReadNoiseGain: GhostReadNoiseGain = GhostReadNoiseGain.DEFAULT
  def getBlueReadNoiseGain: GhostReadNoiseGain = blueReadNoiseGain
  def setBlueReadNoiseGain(newValue: GhostReadNoiseGain): Unit = {
    val oldValue = getBlueReadNoiseGain
    if (oldValue != newValue) {
      blueReadNoiseGain = newValue
      firePropertyChange(Ghost.BLUE_READ_NOISE_GAIN_PROP, oldValue, newValue)
    }
  }
}

object Ghost {

  // Unfortunately we need a Java "Supplier" and "Function" which makes it
  // awkward to create the NodeInitializer via ComponentNodeInitializer.
  private val GhostSupplier: java.util.function.Supplier[Ghost] =
    new java.util.function.Supplier[Ghost] {
      def get(): Ghost = new Ghost()
    }

  private val GhostCbFactory: java.util.function.Function[ISPObsComponent, GhostCB] =
    new java.util.function.Function[ISPObsComponent, GhostCB] {
      def apply(oc: ISPObsComponent): GhostCB = new GhostCB(oc)
    }

  val NI: ISPNodeInitializer[ISPObsComponent, Ghost] =
    new ComponentNodeInitializer(SPComponentType.INSTRUMENT_GHOST, GhostSupplier, GhostCbFactory)

  val OBSERVATION_NI: ISPNodeInitializer[ISPObservation, SPObservation] = new ObservationNI(Instrument.Ghost.some()) {
    override protected def addTargetEnv(factory: ISPFactory, obsNode: ISPObservation): Unit = {
      Try {
        val p   = obsNode.getProgram
        val oc  = factory.createObsComponent(p, TargetObsComp.SP_TYPE, null)
        val toc = oc.getDataObject.asInstanceOf[TargetObsComp]

        // Create a single target GHOST asterism as the default.
        val a   = GhostAsterism.createEmptySingleTargetAsterism
        val env = TargetEnvironment.create(a)

        toc.setTargetEnvironment(env)
        oc.setDataObject(toc)
        obsNode.addObsComponent(oc)
      } match {
        case Success(_)               =>
          // Do nothing.
        case Failure(ex: SPException) =>
          throw new RuntimeException("Unable to create and initialize GHOST target environment", ex)
        case Failure(_)               =>
          // This should never happen.
          throw new RuntimeException("Unknown failure in creating GHOST target environment")
      }
    }
  }

  // The name of the Ghost instrument configuration.
  val INSTRUMENT_NAME_PROP: String = "GHOST"

  // The names of the base position / IFUs.
  val BaseRADegrees: String  = "baseRADeg"
  val BaseRAHMS: String      = "baseRAHMS"
  val BaseDecDegrees: String = "baseDecDeg"
  val BaseDecDMS: String     = "baseDecDMS"

  val SRIFU1Name: String     = "srifu1Name"
  val SRIFU1RADeg: String    = "srifu1CoordsRADeg"
  val SRIFU1DecDeg: String   = "srifu1CoordsDecDeg"
  val SRIFU1RAHMS: String    = "srifu1CoordsRAHMS"
  val SRIFU1DecDMS: String   = "srifu1CoordsDecDMS"

  val SRIFU2Name: String     = "srifu2Name"
  val SRIFU2RADeg: String    = "srifu2CoordsRADeg"
  val SRIFU2DecDeg: String   = "srifu2CoordsDecDeg"
  val SRIFU2RAHMS: String    = "srifu2CoordsRAHMS"
  val SRIFU2DecDMS: String   = "srifu2CoordsDecDMS"

  val HRIFU1Name: String     = "hrifu1Name"
  val HRIFU1RADeg: String    = "hrifu1CoordsRADeg"
  val HRIFU1DecDeg: String   = "hrifu1CoordsDecDeg"
  val HRIFU1RAHMS: String    = "hrifu1CoordsRAHMS"
  val HRIFU1DecDMS: String   = "hrifu1CoordsDecDMS"

  val HRIFU2Name: String     = "hrifu2Name"
  val HRIFU2RADeg: String    = "hrifu2CoordsRADeg"
  val HRIFU2DecDeg: String   = "hrifu2CoordsDecDeg"
  val HRIFU2RAHMS: String    = "hrifu2CoordsRAHMS"
  val HRIFU2DecDMS: String   = "hrifu2CoordsDecDMS"

  /** The properties supported by this class. */
  private def initProp(propName: String, query: Boolean, iter: Boolean): PropertyDescriptor = {
    PropertySupport.init(propName, classOf[Ghost], query, iter)
  }

  private val query_yes = true
  private val query_no  = false
  private val iter_yes  = true
  private val iter_no   = false

  val POS_ANGLE_PROP: PropertyDescriptor = initProp(InstConstants.POS_ANGLE_PROP, query = query_no, iter = iter_no)
  val PORT_PROP: PropertyDescriptor = initProp(IssPortProvider.PORT_PROPERTY_NAME, query = query_no, iter = iter_no)
  val ENABLE_FIBER_AGITATOR_PROP: PropertyDescriptor = initProp("enableFiberAgitator", query = query_no, iter = iter_no)
//  val ENABLE_IFU1_OIWFS_GUIDE_STAR: PropertyDescriptor = initProp("enableIfu1OiwfsGuideStar", query = query_no, iter = iter_no)
//  val ENABLE_IFU2_OIWFS_GUIDE_STAR: PropertyDescriptor = initProp("enableIfu2OiwfsGuideStar", query = query_no, iter = iter_no)
  val RED_EXPOSURE_TIME_PROP: PropertyDescriptor = initProp("redExposureTime", query = query_no, iter = iter_no)
  val RED_SPECTRAL_BINNING_PROP: PropertyDescriptor = initProp("redSpectralBinning", query = query_yes, iter = iter_yes)
  val RED_SPATIAL_BINNING_PROP: PropertyDescriptor = initProp("redSpatialBinning", query = query_yes, iter = iter_yes)
  val BLUE_EXPOSURE_TIME_PROP: PropertyDescriptor = initProp("blueExposureTime", query = query_no, iter = iter_no)
  val BLUE_SPECTRAL_BINNING_PROP: PropertyDescriptor = initProp("blueSpectralBinning", query = query_yes, iter = iter_yes)
  val BLUE_SPATIAL_BINNING_PROP: PropertyDescriptor = initProp("blueSpatialBinning", query = query_yes, iter = iter_yes)
  val BLUE_READ_NOISE_GAIN_PROP: PropertyDescriptor = initProp("blueReadNoiseGain", query = query_no, iter = iter_no)

  private val Properties: List[(String, PropertyDescriptor)] = List(
    POS_ANGLE_PROP,
    PORT_PROP,
    ENABLE_FIBER_AGITATOR_PROP,
//    ENABLE_IFU1_OIWFS_GUIDE_STAR,
//    ENABLE_IFU2_OIWFS_GUIDE_STAR,
    RED_EXPOSURE_TIME_PROP,
    RED_SPECTRAL_BINNING_PROP,
    RED_SPATIAL_BINNING_PROP,
    BLUE_EXPOSURE_TIME_PROP,
    BLUE_SPECTRAL_BINNING_PROP,
    BLUE_SPATIAL_BINNING_PROP,
    BLUE_READ_NOISE_GAIN_PROP
  ).map(p => (p.getName, p))

  private[ghost] val PropertyMap: JMap[String, PropertyDescriptor] = {
    Collections.unmodifiableMap(TreeMap(Properties: _*).asJava)
  }

  /** Currently, the instrument has no queryable configuration parameters. */
  val getInstConfigInfo: JList[InstConfigInfo] = List.empty[InstConfigInfo].asJava
}