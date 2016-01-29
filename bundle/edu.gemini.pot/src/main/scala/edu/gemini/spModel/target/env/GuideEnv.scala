package edu.gemini.spModel.target.env

import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.target.SPTarget

import scalaz._
import Scalaz._

import GuideEnv.{Auto, Manual}

final case class GuideEnv(auto: AutomaticGroup, manual: Option[OptsList[ManualGroup]]) {

  def groups: List[GuideGrp] =
    auto :: (manual.map(_.toList) | Nil)

  def primaryGroup: GuideGrp =
    (manual.flatMap(_.focus) : Option[GuideGrp]) | auto

  def primaryIndex: Int =
    manual.flatMap(_.focusIndex).map(_ + 1).getOrElse(0)

  /** Returns all guide probes associated with a guide star, whether or not
    * they are configured to be used in this observation.
    */
  def referencedGuiders: Set[GuideProbe] =
    groups.foldMap(_.referencedGuiders)

  /** Returns the guide probes that will actually be in use to execute the
    * observation.
    */
  def primaryReferencedGuiders: Set[GuideProbe] =
    primaryGroup.primaryReferencedGuiders

  def length: Int =
    1 + manual.map(_.length).orZero

  def selectPrimaryIndex(i: Int): Option[GuideEnv] =
    if (i == 0) some(Manual.mod(_.map(_.clearFocus), this))
    else manual.flatMap { _.focusOnIndex(i-1) }.map { opts =>
      Manual.set(this, some(opts))
    }

  def toList: List[GuideGrp] =
    auto :: manual.map(_.toList).orZero

  def toNel: NonEmptyList[GuideGrp] =
    NonEmptyList.nel(auto, manual.map(_.toList).orZero)
}

/** A guide environment is a bags group (possibly empty or "initial") followed
  * by zero or more manual groups. One is always selected. If the second
  * element in the pair is a list, it means the bags group is selected.
  * Otherwise the selection is indicated by the zipper.
  */
object GuideEnv {
  val initial: GuideEnv = GuideEnv(AutomaticGroup.Initial, none)

  val Auto: GuideEnv @> AutomaticGroup =
    Lens.lensu((ge,a) => ge.copy(auto = a), _.auto)

  val Manual: GuideEnv @> Option[OptsList[ManualGroup]] =
    Lens.lensu((ge,m) => ge.copy(manual = m), _.manual)

  import TargetCollection._

  implicit val TargetCollectionGuideEnv: TargetCollection[GuideEnv] = new TargetCollection[GuideEnv] {
    def mod(ge: GuideEnv)(fa: AutomaticGroup => AutomaticGroup, fm: ManualGroup => ManualGroup): GuideEnv = {
      val s: State[GuideEnv, Unit] = (Auto %== fa) *> (Manual %== (_.map(_.map(fm))))
      s.exec(ge)
    }

    override def cloneTargets(ge: GuideEnv): GuideEnv =
      mod(ge)(_.cloneTargets, _.cloneTargets)

    override def containsTarget(ge: GuideEnv, t: SPTarget): Boolean =
      ge.auto.containsTarget(t) || ge.manual.exists(_.toList.exists(_.containsTarget(t)))

    override def removeTarget(ge: GuideEnv, t: SPTarget): GuideEnv =
      mod(ge)(_.removeTarget(t), _.removeTarget(t))

    type TargetMap = GuideProbe ==>> NonEmptyList[SPTarget]

    override def targets(ge: GuideEnv): TargetMap = {
      def merge(tm0: TargetMap, tm1: TargetMap): TargetMap =
        tm0.intersectionWith(tm1)(_.append(_)) union tm0 union tm1

      (ge.auto.targets :: ge.manual.fold(List.empty[TargetMap]) {
        _.toList.map(_.targets)
      }).reduceLeft(merge)
    }
  }

  implicit val EqualGuideEnv: Equal[GuideEnv] = Equal.equal { (ge0, ge1) =>
    ge0.auto === ge1.auto && ge0.manual === ge1.manual
  }
}