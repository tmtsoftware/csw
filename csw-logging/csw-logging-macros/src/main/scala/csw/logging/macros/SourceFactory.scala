/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.logging.macros

trait SourceFactory {
  def get(): SourceLocation
}

object SourceFactory {
  inline implicit def factory: SourceFactory = getSourceLocation

  def from(f: () => SourceLocation): SourceFactory = () => f()

  def from(cls: Class[_]): SourceFactory = from(() => SourceLocation("", "", cls.getName, -1))

  private inline def getSourceLocation: SourceFactory =
    () => SourceLocation(sourcecode.FileName(), sourcecode.Pkg(), sourcecode.Enclosing(), sourcecode.Line())
}
