package csw.params.events;

import csw.params.core.models.Choice;

import java.util.HashSet;

public class ObserveEventUtil {
    public static HashSet<Choice> getOperationalStateChoices() {
        HashSet<Choice> operationalStateChoices = new HashSet<>();
        operationalStateChoices.add(new Choice(JOperationalState.BUSY().entryName()));
        operationalStateChoices.add(new Choice(JOperationalState.READY().entryName()));
        operationalStateChoices.add(new Choice(JOperationalState.ERROR().entryName()));
        return operationalStateChoices;
    }
}
