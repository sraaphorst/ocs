package edu.gemini.seqexec.server

import edu.gemini.seqexec.server.SeqexecFailure._
import edu.gemini.spModel.config2.{Config, ConfigSequence, ItemKey}
import edu.gemini.spModel.obscomp.InstConstants.INSTRUMENT_NAME_PROP
import edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_KEY

import scalaz._
import Scalaz._

import scalaz.concurrent.Task

/**
 * Created by jluhrs on 4/28/15.
 */
final class Executor(sequenceConfig: ConfigSequence) {

  def execute: TrySeq[List[StepResult]] =
    executeSequence(sequenceConfig.getAllSteps.toList).runSeqAction

  private def executeSequence(config: List[Config]): SeqAction[List[StepResult]] = {
    config.traverseU(executeStep)
  }

  private def executeStep(stepConfig: Config): SeqAction[StepResult] = {
    val instName = stepConfig.getItemValue(new ItemKey(INSTRUMENT_KEY, INSTRUMENT_NAME_PROP))
    val instrument = instName match {
      case GMOS_S.name => Some(GMOS_S)
      case _           => None
    }

    instrument map { a =>
      val systems: List[System] = List(TCS, a)
      for {
        r <- NondeterminismSeq.gather(systems.map(_.configure(stepConfig)))
        s <- a.observe(stepConfig)
      } yield StepResult(r, s)
    } getOrElse SeqAction.fail(UnrecognizedInstrument(instName.toString))
  }
}

object Executor {
  def apply(sequenceConfig: ConfigSequence): Executor = new Executor(sequenceConfig)
}

final class StepResult(configResults: List[ConfigResult], observeResult: ObserveResult)
object StepResult {
  def apply(configResults: List[ConfigResult], observeResult: ObserveResult): StepResult =
    new StepResult(configResults, observeResult)
}