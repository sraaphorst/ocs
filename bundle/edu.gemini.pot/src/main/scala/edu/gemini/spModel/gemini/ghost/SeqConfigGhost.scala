package edu.gemini.spModel.gemini.ghost

import java.beans.PropertyDescriptor
import java.util.function.Supplier
import java.util.{Collections, Map => JMap}

import edu.gemini.pot.sp.{ISPNodeInitializer, ISPSeqComponent, SPComponentType}
import edu.gemini.spModel.data.property.{PropertyFilter, PropertyProvider, PropertySupport}
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer
import edu.gemini.spModel.seqcomp.{SeqConfigNames, SeqConfigObsBase}

final class SeqConfigGhost extends SeqConfigObsBase(SeqConfigGhost.SPType, SeqConfigGhost.SystemName) with PropertyProvider {
  override def getProperties(): JMap[String, PropertyDescriptor] = SeqConfigGhost.PropertyMap
}

object SeqConfigGhost {
  val SPType: SPComponentType = SPComponentType.ITERATOR_GHOST
  val SystemName: String = SeqConfigNames.INSTRUMENT_CONFIG_NAME

  val NI: ISPNodeInitializer[ISPSeqComponent, SeqConfigGhost] = new ComponentNodeInitializer[ISPSeqComponent, SeqConfigGhost](SPType,
    new Supplier[SeqConfigGhost] {
      override def get(): SeqConfigGhost = new SeqConfigGhost
    },
    new Supplier[SeqConfigGhostCB] {
      override def get(): SeqConfigGhostCB = new SeqConfigGhostCB
    }
  )

  val PropertyMap: JMap[String, PropertyDescriptor] = Collections.unmodifiableMap(PropertySupport.filter(PropertyFilter.ITERABLE_FILTER, Ghost.PropertyMap))
}