package csw.params.events;

import csw.params.core.generics.Parameter;
import csw.params.javadsl.JKeyType;
import org.junit.Assert;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;

import java.util.HashSet;
import java.util.Set;

public class JWFSDetectorEventTest extends JUnitSuite {
  String sourcePrefix = "ESW.filter.wheel";
  String detector = "wfs-detector";

  @Test
  public void shouldCreatePublishSuccessEvent__CSW_118() {
    ObserveEvent event = WFSDetectorEvent.publishSuccess(sourcePrefix);

    Assert.assertEquals("ESW.ObserveEvent", event.source().toString());
    Assert.assertEquals("PublishSuccess", event.eventName().name());
  }

  @Test
  public void shouldCreatePublishFailEvent__CSW_118() {
    ObserveEvent event = WFSDetectorEvent.publishFail(sourcePrefix);

    Assert.assertEquals("ESW.ObserveEvent", event.source().toString());
    Assert.assertEquals("PublishFail", event.eventName().name());
  }

  @Test
  public void shouldCreateIrDetectorExposureStateEvent__CSW_118_CSW_119() {
    Set<Parameter<?>> paramSet = getParamSetForExposureStateEvent();

    ObserveEvent event = WFSDetectorEvent.exposureState(
      sourcePrefix,
      detector,
      true,
      false,
      true,
      JOperationalState.READY(),
      ""
    );

    Assert.assertEquals(paramSet, event.jParamSet());
    Assert.assertEquals("wfsDetectorExposureState", event.eventName().name());
    Assert.assertEquals("ESW.ObserveEvent", event.source().toString());
  }

  private Set<Parameter<?>> getParamSetForExposureStateEvent() {
    Parameter<String> detectorParam = JKeyType.StringKey().make("detector").set(detector);
    Parameter<Boolean> exposureInProgress = JKeyType.BooleanKey().make("exposureInProgress").set(true);
    Parameter<Boolean> abortInProgress = JKeyType.BooleanKey().make("abortInProgress").set(false);
    Parameter<Boolean> isAborted = JKeyType.BooleanKey().make("isAborted").set(true);
    Parameter<String> errorMessage = JKeyType.StringKey().make("errorMessage").set("");
    Parameter<String> operationalState = JKeyType.StringKey().make("operationalState").set("READY");
    Parameter<String> sourcePrefixParam = JKeyType.StringKey().make("sourcePrefix").set(sourcePrefix);

    Set<Parameter<?>> paramSet = new HashSet<>(10);
    paramSet.add(exposureInProgress);
    paramSet.add(detectorParam);
    paramSet.add(sourcePrefixParam);
    paramSet.add(abortInProgress);
    paramSet.add(isAborted);
    paramSet.add(errorMessage);
    paramSet.add(operationalState);
    return paramSet;
  }
}

