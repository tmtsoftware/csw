package csw.services.location.models;

/**
 * Java API for location service component types.
 */
@SuppressWarnings("unused")
public class JComponentType {

    /**
     * A container for components (assemblies and HCDs)
     */
    public static final ComponentType Container = ComponentType.Container$.MODULE$;

    /**
     * A component that controls a hardware device
     */
    public static final ComponentType HCD = ComponentType.HCD$.MODULE$;

    /**
     * A component that controls one or more HCDs or assemblies
     */
    public static final ComponentType Assembly = ComponentType.Assembly$.MODULE$;

    /**
     * A general purpose service component (actor and/or web service application)
     */
    public static final ComponentType Service = ComponentType.Service$.MODULE$;
}
