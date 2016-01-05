package edu.gemini.horizons.server.backend

import scalaz._
import Scalaz._
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop._
import org.scalacheck.Gen
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

object HS2Spec extends Specification with ScalaCheck {

  import HS2.{ Search, Row }
  import Searcher.search

  import edu.gemini.spModel.core.{ HorizonsDesignation => HD }


  "comet search" should {

    "handle empty results" in {
      search(Search.Comet("kjhdwekuq")) must_== \/-(Nil)
    }

    "handle multiple results" in {
      search(Search.Comet("hu")).map(_.take(5)) must_== \/-(List(      
        Row(HD.Comet("67P"), "Churyumov-Gerasimenko"), 
        Row(HD.Comet("106P"), "Schuster"), 
        Row(HD.Comet("130P"), "McNaught-Hughes"), 
        Row(HD.Comet("178P"), "Hug-Bell"), 
        Row(HD.Comet("C/1880 Y1"), "Pechule")
      ))
    }

    "handle single result (Format 1) Hubble (C/1937 P1)" in {
      search(Search.Comet("hubble")) must_== \/-(List(
        Row(HD.Comet("C/1937 P1"), "Hubble")
      ))
    }   

    "handle single result (Format 2) 1P/Halley pattern" in {
      search(Search.Comet("halley")) must_== \/-(List(
        Row(HD.Comet("1P"), "Halley")
      ))
    }   

  }

  "asteroid search" should {

    "handle empty results" in {
      search(Search.Asteroid("kjhdwekuq")) must_== \/-(Nil)
    }

    "handle multiple results" in {
      search(Search.Asteroid("her")).map(_.take(5)) must_== \/-(List(
        Row(HD.AsteroidOldStyle(103), "Hera"),
        Row(HD.AsteroidOldStyle(121), "Hermione"),
        Row(HD.AsteroidOldStyle(135), "Hertha"),
        Row(HD.AsteroidOldStyle(206), "Hersilia"),
        Row(HD.AsteroidOldStyle(214), "Aschera")
      ))
    }

    "handle single result (Format 1) 90377 Sedna (2003 VB12)" in {
      search(Search.Asteroid("sedna")) must_== \/-(List(
        Row(HD.AsteroidNewStyle("2003 VB12"), "Sedna")
      ))
    }   

    "handle single result (Format 2) 29 Amphitrite" in {
      search(Search.Asteroid("amphitrite")) must_== \/-(List(
        Row(HD.AsteroidOldStyle(29), "Amphitrite")
      ))
    }   

  }

  "major body search" should {

    "handle empty results" in {
      search(Search.MajorBody("kjhdwekuq")) must_== \/-(Nil)
    }

    "handle empty results with small-body fallthrough (many)" in {
      search(Search.MajorBody("hu")) must_== \/-(Nil)
    }

    "handle empty results with small-body fallthrough (single)" in {
      search(Search.MajorBody("hermione")) must_== \/-(Nil)
    }

    "handle multiple results" in {
      search(Search.MajorBody("mar")).map(_.take(5)) must_== \/-(List(
        Row(HD.MajorBody(4), "Mars Barycenter"),
        Row(HD.MajorBody(499), "Mars"),
        Row(HD.MajorBody(723), "Margaret")
      ))
    }

    "handle single result" in {
      search(Search.MajorBody("charon")) must_== \/-(List(
        Row(HD.MajorBody(901), "Charon / (Pluto)")
      ))
    }   

  }
}
