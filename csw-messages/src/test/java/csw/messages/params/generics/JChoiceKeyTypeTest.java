package csw.messages.params.generics;

import csw.messages.params.models.Choice;
import csw.messages.params.models.Choices;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.List;

import static csw.messages.javadsl.JUnits.NoUnits;
import static csw.messages.javadsl.JUnits.kilometer;

// DEOPSCSW-183: Configure attributes and values
// DEOPSCSW-190: Implement Unit Support
public class JChoiceKeyTypeTest {
    private final String keyName = " choiceKey";
    private final Choices choices = Choices.from("A", "B", "C");

    private final GChoiceKey choiceKey = JKeyType.ChoiceKey().make(keyName, choices);

    @Test
    public void choicesAPIShouldBeAccessible() {
        Assert.assertTrue(choices.contains(new Choice("B")));
        List<Choice> expectedChoiceList = Arrays.asList(new Choice("A"), new Choice("B"), new Choice("C"));
        Assert.assertEquals(expectedChoiceList, choices.jValues());
    }

    @Test
    public void choiceKeyShouldHaveNameTypeAndChoices() {
        Assert.assertEquals(choices, choiceKey.choices());
        Assert.assertEquals(keyName, choiceKey.keyName());
        Assert.assertEquals(choices, choiceKey.choices());
    }

    @Test
    public void shouldAbleToCreateChoiceParameterWithoutUnits() {
        Choice choice1 = new Choice("A");
        Choice choice2 = new Choice("B");
        Choice[] choicesArr = {choice1, choice2};

        // set with varargs
        Parameter<Choice> choiceParameter = choiceKey.set(choice1, choice2);
        Assert.assertEquals(NoUnits, choiceParameter.units());
        Assert.assertEquals(choice1, choiceParameter.jGet(0).get());
        Assert.assertEquals(choice2, choiceParameter.jGet(1).get());
        Assert.assertEquals(choice1, choiceParameter.head());
        Assert.assertEquals(choice1, choiceParameter.value(0));
        Assert.assertEquals(choice2, choiceParameter.value(1));
        Assert.assertEquals(2, choiceParameter.size());
        Assert.assertArrayEquals(choicesArr, (Choice[])choiceParameter.values());
        Assert.assertEquals(Arrays.asList(choicesArr), choiceParameter.jValues());
    }

    @Test
    public void shouldAbleToCreateChoiceParameterWithUnits() {
        Choice choice1 = new Choice("A");
        Choice choice2 = new Choice("B");
        Choice[] choicesArr = {choice1, choice2};

        // set with Array and Units
        Parameter<Choice> choiceParameter = choiceKey.set(choicesArr, kilometer);
        Assert.assertEquals(kilometer, choiceParameter.units());
        Assert.assertEquals(choice1, choiceParameter.jGet(0).get());
        Assert.assertEquals(choice2, choiceParameter.jGet(1).get());
        Assert.assertEquals(choice1, choiceParameter.head());
        Assert.assertEquals(choice1, choiceParameter.value(0));
        Assert.assertEquals(choice2, choiceParameter.value(1));
        Assert.assertEquals(2, choiceParameter.size());
        Assert.assertArrayEquals(choicesArr, (Choice[])choiceParameter.values());
        Assert.assertEquals(Arrays.asList(choicesArr), choiceParameter.jValues());
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldThrowExceptionForInvalidChoice() {
        Choice invalidChoice = new Choice("D");
        exception.expect(AssertionError.class);
        choiceKey.set(invalidChoice);
    }

}
