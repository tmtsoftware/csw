package csw.params.javadsl;

import csw.params.core.models.Units;

public interface JUnits {
    // SI units
    Units angstrom    = Units.angstrom$.MODULE$;
    Units arcmin      = Units.arcmin$.MODULE$;
    Units arcsec      = Units.arcsec$.MODULE$;
    Units day         = Units.day$.MODULE$;
    Units degree      = Units.degree$.MODULE$;
    Units elvolt      = Units.elvolt$.MODULE$;
    Units gram        = Units.gram$.MODULE$;
    Units hour        = Units.hour$.MODULE$;
    Units hertz       = Units.hertz$.MODULE$;
    Units joule       = Units.joule$.MODULE$;
    Units kelvin      = Units.kelvin$.MODULE$;
    Units kilogram    = Units.kilogram$.MODULE$;
    Units kilometer   = Units.kilometer$.MODULE$;
    Units liter       = Units.liter$.MODULE$;
    Units meter       = Units.meter$.MODULE$;
    Units marcsec     = Units.marcsec$.MODULE$;
    Units millimeter  = Units.millimeter$.MODULE$;
    Units millisecond = Units.millisecond$.MODULE$;
    Units micron      = Units.micron$.MODULE$;
    Units micrometer  = Units.micrometer$.MODULE$;
    Units minute      = Units.minute$.MODULE$;
    Units newton      = Units.newton$.MODULE$;
    Units pascal      = Units.pascal$.MODULE$;
    Units radian      = Units.radian$.MODULE$;
    Units second      = Units.second$.MODULE$;
    Units sday        = Units.sday$.MODULE$;
    Units steradian   = Units.steradian$.MODULE$;
    Units microarcsec = Units.microarcsec$.MODULE$;
    Units volt        = Units.volt$.MODULE$;
    Units watt        = Units.watt$.MODULE$;
    Units week        = Units.week$.MODULE$;
    Units year        = Units.year$.MODULE$;

    // CGS units
    Units coulomb    = Units.coulomb$.MODULE$;
    Units centimeter = Units.centimeter$.MODULE$;
    Units erg        = Units.erg$.MODULE$;

    // Astrophsyics units
    Units au        = Units.au$.MODULE$;
    Units jansky    = Units.jansky$.MODULE$;
    Units lightyear = Units.lightyear$.MODULE$;
    Units mag       = Units.mag$.MODULE$;

    // Imperial units
    Units cal   = Units.cal$.MODULE$;
    Units foot  = Units.foot$.MODULE$;
    Units inch  = Units.inch$.MODULE$;
    Units pound = Units.pound$.MODULE$;
    Units mile  = Units.mile$.MODULE$;
    Units ounce = Units.ounce$.MODULE$;
    Units yard  = Units.yard$.MODULE$;

    // Others - engineering
    Units NoUnits = Units.NoUnits$.MODULE$;
    Units encoder = Units.encoder$.MODULE$;
    Units count   = Units.count$.MODULE$;
    Units pix     = Units.pix$.MODULE$;

    // Datetime units
    Units tai = Units.tai$.MODULE$;
    Units utc = Units.utc$.MODULE$;

}
