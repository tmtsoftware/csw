package csw.location.api.javadsl;


import csw.location.models.ComponentType;

/**
 * Helper class for Java to get the handle of `ComponentType` which is fundamental to LocationService library
 */
public interface JComponentType {
    /**
     * A container for components (assemblies and HCDs)
     */
    ComponentType Container = ComponentType.Container$.MODULE$;

    /**
     * A component that controls a hardware device
     */
    ComponentType HCD = ComponentType.HCD$.MODULE$;

    /**
     * A component that controls one or more HCDs or assemblies
     */
    ComponentType Assembly = ComponentType.Assembly$.MODULE$;

    /**
     * A component that controls one or more sequencers or assemblies
     */
    ComponentType Sequencer = ComponentType.Sequencer$.MODULE$;

    /**
     * A sequence component e.g ocs_1, iris_1
     */
    ComponentType SequenceComponent = ComponentType.SequenceComponent$.MODULE$;

    /**
     * A general purpose service component (actor and/or web service application)
     */
    ComponentType Service = ComponentType.Service$.MODULE$;
}
