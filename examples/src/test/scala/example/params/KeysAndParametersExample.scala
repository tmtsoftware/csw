/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package example.params

import csw.params.core.generics.KeyType.{ChoiceKey, CoordKey}
import csw.params.core.generics.{GChoiceKey, Key, KeyType, Parameter}
import csw.params.core.models.Coords.EqFrame.FK5
import csw.params.core.models.Coords.SolarSystemObject.Venus
import csw.params.core.models.Units.NoUnits
import csw.params.core.models.*
import csw.time.core.models.{TAITime, UTCTime}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class KeysAndParametersExample extends AnyFunSpec with Matchers {
  // DEOPSCSW-196: Command Payloads for variable command content
  describe("Examples of keys and parameters") {

    it("should show usage of primitive types") {

      // #primitives
      // declare keyName
      val s1: String = "encoder"

      // making 3 keys
      val k1: Key[Boolean] = KeyType.BooleanKey.make(s1)
      val k2: Key[Short]   = KeyType.ShortKey.make("RandomKeyName")
      val k3: Key[String]  = KeyType.StringKey.make(s1)

      // storing a single value
      val booleanParam: Parameter[Boolean] = k1.set(true)

      // storing multiple values
      val paramWithShorts1: Parameter[Short] = k2.set(1, 2, 3, 4)
      val paramWithShorts3: Parameter[Short] = k2.setAll(Array[Short](1, 2, 3, 4))

      // associating units
      val weekDays: Array[String]            = Array("Sunday", "Monday", "Tuesday")
      val paramWithUnits1: Parameter[String] = k3.setAll(weekDays)
      val paramWithUnits2: Parameter[String] = k3.setAll(weekDays) `withUnits` Units.day

      // default unit is NoUnits
      assert(booleanParam.units === Units.NoUnits)

      // set units explicitly on an existing Parameter
      val paramWithUnits3: Parameter[Short] = paramWithShorts1.withUnits(Units.meter)

      // retrieve values from Parameter
      val allValues: Array[Short] = paramWithShorts1.values

      // retrieve just top value
      val head: Short = paramWithShorts1.head
      // #primitives

      // validations
      assert(head === 1)
      assert(allValues === Array(1, 2, 3, 4))
      assert(paramWithShorts1.values === paramWithShorts3.values)
      assert(paramWithUnits3.units === Units.meter)
      assert(paramWithUnits1.values === paramWithUnits2.values)
    }

    it("should show usage of arrays") {
      // #arrays
      // make some arrays
      val arr1: Array[Double] = Array(1.0, 2.0, 3.0, 4.0, 5.0)
      val arr2: Array[Double] = Array(10.0, 20.0, 30.0, 40.0, 50.0)

      // keys
      val filterKey: Key[ArrayData[Double]] = KeyType.DoubleArrayKey.make("filter")

      // Store some values using helper class ArrayData
      val p1: Parameter[ArrayData[Double]] = filterKey.set(ArrayData(arr1), ArrayData(arr2))
      val p2: Parameter[ArrayData[Double]] = filterKey -> ArrayData(arr1 ++ arr2) `withUnits` Units.liter

      // add units to existing parameters
      val p1AsCount = p1.withUnits(Units.count)

      // default unit is NoUnits
      assert(p1.units === Units.NoUnits)

      // retrieving values
      val head: Array[Double]                 = p1.head.data.toArray
      val allValues: Array[ArrayData[Double]] = p1.values
      // #arrays

      // validations
      assert(head === arr1)
      assert(allValues === Array(ArrayData(arr1), ArrayData(arr2)))
      assert(p2.values === Array(ArrayData.fromArray(arr1 ++ arr2)))
      assert(p1AsCount.units === Units.count)
    }

    it("should show usage of matrices") {
      // #matrices
      // make some arrays
      val m1: Array[Array[Byte]] = Array(Array[Byte](1, 2, 3), Array[Byte](4, 5, 6), Array[Byte](7, 8, 9))
      val m2: Array[Array[Byte]] = Array(Array[Byte](1, 2, 3, 4, 5), Array[Byte](10, 20, 30, 40, 50))

      // keys
      val encoderKey: Key[MatrixData[Byte]] = KeyType.ByteMatrixKey.make("encoder")

      // Store some values using helper class MatrixData
      val p1: Parameter[MatrixData[Byte]] = encoderKey.set(MatrixData.fromArrays(m1))
      val p2: Parameter[MatrixData[Byte]] = encoderKey.set(m1, m2) `withUnits` Units.liter

      // add units to existing parameters
      val p1AsLiter = p1.withUnits(Units.liter)

      // default unit is NoUnits
      assert(p1.units === Units.NoUnits)

      // retrieving values
      val head: Array[Array[Byte]]           = p1.head.data.map(_.toArray).toArray
      val allValues: Array[MatrixData[Byte]] = p1.values
      // #matrices

      // validations
      assert(head === m1)
      assert(allValues === Array(MatrixData.fromArrays(m1)))
      assert(p2.values === Array(MatrixData.fromArrays(m1), MatrixData.fromArrays(m2)))
      assert(p1AsLiter.units === Units.liter)
    }

    it("should show usage of choice") {
      // #choice
      // Choice
      val choices = Choices.from("A", "B", "C")

      // keys
      val choice1Key: GChoiceKey = ChoiceKey.make("mode", NoUnits, choices)
      val choice2Key: GChoiceKey = ChoiceKey.make(
        "mode-reset",
        NoUnits,
        Choices.fromChoices(Choice("c"), Choice("b"), Choice("a"))
      )

      // store values
      val p1: Parameter[Choice] = choice1Key
        .setAll(Array(Choice("A")))
        .withUnits(Units.foot)
      val p2: Parameter[Choice] = choice2Key.setAll(Array(Choice("c")))

      // add units
      val paramWithFoot = p1.withUnits(Units.foot)

      // default unit is NoUnits
      assert(p2.units === Units.NoUnits)

      // retrieving values
      val head: Choice          = p1.head
      val values: Array[Choice] = p2.values

      // #choice
      // validations
      head should be(Choice("A"))
      values should be(Array(Choice("c")))
      paramWithFoot.units should be(Units.foot)
    }

    it("should show usage of coordinate types") {
      // #coords
      import Angle._
      import Coords._

      // Coordinate types
      val pm               = ProperMotion(0.5, 2.33)
      val eqCoord          = EqCoord(ra = "12:13:14.15", dec = "-30:31:32.3", frame = FK5, pmx = pm.pmx, pmy = pm.pmy)
      val solarSystemCoord = SolarSystemCoord(Tag("BASE"), Venus)
      val minorPlanetCoord = MinorPlanetCoord(Tag("GUIDER1"), 2000, 90.degree, 2.degree, 100.degree, 1.4, 0.234, 220.degree)
      val cometCoord       = CometCoord(Tag("BASE"), 2000.0, 90.degree, 2.degree, 100.degree, 1.4, 0.234)
      val altAzCoord       = AltAzCoord(Tag("BASE"), 301.degree, 42.5.degree)

      // Can use base trait CoordKey to store values for all types
      val basePosKey = CoordKey.make("BasePosition")
      val posParam   = basePosKey.set(eqCoord, solarSystemCoord, minorPlanetCoord, cometCoord, altAzCoord)

      // retrieving values
      assert(posParam.values.length == 5)
      assert(posParam.values(0) == eqCoord)
      assert(posParam.values(1) == solarSystemCoord)
      assert(posParam.values(2) == minorPlanetCoord)
      assert(posParam.values(3) == cometCoord)
      assert(posParam.values(4) == altAzCoord)
      // #coords
    }

    it("should show usage of units | CSW-152") {

      // #units
      // declare keyname
      val s1: String = "encoder"

      // making 3 keys
      val k1: Key[Boolean] = KeyType.BooleanKey.make(s1)
      val k2: Key[Short]   = KeyType.ShortKey.make("RandomKeyName")
      val k3: Key[String]  = KeyType.StringKey.make(s1)

      // storing a single value, default unit is NoUnits
      val bParam: Parameter[Boolean] = k1.set(true)
      val bDefaultUnitSet: Boolean   = bParam.units === Units.NoUnits // true

      // default unit for UTCTimeKey
      val utcParam: Parameter[UTCTime] = KeyType.UTCTimeKey.make("now").set(UTCTime.now())
      // default unit for TAITimeKey
      val taiParam: Parameter[TAITime] = KeyType.TAITimeKey.make("now").set(TAITime.now())

      // storing multiple values
      val paramOfShorts: Parameter[Short] = k2.set(1, 2, 3, 4)

      // values to store
      val weekDays: Array[String] = Array("Sunday", "Monday", "Tuesday")

      // default units via set
      val paramWithUnits1: Parameter[String] = k3.setAll(weekDays)
      // associating units via withUnits
      val paramWithUnits2: Parameter[String] = k3.setAll(weekDays) `withUnits` Units.day
      // change existing unit
      val paramWithUnits3: Parameter[Short] = paramOfShorts.withUnits(Units.meter)
      // #units

      // validations
      assert(bDefaultUnitSet === true)
      assert(utcParam.units === Units.utc)
      assert(taiParam.units === Units.tai)
      assert(paramWithUnits1.units === Units.NoUnits)
      assert(paramWithUnits2.units === Units.day)
      assert(paramWithUnits3.units === Units.meter)
    }
  }
}
