package jsky.app.ot.gemini.editor.targetComponent

import edu.gemini.spModel.obs.context.ObsContext

import jsky.app.ot.ags.AgsContext

import scala.swing._

class GuidingControls extends GridBagPanel {
  opaque = false

  private object guiderLabel extends Label {
    text                = "Guide with:"
    horizontalAlignment = Alignment.Right
    opaque              = false
  }

  layout(guiderLabel) = new Constraints {
    gridx  = 0
    insets = new Insets(0, 0, 0, 0)
  }

  val autoGuideStarGuiderSelector = new AgsStrategyCombo

  layout(Component.wrap(autoGuideStarGuiderSelector.getUi)) = new Constraints {
    gridx  = 1
    insets = new Insets(0, 5, 0, 10)
  }

  val manualGuideStarButton = new Button("Manual GS")

  layout(manualGuideStarButton) = new Constraints {
    gridx  = 2
    insets = new Insets(0, 0, 0, 5)
  }

  def update(ctxOpt: edu.gemini.shared.util.immutable.Option[ObsContext]): Unit = {
    autoGuideStarGuiderSelector.setAgsOptions(AgsContext.create(ctxOpt))
  }

  def supportsAgs_=(supports: Boolean): Unit = {
    guiderLabel.visible = supports
    autoGuideStarGuiderSelector.getUi.setVisible(supports)
  }

}
