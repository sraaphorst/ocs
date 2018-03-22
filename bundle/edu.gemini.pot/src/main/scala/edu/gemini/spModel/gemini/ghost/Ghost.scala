package edu.gemini.spModel.gemini.ghost

import java.beans.PropertyDescriptor
import java.util.{Collections, List => JList, Map => JMap, Set => JSet}

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.data.config.{DefaultParameter, DefaultSysConfig, ISysConfig, StringParameter}
import edu.gemini.spModel.data.property.{PropertyProvider, PropertySupport}
import edu.gemini.spModel.obscomp.{InstConfigInfo, InstConstants, SPInstObsComp}
import edu.gemini.spModel.pio.{ParamSet, PioFactory}
import edu.gemini.spModel.seqcomp.SeqConfigNames

import scala.collection.immutable.TreeMap
import scala.collection.JavaConverters._


/** The GHOST instrument SP model.
  * Note that we do not override clone since private variables are immutable.
  */
final class Ghost extends SPInstObsComp(Ghost.SP_TYPE) with PropertyProvider {
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
    super.getParamSet(factory)
  }

  override def setParamSet(paramSet: ParamSet): Unit = {
    super.setParamSet(paramSet)
  }

  override def getSysConfig: ISysConfig = {
    val sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME)
    sc.putParameter(StringParameter.getInstance(ISPDataObject.VERSION_PROP, getVersion))
    sc.putParameter(DefaultParameter.getInstance(Ghost.POS_ANGLE_PROP, getPosAngle))
    sc
  }
}

object Ghost {
  val SP_TYPE: SPComponentType = SPComponentType.INSTRUMENT_GHOST

  // The name of the Ghost instrument configuration.
  val INSTRUMENT_NAME_PROP: String = "GHOST"

  /** The properties supported by this class.
    */
  private def initProp(propName: String, query: Boolean, iter: Boolean): PropertyDescriptor = {
    PropertySupport.init(propName, classOf[Ghost], query, iter)
  }

  private val query_yes = true
  private val query_no  = false
  private val iter_yes  = true
  private val iter_no   = false

  val POS_ANGLE_PROP: PropertyDescriptor = initProp(InstConstants.POS_ANGLE_PROP, query = query_no, iter = iter_no)

  // Use Java classes to be compatible with existing instruments.
  private val Properties: List[(String, PropertyDescriptor)] = List(
    POS_ANGLE_PROP.getName -> POS_ANGLE_PROP
  )

  private[ghost] val PropertyMap: JMap[String, PropertyDescriptor] = {
    Collections.unmodifiableMap(TreeMap(Properties: _*).asJava)
  }

  /** Currently, the instrument has no queryable configuration parameters.
    */
  val getInstConfigInfo: JList[InstConfigInfo] = List.empty.asJava
}