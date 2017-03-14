package csw.services.location.common;

import java.util.Collections;
import java.util.Map;

public class JActorRuntime {

    public static ActorRuntime actorRuntime(String name, Map<String, Object> settings) {
        return new ActorRuntime(name, ScalaCompat.toMap(settings));
    }

    public static ActorRuntime actorRuntime(String name) {
        return actorRuntime(name, Collections.emptyMap());
    }

}
