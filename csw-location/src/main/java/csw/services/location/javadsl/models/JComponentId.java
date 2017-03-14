package csw.services.location.javadsl.models;

import csw.services.location.scaladsl.models.ComponentId;
import csw.services.location.scaladsl.models.ComponentType;

/**
 * Java API to the location service ComponentId class, which is Used to identify a component.
 */
@SuppressWarnings("unused")
public class JComponentId {
    public static ComponentId componentId(String name, ComponentType componentType) {
        return new ComponentId(name, componentType);
    }
}
