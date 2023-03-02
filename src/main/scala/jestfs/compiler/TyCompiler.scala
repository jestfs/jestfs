package jestfs.compiler

import jestfs.lang.Type
import jestfs.ty.*
import jestfs.ty.util.{Walker => TyWalker}

object TyCompiler extends TyWalker {
  override def walk(ty: UnknownTy): UnknownTy =
    UnknownTy(ty.msg.map(Type.normalizeName))

  override def walkName(name: NameTy): NameTy =
    NameTy(name.set.map(Type.normalizeName))
}
