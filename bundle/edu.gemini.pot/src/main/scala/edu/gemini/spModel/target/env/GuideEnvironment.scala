package edu.gemini.spModel.target.env

import edu.gemini.shared.util.immutable.{ImList, Option => GemOption}
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.pio.{PioFactory, ParamSet}
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetCollection._

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

import GuideEnvironment._

/**
 *
 */
final case class GuideEnvironment(guideEnv: GuideEnv) extends TargetContainer {

  def getReferencedGuiders: java.util.SortedSet[GuideProbe] =
    toSortedSet(guideEnv.referencedGuiders)

  def getPrimaryReferencedGuiders: java.util.SortedSet[GuideProbe] =
    toSortedSet(guideEnv.primaryReferencedGuiders)

  override def containsTarget(target: SPTarget): Boolean =
    guideEnv.groups.exists { _.containsTarget(target) }

  /** Removes the target from any manual groups in which it is found. */
  override def removeTarget(target: SPTarget): GuideEnvironment =
    GuideEnvironment(guideEnv.removeTarget(target))

  // TODO: REFERENCE
  def removeGroup(grp: GuideGroup): GuideEnvironment =
    ???

  override def cloneTargets(): GuideEnvironment =
    ???

  override def getTargets: ImList[SPTarget] =
    ???

  def getOptions: ImList[GuideGroup] =
    ???

  // TODO: REFERENCE
  // TODO: only used by BagsManager so can be removed when BagsManager is updated
  def update(op: OptionsList.Op[GuideGroup]): GuideEnvironment =
    ???

  def setOptions(newList: ImList[GuideGroup]): GuideEnvironment =
    ???

  // TODO: primary is always defined, remove the option wrapper
  def getPrimary: GemOption[GuideGroup] =
    ???

  // TODO: REFERENCE
  def setPrimary(primary: GuideGroup): GuideEnvironment =
    ???

  def mkString(prefix: String, sep: String, suffix: String): String =
    ???

  def getPrimaryIndex: java.lang.Integer =
    ???

  def setPrimaryIndex(primary: Int): GuideEnvironment =
    ???

  // TODO: REFERENCE
  def selectPrimary(primary: GuideGroup): GuideEnvironment =
    ???

  def iterator(): java.util.Iterator[GuideGroup] =
    ???

  // TODO: REFERENCE
  def putGuideProbeTargets(grp: GuideGroup, gpt: GuideProbeTargets): GuideEnvironment =
    ???

  def getParamSet(f: PioFactory): ParamSet =
    ???
}

object GuideEnvironment {
  val ParamSetName = "guideEnv"
  val Initial: GuideEnvironment = GuideEnvironment(GuideEnv.initial)

  def create(guideGroups: OptionsList[GuideGroup]): GuideEnvironment =
    ???

  def fromParamSet(parent: ParamSet): GuideEnvironment =
    ???

  private def toSortedSet(s: Set[GuideProbe]): java.util.SortedSet[GuideProbe] =
    new java.util.TreeSet(GuideProbe.KeyComparator.instance) <|
      (_.addAll(s.asJavaCollection))
}
