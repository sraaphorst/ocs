package edu.gemini.spModel.target.env

import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.target.SPTarget

import scalaz._
import Scalaz._


sealed trait GuideGrp extends Serializable {
  /** Returns the set of guide probes with at least one associated guide star
    * in this environment.
    */
  def referencedGuiders: Set[GuideProbe] =
    this match {
      case AutomaticGroup.Initial    => Set.empty[GuideProbe]
      case AutomaticGroup.Active(ts) => ts.keySet
      case ManualGroup(_, ts)        => ts.keySet
    }

  /** Returns the set of guide probes with at least one associated primary
    * guide star in this environment.
    */
  def primaryReferencedGuiders: Set[GuideProbe] =
    this match {
      case AutomaticGroup.Initial    => Set.empty[GuideProbe]
      case AutomaticGroup.Active(ts) => ts.keySet
      case ManualGroup(_, ts)        => ts.filter(_._2.hasFocus).keySet
    }

  def targets: Map[GuideProbe, List[SPTarget]] =
    this match {
      case AutomaticGroup.Initial    => Map.empty
      case AutomaticGroup.Active(ts) => ts.mapValues(_ :: Nil)
      case ManualGroup(_, ts)        => ts.mapValues(_.toList)
    }
}

/** A manual group has a name and a mapping from guide probe to a non-empty list
  * of targets, of which exactly one is selected. Should it also have a UUID so
  * we can distinguish [temporarily] identical ones?
 */
final case class ManualGroup(name: String, targetMap: Map[GuideProbe, OptsList[SPTarget]]) extends GuideGrp

object ManualGroup {
  val Name: ManualGroup @> String                                 =
    Lens.lensu((mg, n) => mg.copy(name = n), _.name)

  val TargetMap: ManualGroup @> Map[GuideProbe, OptsList[SPTarget]] =
    Lens.lensu((mg,m) => mg.copy(targetMap = m), _.targetMap)

  implicit val TargetCollectionManualGroup: TargetCollection[ManualGroup] = new TargetCollection[ManualGroup] {
    override def cloneTargets(m: ManualGroup): ManualGroup =
      TargetMap.mod(_.mapValues { opts =>
        OptsList(opts.toDisjunction.bimap(_.map(_.clone()), _.map(_.clone())))
      }, m)

    override def containsTarget(m: ManualGroup, t: SPTarget): Boolean =
      m.targetMap.values.exists(opts => opts.any(_ == t))

    override def removeTarget(m: ManualGroup, t: SPTarget): ManualGroup =
      TargetMap.mod(_.mapValues(_.delete(t)).collect {
        case (probe, Some(opts)) => (probe, opts)
      }, m)
  }
}


sealed trait AutomaticGroup extends GuideGrp

object AutomaticGroup {

  /** The initial automatic group is a marker indicating that the target
    * environment is "new".
    */
  case object Initial extends AutomaticGroup

  /** An active bags group provides a 1:1 mapping from probe to target. If the
    * map is empty this is ok; it just means bags did not find any targets.
    */
  case class Active(targetMap: Map[GuideProbe, SPTarget]) extends AutomaticGroup

  val TargetMap: Active @> Map[GuideProbe, SPTarget] =
    Lens.lensu((a,m) => a.copy(targetMap = m), _.targetMap)

  implicit val TargetCollectionAutomaticGroup: TargetCollection[AutomaticGroup] = new TargetCollection[AutomaticGroup] {
    override def cloneTargets(a: AutomaticGroup): AutomaticGroup =
      a match {
        case AutomaticGroup.Initial    => a
        case AutomaticGroup.Active(ts) => AutomaticGroup.Active(ts.mapValues(_.clone()))
      }

    override def containsTarget(a: AutomaticGroup, t: SPTarget): Boolean =
      a match {
        case AutomaticGroup.Initial    => false
        case AutomaticGroup.Active(ts) => ts.exists { case (_, t0) => t == t0 }
      }

    override def removeTarget(a: AutomaticGroup, t: SPTarget): AutomaticGroup =
      a match {
        case Initial   => Initial
        case Active(m) => Active(m.filterNot(_._2 == t))
      }
  }
}

object GuideGrp {

  val Name: GuideGrp @?> String =
    PLens.plensgf({
      case mg: ManualGroup => n => mg.copy(name = n)
    }, {
      case mg: ManualGroup => mg.name
    })
}
