package csw.params.core.generics;

import csw.params.core.models.Choice;
import csw.params.core.models.Choices;
import csw.params.javadsl.JKeyType;
import org.junit.Assert;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;

import java.util.Arrays;
import java.util.List;

import static csw.params.javadsl.JUnits.kilometer;

// DEOPSCSW-183: Configure attributes and values
// DEOPSCSW-190: Implement Unit Support
public class JChoiceKeyTypeTest extends JUnitSuite {
    private final String keyName = " choiceKey";
    private final Choices choices = Choices.from("A", "B", "C");

    private final GChoiceKey choiceKey = JKeyType.ChoiceKey().make(keyName, kilometer, choices);

    @Test
    public void choicesAPIShouldBeAccessible__DEOPSCSW_183_DEOPSCSW_190() {
        Assert.assertTrue(choices.contains(new Choice("B")));
        List<Choice> expectedChoiceList = Arrays.asList(new Choice("A"), new Choice("B"), new Choice("C"));
        Assert.assertEquals(expectedChoiceList, choices.jValues());
    }

    @Test
    public void choiceKeyShouldHaveNameTypeAndChoices__DEOPSCSW_183_DEOPSCSW_190() {
        Assert.assertEquals(choices, choiceKey.choices());
        Assert.assertEquals(keyName, choiceKey.keyName());
        Assert.assertEquals(choices, choiceKey.choices());
    }

    @Test
    public void shouldAbleToCreateChoiceParameterWithoutUnits__DEOPSCSW_183_DEOPSCSW_190() {
        Choice choice1 = new Choice("A");
        Choice choice2 = new Choice("B");
        Choice[] choicesArr = {choice1, choice2};

        // set with varargs
        Parameter<Choice> choiceParameter = choiceKey.set(choice1, choice2);
        Assert.assertEquals(kilometer, choiceParameter.units());
        Assert.assertEquals(choice1, choiceParameter.jGet(0).orElseThrow());
        Assert.assertEquals(choice2, choiceParameter.jGet(1).orElseThrow());
        Assert.assertEquals(choice1, choiceParameter.head());
        Assert.assertEquals(choice1, choiceParameter.value(0));
        Assert.assertEquals(choice2, choiceParameter.value(1));
        Assert.assertEquals(2, choiceParameter.size());
        Assert.assertArrayEquals(choicesArr, (Choice[]) choiceParameter.values());
        Assert.assertEquals(Arrays.asList(choicesArr), choiceParameter.jValues());
    }

    @Test
    public void shouldAbleToCreateChoiceParameterWithUnits__DEOPSCSW_183_DEOPSCSW_190() {   ///  REVISIT: remove?
        Choice choice1 = new Choice("A");
        Choice choice2 = new Choice("B");
        Choice[] choicesArr = {choice1, choice2};

        // set with Array and Units
        Parameter<Choice> choiceParameter = choiceKey.setAll(choicesArr);
        Assert.assertEquals(kilometer, choiceParameter.units());
        Assert.assertEquals(choice1, choiceParameter.jGet(0).orElseThrow());
        Assert.assertEquals(choice2, choiceParameter.jGet(1).orElseThrow());
        Assert.assertEquals(choice1, choiceParameter.head());
        Assert.assertEquals(choice1, choiceParameter.value(0));
        Assert.assertEquals(choice2, choiceParameter.value(1));
        Assert.assertEquals(2, choiceParameter.size());
        Assert.assertArrayEquals(choicesArr, (Choice[]) choiceParameter.values());
        Assert.assertEquals(Arrays.asList(choicesArr), choiceParameter.jValues());
    }

    @Test
    public void shouldThrowExceptionForInvalidChoice__DEOPSCSW_183_DEOPSCSW_190_CSW_153() {
        Choice invalidChoice = new Choice("D");
        Assert.assertThrows(AssertionError.class, () -> choiceKey.set(invalidChoice));
    }

}
