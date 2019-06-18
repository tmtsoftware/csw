package csw.params.core.models;

import csw.params.commands.CommandName;
import csw.params.commands.Setup;
import csw.params.core.formats.JavaJsonSupport;
import org.junit.Test;
import csw.params.core.models.Coords.*;
import play.api.libs.json.Json;

import java.util.*;

import static org.junit.Assert.*;

import static csw.params.javadsl.JKeyType.*;
import static csw.params.core.models.JCoords.*;
import static csw.params.core.models.Coords.*;
import static csw.params.core.models.JAngle.*;

@SuppressWarnings({"SameParameterValue", "FieldCanBeLocal"})
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
        var c1 = new EqCoord("12:32:01.689", "+44:01:05.12",
            DEFAULT_FRAME(), DEFAULT_TAG(), DEFAULT_CATNAME(), DEFAULT_PMX(), DEFAULT_PMY()); // Note special multiply to accommodate fraction
        assertEquals(c1.ra().uas(), raToUas(12L, 32L, 1.689));
        assertEquals(c1.dec().uas(), decToUas(44L, 1L, 5.12));
    }

    @Test
    public void shouldAllowCreatingWithDegreesCheckRaDec() {
        // Degrees
        var c1 = JEqCoord.make(degree(185.0), degree(32.0));
        assertEquals(new Angle(185 * Angle.D2Uas()), c1.ra());
        assertEquals(new Angle(32 * Angle.D2Uas()), c1.dec());

        // HMS/Deg, check all values here
        var c2 = new EqCoord(arcHour(18), degree(-35), ICRS(), OIWFS1(), "NGC1234", 0.0, 2.0);
        assertEquals(c2.ra().toDegree(), 18 * Angle.H2D(), delta);
        assertEquals(c2.dec().toDegree(), -35, delta);
        assertEquals(c2.tag(), OIWFS1());
        assertEquals(c2.pm(), new ProperMotion(0.0, 2.0));
        assertEquals(c2.catalogName(), "NGC1234");

        // Strings
        var c3 = JEqCoord.make("12:13:14.15", "-30:31:32.3");
        assertEquals(c3.ra().uas(), (12 * Angle.H2Uas() + 13 * Angle.HMin2Uas() + 14.15 * Angle.HSec2Uas()), delta);
        assertEquals(c3.dec().uas(), -1 * (30 * Angle.D2Uas() + 31 * Angle.M2Uas() + 32.3 * Angle.S2Uas()), delta);

        // Both as String
        var c4 = JEqCoord.asBoth("10:12:45.3-45:17:50", FK5(), DEFAULT_TAG(), DEFAULT_CATNAME(), DEFAULT_PMX(), DEFAULT_PMY());
        assertEquals(c4.ra().uas(), (10 * Angle.H2Uas() + 12 * Angle.HMin2Uas() + 45.3 * Angle.HSec2Uas()), delta);
        assertEquals(c4.dec().uas(), -1 * (45 * Angle.D2Uas() + 17 * Angle.M2Uas() + 50 * Angle.S2Uas()));
    }

    @Test
    public void checkDefaults() {
        var c1 = JEqCoord.make(arcHour(18), degree(-1));
        assertEquals(c1.ra(), new Angle(18 * Angle.H2Uas()));
        assertEquals(c1.dec(), new Angle(-1 * Angle.D2Uas()));
        assertEquals(c1.catalogName(), "none");
        assertEquals(c1.tag(), BASE());
        assertEquals(c1.frame(), ICRS());
        assertEquals(c1.pm(), ProperMotion.DEFAULT_PROPERMOTION());
    }

    // JSON tests
    private ProperMotion pm = new ProperMotion(0.5, 2.33);

    @Test
    public void shouldConvertPmToFromJSON() {
        // Check proper motions
        var pmjs = Json.toJson(pm, ProperMotion.pmFormat());

        var pmIn = pmjs.as(ProperMotion.pmFormat());
        assertEquals(pmIn, pm);
    }

    @Test
    public void shouldConvertFrameToFromJSON() {
        var f1 = ICRS();
        var j1 = Json.toJson(f1, Coords.eqfFormat());
        assertEquals(j1.as(Coords.eqfFormat()), ICRS());
    }

    @Test
    public void shouldJSONAnAltAz() {
        var c0 = new AltAzCoord(BASE(), degree(301), degree(42.5));
        var js = Json.toJson(c0, JAltAzCoord.coordFormat());
        var c1 = js.as(JAltAzCoord.coordFormat());
        assertEquals(c0, c1);
    }

    @Test
    public void shouldJSONSolarSystemCoord() {
        var c0 = new SolarSystemCoord(BASE(), Venus());

        var js = Json.toJson(c0, JSolarSystemCoord.coordFormat());
        var c1 = js.as(JSolarSystemCoord.coordFormat());
        assertEquals(c0, c1);
    }

    @Test
    public void shouldJSONMinorPlanetCoord() {
        var c0 = new MinorPlanetCoord(GUIDER1(), 2000.0d, degree(90), degree(2), degree(100), 1.4d, 0.234d, degree(220));

        var js = Json.toJson(c0, JMinorPlanetCoord.coordFormat());
        var c1 = js.as(JMinorPlanetCoord.coordFormat());
        assertEquals(c0, c1);
    }

    @Test
    public void shouldJSONCometCoord() {
        var c0 = new CometCoord(BASE(), 2000.0d, degree(90), degree(2), degree(100), 1.4d, 0.234d);

        var js = Json.toJson(c0, JCometCoord.coordFormat());
        var c1 = js.as(JCometCoord.coordFormat());
        assertEquals(c0, c1);

        var js2 = Json.toJson(c0, JCoord.jsonFormat());
        assertEquals(js2.as(JCoord.jsonFormat()), c0);
    }

    @Test
    public void shouldJSONAnEqCoord() {
        // Check EqCoordinate
        var eq = new EqCoord(180.0, 32.0, FK5(), BASE(), DEFAULT_CATNAME(), pm.pmx(), pm.pmy());

        var js = Json.toJson(eq, JEqCoord.coordFormat());
        var eqIn = js.as(JEqCoord.coordFormat());
        assertEquals(eqIn, eq);
    }

    @Test
    public void shouldJSONEqcoordParameterInSetup() {
        // Check EqCoordinate
        var eq = new EqCoord(180.0, 32.0, FK5(), BASE(), DEFAULT_CATNAME(), pm.pmx(), pm.pmy());

        var baseKey = EqCoordKey().make("BasePosition");
        var posParam = baseKey.set(eq);

        var setup = new Setup(src, new CommandName("test"), Optional.empty()).add(posParam);
        var setupOut = JavaJsonSupport.writeSequenceCommand(setup);
        var setupIn = JavaJsonSupport.readSequenceCommand(setupOut);

        assertEquals(setupIn, setup);
    }

    @Test
    public void shouldJSONAsCoordParameterInSetup() {

        // Check EqCoordinate
        var eq = new EqCoord(180.0, 32.0, FK5(), BASE(), DEFAULT_CATNAME(), pm.pmx(), pm.pmy());

        var baseKey = CoordKey().make("BasePosition");
        var posParam = baseKey.set(eq);

        var setup = new Setup(src, new CommandName("test"), Optional.empty()).add(posParam);

        var setupOut = JavaJsonSupport.writeSequenceCommand(setup);
        var setupIn = JavaJsonSupport.readSequenceCommand(setupOut);

        assertEquals(setupIn, setup);
    }
}
