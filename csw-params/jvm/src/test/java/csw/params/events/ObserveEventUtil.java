package csw.params.events;

import csw.params.core.models.Choice;

import java.util.HashSet;

public class ObserveEventUtil {
    public static HashSet<Choice> getOperationalStateChoices() {
        HashSet<Choice> operationalStateChoices = new HashSet<>();
        operationalStateChoices.add(new Choice(JOperationalState.BUSY().entryName()));
        operationalStateChoices.add(new Choice(JOperationalState.READY().entryName()));
        operationalStateChoices.add(new Choice(JOperationalState.ERROR().entryName()));
        operationalStateChoices.add(new Choice(JOperationalState.NOT_READY().entryName()));
        return operationalStateChoices;
    }

    public static HashSet<Choice> getCoordinateSystemChoices() {
        HashSet<Choice> coordinateSystemChoices = new HashSet<>();
        coordinateSystemChoices.add(new Choice(JCoordinateSystem.RADEC().entryName()));
        coordinateSystemChoices.add(new Choice(JCoordinateSystem.XY().entryName()));
        coordinateSystemChoices.add(new Choice(JCoordinateSystem.ALTAZ().entryName()));
        return coordinateSystemChoices;
    }
}
