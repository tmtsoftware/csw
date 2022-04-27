/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.params.javadsl;

import csw.params.core.models.Units;

public interface JUnits {
    // SI units
    Units angstrom    = Units.angstrom$.MODULE$;
    Units alpha       = Units.alpha$.MODULE$;
    Units ampere      = Units.ampere$.MODULE$;
    Units arcmin      = Units.arcmin$.MODULE$;
    Units arcsec      = Units.arcsec$.MODULE$;
    Units bar         = Units.bar$.MODULE$;
    Units candela     = Units.candela$.MODULE$;
    Units day         = Units.day$.MODULE$;
    Units degree      = Units.degree$.MODULE$;
    Units degC        = Units.degC$.MODULE$;
    Units degF        = Units.degF$.MODULE$;
    Units elvolt      = Units.elvolt$.MODULE$;
    Units gauss       = Units.gauss$.MODULE$;
    Units gram        = Units.gram$.MODULE$;
    Units henry       = Units.hertz$.MODULE$;
    Units hertz       = Units.hertz$.MODULE$;
    Units hour        = Units.hour$.MODULE$;
    Units joule       = Units.joule$.MODULE$;
    Units kelvin      = Units.kelvin$.MODULE$;
    Units kilogram    = Units.kilogram$.MODULE$;
    Units kilometer   = Units.kilometer$.MODULE$;
    Units liter       = Units.liter$.MODULE$;
    Units lm          = Units.lm$.MODULE$;
    Units lsun        = Units.lsun$.MODULE$;
    Units lx          = Units.lx$.MODULE$;
    Units mas         = Units.mas$.MODULE$;
    Units me          = Units.me$.MODULE$;
    Units meter       = Units.meter$.MODULE$;
    Units microarcsec = Units.microarcsec$.MODULE$;
    Units millimeter  = Units.millimeter$.MODULE$;
    Units millisecond = Units.millisecond$.MODULE$;
    Units micron      = Units.micron$.MODULE$;
    Units micrometer  = Units.micrometer$.MODULE$;
    Units minute      = Units.minute$.MODULE$;
    Units MJD         = Units.MJD$.MODULE$;
    Units mol         = Units.mol$.MODULE$;
    Units month       = Units.month$.MODULE$;
    Units mmyy        = Units.mmyy$.MODULE$;
    Units mu0         = Units.mu0$.MODULE$;
    Units muB         = Units.muB$.MODULE$;
    Units nanometer   = Units.nanometer$.MODULE$;
    Units newton      = Units.newton$.MODULE$;
    Units ohm         = Units.ohm$.MODULE$;
    Units pascal      = Units.pascal$.MODULE$;
    Units pi          = Units.pi$.MODULE$;
    Units pc          = Units.pc$.MODULE$;
    Units ppm         = Units.ppm$.MODULE$;
    Units radian      = Units.radian$.MODULE$;
    Units second      = Units.second$.MODULE$;
    Units sday        = Units.sday$.MODULE$;
    Units steradian   = Units.steradian$.MODULE$;
    Units volt        = Units.volt$.MODULE$;
    Units watt        = Units.watt$.MODULE$;
    Units Wb          = Units.Wb$.MODULE$;
    Units week        = Units.week$.MODULE$;
    Units year        = Units.year$.MODULE$;

    // CGS units
    Units coulomb    = Units.coulomb$.MODULE$;
    Units centimeter = Units.centimeter$.MODULE$;
    Units D          = Units.D$.MODULE$;
    Units dyn        = Units.dyn$.MODULE$;
    Units erg        = Units.erg$.MODULE$;

    // Astrophysics units
    Units au        = Units.au$.MODULE$;
    Units a0        = Units.a0$.MODULE$;
    Units c         = Units.c$.MODULE$;
    Units cKayser   = Units.cKayser$.MODULE$;
    Units crab      = Units.crab$.MODULE$;
    Units damas     = Units.damas$.MODULE$;
    Units e         = Units.e$.MODULE$;
    Units earth     = Units.earth$.MODULE$;
    Units F         = Units.F$.MODULE$;
    Units G         = Units.G$.MODULE$;
    Units geomass   = Units.geoMass$.MODULE$;
    Units hm        = Units.hm$.MODULE$;
    Units hms       = Units.hms$.MODULE$;
    Units hhmmss    = Units.hhmmss$.MODULE$;
    Units jansky    = Units.jansky$.MODULE$;
    Units jd        = Units.jd$.MODULE$;
    Units jovmass   = Units.jovmass$.MODULE$;
    Units lightyear = Units.lightyear$.MODULE$;
    Units mag       = Units.mag$.MODULE$;
    Units mjup      = Units.mjup$.MODULE$;
    Units mp        = Units.mp$.MODULE$;
    Units minsec    = Units.minsec$.MODULE$;
    Units msun      = Units.msun$.MODULE$;
    Units photon    = Units.photon$.MODULE$;
    Units rgeo      = Units.rgeo$.MODULE$;
    Units rgup      = Units.rjup$.MODULE$;
    Units rsun      = Units.rsun$.MODULE$;
    Units rydberg   = Units.rydberg$.MODULE$;
    Units seimens   = Units.seimens$.MODULE$;
    Units tesla     = Units.tesla$.MODULE$;
    Units u         = Units.u$.MODULE$;

    // Imperial units
    Units barn  = Units.barn$.MODULE$;
    Units cal   = Units.cal$.MODULE$;
    Units foot  = Units.foot$.MODULE$;
    Units inch  = Units.inch$.MODULE$;
    Units pound = Units.pound$.MODULE$;
    Units mile  = Units.mile$.MODULE$;
    Units ounce = Units.ounce$.MODULE$;
    Units yard  = Units.yard$.MODULE$;

    // Others - engineering
    Units NoUnits = Units.NoUnits$.MODULE$;
    Units bit     = Units.bit$.MODULE$;
    Units encoder = Units.encoder$.MODULE$;
    Units count   = Units.count$.MODULE$;
    Units mmhg = Units.mmhg$.MODULE$;
    Units percent= Units.percent$.MODULE$;
    Units pix     = Units.pix$.MODULE$;

    // Datetime units
    Units tai = Units.tai$.MODULE$;
    Units utc = Units.utc$.MODULE$;
    Units date = Units.date$.MODULE$;
    Units datetime= Units.datetime$.MODULE$;
}
