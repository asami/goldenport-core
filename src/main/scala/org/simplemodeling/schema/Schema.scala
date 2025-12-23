package org.simplemodeling.schema

/*
 * Core schema definition shared across front-side protocol layers
 * (CLI / REST / MCP) and runtime layers (CNCF).
 *
 * Idiom:
 *   - ValueBackedAbstractObject (structural idiom)
 *
 * Design notes:
 * - This core schema is intentionally non-generic.
 * - Schema.Core holds abstract Column instances so that CNCF-extended
 *   columns (e.g. SemanticColumn) are preserved and returned as-is.
 */
/*
 * @since   Dec. 23, 2025
 * @version Dec. 23, 2025
 * @author  ASAMI, Tomoharu
 */
abstract class Schema extends Schema.Core.Holder {
  def core: Schema.Core
}

object Schema {

  // ------------------------------------------------------------------
  // Core
  // ------------------------------------------------------------------

  final case class Core(
    columns: Vector[Column]
  )

  object Core {

    trait Holder {
      def core: Core
      final def columns: Vector[Column] = core.columns
    }
  }

  // ------------------------------------------------------------------
  // Column
  // ------------------------------------------------------------------

  /*
   * Column definition.
   *
   * Idiom:
   *   - ValueBackedAbstractObject (structural idiom)
   */
  abstract class Column extends Column.Core.Holder {
    def core: Column.Core
  }

  object Column {

    final case class Core(
      name: String,
      datatype: Datatype,
      multiplicity: Multiplicity,
      size: SizeConstraint
    )

    object Core {

      trait Holder {
        def core: Core

        final def name: String = core.name
        final def datatype: Datatype = core.datatype
        final def multiplicity: Multiplicity = core.multiplicity
        final def size: SizeConstraint = core.size
      }
    }

    final case class Instance(
      core: Core
    ) extends Column

    def apply(
      name: String,
      datatype: Datatype,
      multiplicity: Multiplicity,
      size: SizeConstraint = SizeConstraint.unbounded
    ): Column =
      Instance(
        Core(
          name,
          datatype,
          multiplicity,
          size
        )
      )
  }

  // ------------------------------------------------------------------
  // Multiplicity
  // ------------------------------------------------------------------

  sealed trait Multiplicity

  object Multiplicity {
    case object One extends Multiplicity
    case object Optional extends Multiplicity
    final case class Many(
      minOccurs: Int = 0,
      maxOccurs: Option[Int] = None
    ) extends Multiplicity
  }

  // ------------------------------------------------------------------
  // Size constraint
  // ------------------------------------------------------------------

  final case class SizeConstraint(
    minLength: Int = 0,
    maxLength: Option[Int] = None
  )

  object SizeConstraint {
    val unbounded: SizeConstraint = SizeConstraint()
  }

  // ------------------------------------------------------------------
  // Instance
  // ------------------------------------------------------------------

  final case class Instance(
    core: Core
  ) extends Schema

  def apply(
    columns: Vector[Column]
  ): Schema =
    Instance(
      Core(columns)
    )

  // ------------------------------------------------------------------
  // Optional typed schema helper for runtime layers
  // ------------------------------------------------------------------

  trait Typed[C <: Column] extends Schema {

    /**
     * Strongly typed view of columns for runtime layers (e.g. CNCF).
     */
    def typedColumns: Vector[C]

    /**
     * Schema.Core must be constructed from typedColumns so that
     * the inherited final `columns` method remains valid.
     */
    final override def core: Core =
      Core(columns = typedColumns.asInstanceOf[Vector[Column]])
  }
}
