package org.goldenport.model.value

import org.goldenport.datatype.ObjectId

/*
 * @since   Aug.  1, 2025
 * @version Aug.  2, 2025
 * @author  ASAMI, Tomoharu
 */
case class SecurityAttributes(
  ownerId: ObjectId,
  groupId: ObjectId,
  rights: SecurityAttributes.Rights,
  privilegeId: ObjectId
)

object SecurityAttributes {
  case class Rights(
    owner: Rights.Permissions,
    group: Rights.Permissions,
    other: Rights.Permissions
  )
  object Rights {
    case class Permissions(
      read: Boolean,
      write: Boolean,
      execute: Boolean
    )
  }

  trait Holder {
    protected def security_Attributes: SecurityAttributes
  }
}
