package edu.gemini.spModel.target.env

import edu.gemini.shared.util.immutable.{ImList, ImOption}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.guide.{GuideProbeMap, GuideProbe}
import edu.gemini.spModel.pio.xml.PioXmlFactory
import edu.gemini.spModel.target.SPTarget

import org.scalacheck.Prop._

import org.specs2.ScalaCheck

import org.specs2.mutable.Specification

import scala.collection.JavaConverters._
import scalaz._, Scalaz._

class GuideGroupSpec extends Specification with ScalaCheck with Arbitraries {

  import GuideGroupSpec.{AllProbes, equalGuideGroups}

  "GuideGroup name" should {
    "always be defined for manual groups, undefined for automatic" in
      forAll { (g: GuideGroup) =>
        g.grp match {
          case a: AutomaticGroup => g.getName == ImOption.empty
          case _                 => g.getName != ImOption.empty
        }
      }

    "be modifiable for manual groups but updates ignored for automatic groups" in
      forAll { (g: GuideGroup, n: String) =>
        val nopt = ImOption.apply(n)
        g.grp match {
          case a: AutomaticGroup => g.setName(nopt) == g
          case _                 => g.setName(nopt).getName == nopt && g.setName(n).getName == nopt
        }
      }
  }

  "GuideGroup contains" should {
    "be false for any guide probe for the initial automatic group" in
      forAll { (gp: GuideProbe) =>
        !GuideGroup(AutomaticGroup.Initial).contains(gp)
      }

    "be true iff there are targets associated with the guide probe" in
      forAll { (g: GuideGroup) =>
        AllProbes.forall { gp =>
          g.contains(gp) == g.get(gp).asScalaOpt.exists(_.getTargets.nonEmpty())
        }
      }
  }

  "GuideGroup get" should {
    "return none or else a non empty list" in
      forAll { (g: GuideGroup) =>
        AllProbes.forall { gp =>
          g.get(gp).asScalaOpt.forall(_.getTargets.nonEmpty)
        }
      }

    "return none or else GuideProbeTargets with a matching probe" in
      forAll { (g: GuideGroup) =>
        AllProbes.forall { gp =>
          g.get(gp).asScalaOpt.forall(_.getGuider == gp)
        }
      }

    "return a non empty GuideProbeTargets iff the group contains the probe" in
      forAll { (g: GuideGroup) =>
        AllProbes.forall { gp =>
          g.get(gp).asScalaOpt.isDefined == g.contains(gp)
        }
      }

    "return a GuideProbeTargets with primary star that matches the options list focus" in
      forAll { (g: GuideGroup) =>
        AllProbes.forall { gp =>
          g.get(gp).asScalaOpt.forall { gpt =>
            gpt.getPrimary.asScalaOpt == (g.grp match {
              case ManualGroup(_, m)        => m.get(gp).flatMap(_.focus)
              case AutomaticGroup.Active(m) => m.get(gp)
              case AutomaticGroup.Initial   => None
            })
          }
        }
      }
  }

  "GuideGroup put" should {
    "remove all guide stars if GuideProbeTargets is empty" in
      forAll { (g: GuideGroup) =>
        AllProbes.forall { gp =>
          val emptyGpt = GuideProbeTargets.create(gp)
          val g2       = g.put(emptyGpt)
          !g2.contains(gp) && g2.get(gp).isEmpty
        }
      }

    "remove the primary guide star if there is no primary in GuideProbeTargets" in
      forAll { (g: GuideGroup) =>
        AllProbes.forall { gp =>
          val noPrimaryGpt = GuideProbeTargets.create(gp, new SPTarget()).clearPrimarySelection()
          val g2 = g.put(noPrimaryGpt)
          g2.get(gp).asScalaOpt.forall(_.getPrimary.isEmpty)
        }
      }

    "remove the guider from automatic groups if there is no primary in GuideProbeTargets" in
      forAll { (g: GuideGroup) =>
        AllProbes.forall { gp =>
          val noPrimaryGpt = GuideProbeTargets.create(gp, new SPTarget()).clearPrimarySelection()
          val g2           = g.put(noPrimaryGpt)
          val gpt2         = g2.get(gp).asScalaOpt
          g2.grp match {
            case _: AutomaticGroup => gpt2.isEmpty
            case _: ManualGroup    => gpt2.exists(_.getPrimary.isEmpty)
          }
        }
      }

    "set guide stars associated with a probe but ignore non-primary guide stars for automatic groups" in
      forAll { (g: GuideGroup) =>
        val primaryTarget = new SPTarget() <| (_.setName("primary"))
        val notPrimaryTarget = new SPTarget() <| (_.setName("not primary"))
        AllProbes.forall { gp =>
          val gpt = GuideProbeTargets.create(gp, primaryTarget, notPrimaryTarget)
          val g2 = g.put(gpt)
          val lst2 = g2.get(gp).asScalaOpt.toList.flatMap(_.getTargets.asScalaList)
          g2.grp match {
            case _: AutomaticGroup => lst2 == List(primaryTarget)
            case _: ManualGroup    => lst2 == List(primaryTarget, notPrimaryTarget)
          }
        }
      }
  }

