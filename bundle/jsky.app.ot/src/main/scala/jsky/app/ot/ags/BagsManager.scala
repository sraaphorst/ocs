package jsky.app.ot.ags

import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.util.concurrent.TimeoutException
import java.util.concurrent._
import java.util.logging.{Level, Logger}

import edu.gemini.ags.api.{AgsRegistrar, AgsStrategy}
import edu.gemini.catalog.votable.{CatalogException, GenericError}
import edu.gemini.pot.sp.{ISPObservationContainer, ISPNode, ISPProgram, ISPObservation, SPNodeKey}
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.obs.ObservationStatus
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.target.env.AutomaticGroup
import jsky.app.ot.OT
import jsky.app.ot.gemini.altair.Altair_WFS_Feature
import jsky.app.ot.gemini.inst.OIWFS_Feature
import jsky.app.ot.gemini.tpe.TpePWFSFeature
import jsky.app.ot.tpe.{TpeManager, TpeContext}

import scala.collection.JavaConverters._
import scala.concurrent.{Future, ExecutionContext}
import scala.swing.Swing
import scala.util.{Success, Failure}

import scalaz._, Scalaz._


final class BagsManager(executorService: ExecutorService) {
  private val LOG: Logger = Logger.getLogger(getClass.getName)

  private implicit val executionContext = new ExecutionContext {
    def shutdown() = executorService.shutdown()
    override def reportFailure(t: Throwable): Unit = throw t
    override def execute(runnable: Runnable): Unit = {
      executorService.submit(new Thread(runnable) {
        setPriority(Thread.NORM_PRIORITY - 1)
      })
    }
  }

  /** Our state is just a set of programs to watch, and a set of pending keys. */
  private case class BagsState(programs: Set[SPProgramID], keys: Set[SPNodeKey]) {
    def +(key: SPNodeKey): BagsState = copy(keys = keys + key)
    def -(key: SPNodeKey): BagsState = copy(keys = keys - key)
    def +(pid: SPProgramID): BagsState = copy(programs = programs + pid)
    def -(pid: SPProgramID): BagsState = copy(programs = programs - pid)
  }
  @volatile private var state: BagsState = BagsState(Set.empty, Set.empty)

  /**
    * Atomically add a program to our watch list and attach listeners.
    * Enqueue all observations for consideration for a BAGS lookup.
    */
  def watch(prog: ISPProgram): Unit = {
    synchronized {
      state += prog.getProgramID
      prog.addStructureChangeListener(StructurePropertyChangeListener)
      prog.addCompositeChangeListener(CompositePropertyChangeListener)
    }
    prog.getAllObservations.asScala.foreach(enqueue(_, 0L))
  }

  /**
    * Atomically remove a program from our watch list and remove listeners. Any queued observations
    * will be discarded as they come up for consideration.
    */
  def unwatch(prog: ISPProgram): Unit = {
    synchronized {
      state -= prog.getProgramID
      prog.removeStructureChangeListener(StructurePropertyChangeListener)
      prog.removeCompositeChangeListener(CompositePropertyChangeListener)
    }
  }

  /**
    * Remove the specified key from the task queue, if present. Return true if the key was present
    * *and* the specified program is still on the watch list.
    */
  private def dequeue(key: SPNodeKey, pid: SPProgramID): Boolean =
    synchronized {
      if (state.keys(key)) {
        state -= key
        state.programs(pid)
      } else false
    }

