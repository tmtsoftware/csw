package csw.params.core.models;

import org.junit.Assert;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;
import csw.params.core.models.Coords.*;

import java.util.*;

import static org.junit.Assert.*;

import static csw.params.core.models.JCoords.*;
import static csw.params.core.models.Coords.*;
import static csw.params.core.models.JAngle.*;

public class JCoordsTest {
  private double delta = 0.00000001;
  private Prefix src = new Prefix("esw.ocs.seq");

  // Basic Eq Coordinate Tests

  private long raToUas(Long h, long m, double s) {
    return h * 15L * 60L * 60L * 1000L * 1000L + m * 15L * 60L * 1000L * 1000L + (long) (s * 1000) * 15L * 1000L;

  }

  private long decToUas(Long d, long m, double s) {
    return d * 60L * 60L * 1000L * 1000L + m * 60L * 1000L * 1000L + (long) (s * 1000) * 1000L;
  }

  @Test
  public void shouldAllowCreatingWithStringsCheckRaDec() {
    // One hard test
    EqCoord c1 = new EqCoord("12:32:01.689", "+44:01:05.12",
        DEFAULT_FRAME(), DEFAULT_TAG(), DEFAULT_CATNAME(), DEFAULT_PMX(), DEFAULT_PMY()); // Note special multiply to accommodate fraction
    assertEquals(c1.ra().uas(), raToUas(12L, 32L, 1.689));
    assertEquals(c1.dec().uas(), decToUas(44L, 1L, 5.12));
  }

  @Test
  public void shouldAllowCreatingWithDegreesCheckRaDec() {
    // Degrees
    EqCoord c1 = JEqCoord.make(degree(185.0), degree(32.0));
    assertEquals(new Angle(185 * Angle.D2Uas()), c1.ra());
    assertEquals(new Angle(32 * Angle.D2Uas()), c1.dec());

    // HMS/Deg, check all values here
    EqCoord c2 = new EqCoord(arcHour(18), degree(-35), ICRS(), OIWFS1(), "NGC1234", 0.0, 2.0);
    assertEquals(c2.ra().toDegree(), 18 * Angle.H2D(), delta);
    assertEquals(c2.dec().toDegree(), -35, delta);
    assertEquals(c2.tag(), OIWFS1());
    assertEquals(c2.pm(), new ProperMotion(0.0, 2.0));
    assertEquals(c2.catalogName(), "NGC1234");

    // Strings
    EqCoord c3 = JEqCoord.make("12:13:14.15", "-30:31:32.3");
    assertEquals(c3.ra().uas(), (12 * Angle.H2Uas() + 13 * Angle.HMin2Uas() + 14.15 * Angle.HSec2Uas()), delta);
    assertEquals(c3.dec().uas(), -1 * (30 * Angle.D2Uas() + 31 * Angle.M2Uas() + 32.3 * Angle.S2Uas()), delta);

    // Both as String
    EqCoord c4 = JEqCoord.asBoth("10:12:45.3-45:17:50", FK5(), DEFAULT_TAG(), DEFAULT_CATNAME(), DEFAULT_PMX(), DEFAULT_PMY());
    assertEquals(c4.ra().uas(), (10 * Angle.H2Uas() + 12 * Angle.HMin2Uas() + 45.3 * Angle.HSec2Uas()), delta);
    assertEquals(c4.dec().uas(), -1 * (45 * Angle.D2Uas() + 17 * Angle.M2Uas() + 50 * Angle.S2Uas()));
  }

  @Test
  public void checkDefaults() {
    EqCoord c1 = JEqCoord.make(arcHour(18), degree(-1));
    assertEquals(c1.ra(), new Angle(18 * Angle.H2Uas()));
    assertEquals(c1.dec(), new Angle(-1 * Angle.D2Uas()));
    assertEquals(c1.catalogName(), "none");
    assertEquals(c1.tag(), BASE());
    assertEquals(c1.frame(), ICRS());
    assertEquals(c1.pm(), ProperMotion.DEFAULT_PROPERMOTION());
  }

}
