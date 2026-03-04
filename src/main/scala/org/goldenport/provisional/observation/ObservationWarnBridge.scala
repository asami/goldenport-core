package org.goldenport.provisional.observation

import org.goldenport.observation.Descriptor

/*
 * @since   Mar.  4, 2026
 * @version Mar.  4, 2026
 * @author  ASAMI, Tomoharu
 */
object ObservationRender {
  def warnMessage(observation: Observation): String = {
    val attrs = ObservationProject.warnAttributes(observation)
    val head = s"${observation.taxonomy.category.name}/${observation.taxonomy.symptom.name}"
    val context = _context_tokens(attrs)
    val detail = attrs.get("message")
    (context, detail) match {
      case ("", None) => head
      case ("", Some(d)) => s"$head - $d"
      case (c, None) => s"$head [$c]"
      case (c, Some(d)) => s"$head [$c] - $d"
    }
  }

  private def _context_tokens(attrs: Map[String, String]): String =
    Vector("component", "class.name", "artifact.id", "repository.type")
      .flatMap(k => attrs.get(k).map(v => s"$k=$v"))
      .mkString(",")
}

object ObservationProject {
  def warnAttributes(observation: Observation): Map[String, String] = {
    val pairs = Vector(
      Some("phenomenon" -> observation.phenomenon.value),
      Some("taxonomy.category" -> observation.taxonomy.category.name),
      Some("taxonomy.symptom" -> observation.taxonomy.symptom.name),
      observation.cause.kind.map(x => "cause.kind" -> x.name),
      observation.cause.getEffectiveMessage.map(x => "message" -> x)
    ) ++ _facet_pairs(observation.cause.descriptor.facets)
    pairs.flatten.toMap
  }

  private def _facet_pairs(
    facets: Vector[Descriptor.Facet]
  ): Vector[Option[(String, String)]] =
    Vector(
      _first(facets, { case Descriptor.Facet.Component(name) => "component" -> name }),
      _first(facets, { case Descriptor.Facet.Operation(name) => "operation" -> name }),
      _first(facets, { case Descriptor.Facet.ClassName(name) => "class.name" -> name }),
      _first(facets, { case Descriptor.Facet.Artifact(id) => "artifact.id" -> id }),
      _first(facets, { case Descriptor.Facet.RepositoryType(name) => "repository.type" -> name }),
      _first(facets, { case Descriptor.Facet.Exception(e) => "exception.class" -> e.getClass.getName }),
      _first(facets, { case Descriptor.Facet.Exception(e) => "exception.message" -> Option(e.getMessage).getOrElse("") })
    )

  private def _first(
    facets: Vector[Descriptor.Facet],
    pf: PartialFunction[Descriptor.Facet, (String, String)]
  ): Option[(String, String)] =
    facets.collectFirst(pf)
}
