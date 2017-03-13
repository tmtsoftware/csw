package csw.services.location.models;


import csw.services.location.common.ActorRuntime;

import java.util.Map;

public interface IActorRuntime {

    public ActorRuntime create(String name, Map<String, Object> settings);
    public ActorRuntime create(String name);

}

