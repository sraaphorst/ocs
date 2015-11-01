package edu.gemini.dataman.gsa.query

import edu.gemini.dataman.gsa.query.JsonCodecs._
import edu.gemini.spModel.dataset.DatasetLabel

import argonaut._
import Argonaut._

import org.specs2.mutable.Specification

import scalaz._

object DatasetLabelCodecSpec extends Specification {

  val ExampleLabel = new DatasetLabel("GS-2015B-Q-1-2-3")

  "DatasetLabel encode" should {
    "produce a json string" in {
      ExampleLabel.asJson must_== jString("GS-2015B-Q-1-2-003")
    }
  }

  "DatasetLabel decode" should {
    "work for valid dataset labels" in {
      Parse.decodeEither[DatasetLabel](s""""${ExampleLabel.toString}"""") must_== \/-(ExampleLabel)
    }

    "fail for invalid labels" in {
      Parse.decodeEither[DatasetLabel](s""""foo"""") match {
        case -\/(m) => m.startsWith(invalidDatasetLabel("foo"))
        case _      => failure("expected to fail on input `foo`")
      }
    }
  }
}
