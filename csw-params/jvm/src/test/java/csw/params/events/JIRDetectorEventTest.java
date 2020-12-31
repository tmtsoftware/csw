package csw.params.events;

import csw.params.core.generics.Key;
import csw.params.core.generics.Parameter;
import csw.params.core.models.ObsId;
import csw.params.javadsl.JKeyType;
import csw.prefix.javadsl.JSubsystem;
import csw.prefix.models.Prefix;
import csw.prefix.models.Subsystem;
import org.junit.Assert;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JIRDetectorEventTest extends JUnitSuite {

  @Test
  public void shouldCreateIrDetectorExposureStateEvent__CSW_118() {
    String sourcePrefix = "ESW.filter.wheel";
    ObsId obsId = new ObsId("someObsId");
    String detector = "ir-detector";
    ObserveEvent event = JIRDetectorEvent.IRDetectorExposureState().create(
      sourcePrefix,
      obsId,
      detector,
      true,
      false,
      true,
      "",
      OperationalState.BUSY()
    );

    Assert.assertEquals(event.source(), new Prefix(JSubsystem.ESW, "filter.wheel"));

    Parameter<String> detectorParam = JKeyType.StringKey().make("detector").set(detector);
    Parameter<Boolean> exposureInProgress = JKeyType.BooleanKey().make("exposureInProgress").set(true);
    Parameter<Boolean> abortInProgress = JKeyType.BooleanKey().make("abortInProgress").set(false);
    Parameter<Boolean> isAborted = JKeyType.BooleanKey().make("isAborted").set(true);
    Parameter<String> errorMessage = JKeyType.StringKey().make("errorMessage").set("");
    Parameter<String> operationalState = JKeyType.StringKey().make("operationalState").set("BUSY");

    Set<Parameter<?>> paramSet = new HashSet<>(10);

    paramSet.add(detectorParam);
    paramSet.add(exposureInProgress);
    paramSet.add(abortInProgress);
    paramSet.add(isAborted);
    paramSet.add(errorMessage);
    paramSet.add(operationalState);

    Assert.assertEquals(paramSet, event.jParamSet());

    Assert.assertEquals("IRDetectorExposureState", event.eventName().name());
    Assert.assertEquals("ESW.filter.wheel", event.source().toString());
  }
}