  /**
    * Atomically enqueue a task that will consider the specified observation for BAGS lookup, after
    * a delay of at least `delay` milliseconds.
    */
  def enqueue(observation: ISPObservation, delay: Long): Unit = {
    def isEligibleForBags(ctx: ObsContext): Boolean = {
      ctx.getTargets.getGuideEnvironment.guideEnv.auto match {
        case AutomaticGroup.Initial   => true
        case AutomaticGroup.Active(_) => true
        case _                        => false
      }
    }

    Option(observation).foreach { obs =>
      synchronized {
        val key = obs.getNodeKey
        state += key
        Future {
          // If dequeue is false this means that (a) another task scheduled *after* me ended up
          // running before me, so their result is as good as mine would have been and we're done;
          // or (b) we don't care about that program anymore, so we're done.
          if (dequeue(key, obs.getProgramID)) {
            // Otherwise construct an obs context, verify that it's bagworthy, and go
            ObsContext.create(obs).asScalaOpt.filter(isEligibleForBags).foreach { ctx =>
              //   do the lookup
              //   on success {
              //      if we're in the queue again, it means something changed while this task was
              //      running, so discard this result and do nothing,
              //      otherwise update the model
              //   }
              //   on failure enqueue again, maybe with a delay depending on the failure
              val bagsIdMsg = s"BAGS lookup on thread=${Thread.currentThread.getId} for observation=${obs.getObservationID}"
              LOG.info(s"Performing $bagsIdMsg.")

              AgsRegistrar.currentStrategy(ctx).foreach { strategy =>
                val fut = strategy.select(ctx, OT.getMagnitudeTable)
                fut onComplete {
                  case Success(opt) =>
                    // If this observation is once again in the queue, then something changed while this task
                    // was running, so discard the result.
                    if (!state.keys(key)) {
                      LOG.info(s"$bagsIdMsg successful. Results=${opt ? "Yes" | "No"}.")
                      if (ObservationStatus.computeFor(obs) != ObservationStatus.OBSERVED) {
                        Swing.onEDT {
                          obs.getProgram.removeCompositeChangeListener(CompositePropertyChangeListener)
                          obs.getProgram.removeStructureChangeListener(StructurePropertyChangeListener)
                          BagsManager.applyResults(TpeContext(obs), opt)
                          obs.getProgram.addStructureChangeListener(StructurePropertyChangeListener)
                          obs.getProgram.addCompositeChangeListener(CompositePropertyChangeListener)
                        }
                      }
                    }


                  // We don't want to print the stack trace if the host is simply unreachable.
                  // This is reported only as a GenericError in a CatalogException, unfortunately.
                  case Failure(CatalogException((e: GenericError) :: _)) =>
                    LOG.warning(s"$bagsIdMsg failed: ${e.msg}")
                    enqueue(obs, 5000L)

                  // If we timed out, we don't want to delay.
                  case Failure(ex: TimeoutException) =>
                    LOG.warning(s"$bagsIdMsg failed: ${ex.getMessage}")
                    enqueue(obs, 0L)

                  // For all other exceptions, print the full stack trace.
                  case Failure(ex) =>
                    LOG.log(Level.WARNING, s"$bagsIdMsg} failed.", ex)
                    enqueue(obs, 5000L)
                }
              }
            }
          }
        }
      }
    }
  }

  object CompositePropertyChangeListener extends PropertyChangeListener {
    override def propertyChange(evt: PropertyChangeEvent): Unit =
      evt.getSource match {
        case node: ISPNode => enqueue(node.getContextObservation, 0L)
        case _             => // Ignore
      }
  }

  object StructurePropertyChangeListener extends PropertyChangeListener {
    override def propertyChange(evt: PropertyChangeEvent): Unit =
      evt.getSource match {
        case cont: ISPObservationContainer => cont.getAllObservations.asScala.foreach(enqueue(_, 0L))
        case _                             => // Ignore
      }
  }
}

object BagsManager {
  val instance = {
    // Limit execution of futures in the BagsManager instance to an execution context with NumWorkers threads.
    val NumWorkers = math.max(1, Runtime.getRuntime.availableProcessors - 1)
    val executor = Executors.newFixedThreadPool(NumWorkers)
    new BagsManager(executor)
  }

  private def applyResults(ctx: TpeContext, selOpt: Option[AgsStrategy.Selection]): Unit = {
    applySelection(ctx, selOpt)
    showTpeFeatures(selOpt)
  }

  /** Update the TPE if it is visible */
  private def showTpeFeatures(selOpt: Option[AgsStrategy.Selection]): Unit =
    selOpt.foreach { sel =>
      Option(TpeManager.get()).filter(_.isVisible).foreach { tpe =>
        sel.assignments.foreach { ass =>
          val clazz = ass.guideProbe.getType match {
            case GuideProbe.Type.AOWFS => classOf[Altair_WFS_Feature]
            case GuideProbe.Type.OIWFS => classOf[OIWFS_Feature]
            case GuideProbe.Type.PWFS  => classOf[TpePWFSFeature]
          }
          Option(tpe.getFeature(clazz)).foreach(tpe.selectFeature)
        }
      }
    }

  private def applySelection(ctx: TpeContext, selOpt: Option[AgsStrategy.Selection]): Unit =
    selOpt.foreach { sel =>
      val oldEnv = ctx.targets.envOrDefault

      // Calculate the new target environment and if they are different, apply them.
      ctx.targets.dataObject.foreach { targetComp =>
        val newEnv = sel.applyTo(oldEnv)
        if (newEnv != oldEnv) {
          targetComp.setTargetEnvironment(newEnv)
          ctx.targets.commit()
        }
      }

      // See if the pos angle has changed and set it.
      // TODO: This doesn't seem right. The pos angle should only be set if the group is active?
      // TODO: Are pos angles a guide group property?
      ctx.instrument.dataObject.foreach { inst =>
        val deg = sel.posAngle.toDegrees
        val old = inst.getPosAngleDegrees
        if (deg != old) {
          inst.setPosAngleDegrees(deg)
          ctx.instrument.commit()
        }
      }
    }
}