package example.tutorial.basic.shared;

import csw.params.commands.CommandName;
import csw.params.core.generics.Key;
import csw.params.core.generics.Parameter;
import csw.params.javadsl.JKeyType;
import csw.params.javadsl.JUnits;
import csw.prefix.models.Prefix;

@SuppressWarnings("unused")
public class JSampleInfo {
  public static final Prefix testPrefix = Prefix.apply("ESW.test");

  // Sleep periods in milliseconds for short, medium, and long commands
  public static final Long shortSleepPeriod = 600L;
  public static final Long mediumSleepPeriod = 2000L;
  public static final Long longSleepPeriod = 4000L;

  // AssemblyCommands
  public static final CommandName sleep = new CommandName("sleep");
  public static final CommandName immediateCommand = new CommandName("immediateCommand");
  public static final CommandName shortCommand = new CommandName("shortCommand");
  public static final CommandName mediumCommand = new CommandName("mediumCommand");
  public static final CommandName longCommand = new CommandName("longCommand");
  public static final CommandName complexCommand = new CommandName("complexCommand");
  public static final CommandName badCommand = new CommandName("badCommand");

  // Command parameters and helpers
  public static final Long maxSleep = 5000L;
  public static final Key<Long> sleepTimeKey = JKeyType.LongKey().make("SleepTime");

  // Helper to get units set
  public static Parameter<Long> setSleepTime(Long milli) {
    return sleepTimeKey.set(milli).withUnits(JUnits.millisecond);
  }

  public static final Key<Long> resultKey = JKeyType.LongKey().make("result");

  // HCD Commands
  public static final CommandName hcdSleep = new CommandName("hcdSleep");
  public static final CommandName hcdImmediate = new CommandName("hcdImmediate");
}