  "GuideGroup remove" should {
    "remove all targets associated with the given guider" in
      forAll { (g: GuideGroup) =>
        AllProbes.forall { gp =>
          val g2 = g.remove(gp)
          g2.get(gp).isEmpty && !g2.contains(gp)
        }
      }
  }

  "GuideGroup clear" should {
    "remove all targets" in
      forAll { (g: GuideGroup) =>
        val g2 = g.clear()
        g2.getAll.isEmpty && AllProbes.forall { gp =>
          g2.get(gp).isEmpty && !g2.contains(gp)
        }
      }
  }

  "GuideGroup getAll" should {
    "return its results in sorted order" in
      forAll { (g: GuideGroup) =>
        val probeOrder = AllProbes.zipWithIndex.toMap
        val order = g.getAll.asScalaList.map(gpt => probeOrder(gpt.getGuider))
        order == order.sorted
      }

    "return a result for each guider with associated guide stars" in
      forAll { (g: GuideGroup) =>
        val guiders = g.getAll.asScalaList.map(_.getGuider).toSet
        g.grp match {
          case AutomaticGroup.Active(m) => guiders == m.keySet
          case AutomaticGroup.Initial   => guiders.isEmpty
          case ManualGroup(_, m)        => guiders == m.keySet
        }
      }

    "return matching guide probe targets for all guide probes with associated targets" in
      forAll { (g: GuideGroup) =>
        val gpts1 = g.getAll.asScalaList
        val gpts2 = AllProbes.flatMap { gp => g.get(gp).asScalaOpt }
        gpts1 == gpts2
      }
  }

  "GuideGroup putAll" should {
    "behave the same as if a sequence of individual calls to put were made" in
      forAll { (g: GuideGroup, ts: ImList[GuideProbeTargets]) =>
        g.putAll(ts) === (g /: ts.asScalaList){(gg,gpt) => gg.put(gpt)}
      }

    "contain the original primary guide probes and all primary guide probes for which targets have been added" in
      forAll { (g: GuideGroup, ts: ImList[GuideProbeTargets]) =>
        val allGps = g.getPrimaryReferencedGuiders.asScala.toSet ++ ts.asScalaList.map(_.getGuider)
        g.putAll(ts).getPrimaryReferencedGuiders.asScala.toSet == allGps
      }
  }

  "GuideGroup setAll" should {
    "produce the same guide group if the group is cleared and the elements are added via putAll" in
      forAll { (g: GuideGroup, ts: ImList[GuideProbeTargets]) =>
        g.setAll(ts) === g.clear().putAll(ts)
      }

    "only contain the guide probes in the collection" in
      forAll { (g: GuideGroup, ts: ImList[GuideProbeTargets]) =>
        val g2 = g.setAll(ts)
        val gpOut = g.getReferencedGuiders.asScala.toSet -- g2.getReferencedGuiders.asScala
        g2.getReferencedGuiders.asScala.toSet == ts.asScalaList.map(_.getGuider).toSet &&
          gpOut.forall(gp => !g2.contains(gp))
      }
  }

  "GuideGroup getPrimaryReferencedGuiders" should {
    "contain a guider iff it has a primary target" in
      forAll { (g: GuideGroup) =>
        g.getAll.asScalaList.collect {
          case gpt if gpt.getPrimary.isDefined => gpt.getGuider
        }.toSet == g.getPrimaryReferencedGuiders.asScala.toSet
      }
  }

  "GuideGroup getReferencedGuiders" should {
    "contain a guider iff it has associated targets" in
      forAll { (g: GuideGroup) =>
        g.getAll.asScala.map(_.getGuider).toSet == g.getReferencedGuiders.asScala.toSet
      }
  }

