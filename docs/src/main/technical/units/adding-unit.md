# Adding a new unit

If existing @ref:[Units](../../params/units.md) does not meet your requirements, you can add a new Unit with a unit name and description in [Units.scala]($github.base_url$/csw-params/shared/src/main/scala/csw/params/core/models/Units.scala) and corresponding entry for Java Code consumption in [JUnits.java]($github.base_url$/csw-params/jvm/src/main/java/csw/params/javadsl/JUnits.java)

For frontend consumption, new Unit needs to be added in [Units.ts](https://github.com/tmtsoftware/esw-ts/tree/master/src/models/params/Units.ts) file in `esw-ts` library.
