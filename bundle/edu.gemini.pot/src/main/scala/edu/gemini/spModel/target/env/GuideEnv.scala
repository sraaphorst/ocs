package edu.gemini.spModel.target.env

import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.target.SPTarget

import scalaz._
import Scalaz._

final case class GuideEnv(auto: AutomaticGroup, manual: Option[OptsList[ManualGroup]]) {

  def groups: List[GuideGrp] =
    auto :: (manual.map(_.toList) | Nil)

  def primaryGroup: GuideGrp =
    (manual.flatMap(_.focus) : Option[GuideGrp]) | auto

  def referencedGuiders: Set[GuideProbe] =
    groups.foldMap(_.referencedGuiders)

  def primaryReferencedGuiders: Set[GuideProbe] =
    primaryGroup.referencedGuiders
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
//    def mod(fa: AutomaticGroup => AutomaticGroup, fm: ManualGroup => ManualGroup): State[GuideEnv, Unit] =
//      (Auto %== fa) *> (Manual %== (_.map(_.map(fm))))

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
  }
}