  "GuideGroup removeTarget" should {
    "remove an existing target added to all guiders and preserve the order of the original targets" in
      forAll { (g: GuideGroup, target: SPTarget) =>
        val oldGpts = g.getAll
        val newGpts = g.getAll.asScalaList.map(gt => gt.update(OptionsList.UpdateOps.append(target))).asImList
        val remGpts = g.setAll(newGpts).removeTarget(target).getAll
        remGpts == oldGpts
      }

    "remove an empty mapping from guide probe to targets" in
      forAll { (g: GuideGroup) =>
        g.getAll.asScalaList.forall { ts =>
          val gNew = (g/:ts.getTargets.asScalaList){ (gg,t) => gg.removeTarget(t) }
          !gNew.contains(ts.getGuider)
        }
      }

    "maintain the primary target correctly when removing the primary target for a probe" in
      forAll { (name: String, gp: GuideProbe, lefts: List[SPTarget], focus: SPTarget, rights: List[SPTarget]) =>
        def unrollGroupOptsList[A](gg2: GuideGroup): List[SPTarget] = gg2.grp match {
          case ManualGroup(_, tm) => tm.get(gp).fold(List.empty[SPTarget]) { ol =>
            ol.focus.fold(List.empty[SPTarget])(t => t :: unrollGroupOptsList(gg2.removeTarget(t)))
          }
          case _ => Nil
        }

        val gg = new GuideGroup(ManualGroup(name, List(gp -> OptsList.focused(lefts, focus, rights)).toMap))
        val expectedOrder = focus :: (rights ++ lefts.reverse)
        val actualOrder   = unrollGroupOptsList(gg)
        expectedOrder === actualOrder
      }
  }

  "GuideGroup getAllMatching" should {
    "partition the collections of guide probe targets by guide probe type" in
      forAll { (g: GuideGroup) =>
        val ptns = g.getReferencedGuiders.asScala.map(gp => g.getAllMatching(gp.getType).asScalaList.toSet).toSet
        (ptns.toList.map(_.length).sum == g.getAll.size) && ptns.forall(p => ptns.forall(q => p == q || p.intersect(q).isEmpty))
      }
  }

  "GuideGroup cloneTargets" should {
    "create a new GuideGroup where the underlying ITargets are equal for each guide probe and in the same order" in
      forAll { (g: GuideGroup) =>
        val cg  = g.cloneTargets
        val ts  = g.getAll.asScalaList
        val cts = cg.getAll.asScalaList
        (ts.length == cts.length) && ts.zip(cts).forall {
          case (t1, t2) => t1.getGuider == t2.getGuider && t1.getTargets.asScala.map(_.getTarget) == t2.getTargets.asScala.map(_.getTarget)
        }
      }
  }

  "GuideGroup getAllContaining" should {
    "return a subset of guide probe targets containing a specific target for manual groups" in
    forAll { (g: GuideGroup, t: SPTarget) =>
      val newGpt = g.getAll.asScalaList.zipWithIndex.map { case (gpt, idx) => if (idx % 2 == 0) gpt else gpt.update(OptionsList.UpdateOps.append(t)) }
      g.setAll(newGpt.asImList).getAllContaining(t).asScalaList == (g.grp match {
        case _: ManualGroup    => newGpt.zipWithIndex.collect { case (gpt, idx) if idx % 2 == 1 => gpt }
        case _: AutomaticGroup => Nil
      })
    }
  }

  "GuideGroup getParamSet" should {
    "produce a ParamSet that can be read via fromParamSet to result in an equivalent guide group" in
    forAll { (g: GuideGroup) =>
      equalGuideGroups(g, GuideGroup.fromParamSet(g.getParamSet(new PioXmlFactory())))
    }
  }

  // TODO: Restore this test case once serialization is achieved.
//  "GuideGroup" should {
//    "properly serialize and deserialize" in
//    forAll { (g: GuideGroup) =>
//      val bao = new ByteArrayOutputStream()
//      val oos = new ObjectOutputStream(bao)
//      oos.writeObject(g)
//      oos.close()
//
//      val ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray))
//      ois.readObject() match {
//        case g2: GuideGroup => equalGuideGroups(g, g2)
//        case _              => false
//      }
//    }
//  }
}

object GuideGroupSpec {
  val AllProbes: List[GuideProbe] = GuideProbeMap.instance.values.asScala.toList.sorted

  def equalGuideGroups(gg1: GuideGroup, gg2: GuideGroup): Boolean = {
    implicit val EqualSPTarget: Equal[SPTarget] = Equal.equal((t1,t2) => t1.getTarget.getName === t2.getTarget.getName)
    (gg1.grp, gg2.grp) match {
      case (AutomaticGroup.Initial, AutomaticGroup.Initial)         => true
      case (AutomaticGroup.Active(tm1), AutomaticGroup.Active(tm2)) => tm1 === tm2
      case (ManualGroup(n1, tm1), ManualGroup(n2, tm2))             => n1 === n2 && tm1 === tm2
      case _ => false
    }
  }
}
