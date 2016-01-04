package edu.gemini.spModel.target.env

import edu.gemini.shared.util.immutable.ImOption
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.guide.GuideProbe

import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop._
import org.scalacheck.Gen

import org.specs2.ScalaCheck
import org.specs2.matcher.MatchResultLogicalCombinators.MatchResultCombinator

import org.specs2.mutable.Specification

class GuideGroupSpec extends Specification with ScalaCheck with Arbitraries {
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
          case _                 => g.setName(nopt).getName == nopt &&
                                    g.setName(n).getName == nopt
        }
      }
  }

  "GuideGroup contains" should {
    "be false for any guide probe for the initial automatic group" in
      forAll { (gp: GuideProbe) =>
        !GuideGroup(AutomaticGroup.Initial).contains(gp)
      }

    "be true iff there are targets associated with the guide probe" in
      forAll { (gp: GuideProbe, g: GuideGroup) =>
        g.contains(gp) == g.get(gp).asScalaOpt.exists(_.getTargets.nonEmpty())
      }
  }

//  "GuideGroup get" should {
//
//  }
}
