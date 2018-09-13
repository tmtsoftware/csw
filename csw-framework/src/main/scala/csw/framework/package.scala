package csw

/**
 * == Framework ==
 *
 * This project contains the framework for creating components, such as HCDs and Assemblies.
 *
 * A component is implemented by extending the [[csw.framework.scaladsl.ComponentHandlers]] base class.
 * These handlers are executed under an Supervisor Actor: [[csw.framework.internal.supervisor.SupervisorBehavior]]
 * and TLA Actor (Top Level Actor): [[csw.framework.internal.component.ComponentBehavior]]
 * defined in the framework which handles the lifecycle and supervision of this component.
 *
 * Components are controlled by a [[csw.framework.internal.supervisor.SupervisorBehavior]] actor
 * that intercepts common messages (ex. Shutdown, Restart) or lifecycle messages (ex. GoOffline or GoOnline)
 * sent from external entities to determine the state of the component.
 *
 * Components that are not in the [[csw.command.models.framework.SupervisorLifecycleState.Running]] state, do not receive [[csw.messages.CommandMessage]].
 *
 * When Component is created using this framework, it guarantees that component (HCD/Assembly) is registered with
 * LocationService only when Component moves to [[csw.command.models.framework.SupervisorLifecycleState.Running]] state.
 * That means, one component can resolve other component only when its in ``Running`` state and ready to accept [[messages.CommandMessage]]
 *
 * === Important Actors in Framework ===
 *
 * - [[csw.framework.internal.supervisor.SupervisorBehavior]]
 * : Each component created by this framework is supervised by this actor.
 *
 * Default strategy of supervisor is to stop child actor but depending on nature of the failure, the supervisor has a choice of the following two options:
 *  - [[csw.framework.exceptions.FailureRestart]] : Restart the child actor i.e. kill the current child actor that failed and create a new one in its place
 *  - [[csw.framework.exceptions.FailureStop]] : Stop the child actor permanently
 *
 * Supervisor changes its [[csw.command.models.framework.SupervisorLifecycleState]] based on the messages it receives.
 * Decision to handle external messages or not is taken by the supervisor based on its current [[csw.command.models.framework.SupervisorLifecycleState]].
 * For complete list of supported messages per [[csw.command.models.framework.SupervisorLifecycleState]], see `csw-messages` project.
 *
 * - [[csw.framework.internal.component.ComponentBehavior]]
 * : Like Supervisor, evey component is associate with this actor which is known as TLA (Top Level Actor)
 * And it also maintains its own state [[csw.framework.internal.component.ComponentLifecycleState]] based on messages it receives.
 *
 * Main purpose of this actor is to invoke component specific code written in their corresponding handlers.
 * This is where framework code meets Component specific code.
 *
 * - [[csw.framework.internal.pubsub.PubSubBehavior]]
 * : This actor is created by framework which is wrapped into [[csw.framework.CurrentStatePublisher]] for easy interaction with this actor
 * and then passed to component handlers so that component can publish their [[csw.messages.params.states.CurrentState]].
 *
 * If one component (ex. Assembly) is interested in [[csw.messages.params.states.CurrentState]] published by other component (ex. HCD)
 * then Assembly can subscribe to HCD's current state.
 *
 * PubSub actor maintains the list of subscribers and keeps publishing [[csw.messages.params.states.CurrentState]] to all subscribers.
 *
 * - [[csw.framework.internal.container.ContainerBehavior]]
 * : When multiple components needs to be started in container, then this actor is created.
 * Job of this actor is just to logically group multiple components and support [[csw.messages.SupervisorContainerCommonMessages]].
 * It receives [[csw.messages.SupervisorContainerCommonMessages.Shutdown]] or [[csw.messages.SupervisorContainerCommonMessages.Restart]] message
 * and forwards it to all the components residing in this container.
 *
 * == deploy package ==
 *
 * - [[csw.framework.deploy.containercmd.ContainerCmd]]
 * : ContainerCmd is a helper utility provided by framework to start multiple components in container mode
 * or single component in Standalone mode.
 *
 * === Example of Container Config File ===
 *
 * Here is an example of a config file for creating a container with the name ''IRIS_Container'' that
 * contains one assembly (named ''Filter'') and depends on the services of two HCDs (''Instrument_Filter'' and ''Disperser'').
 * Factory class for assembly is `csw.common.components.framework.SampleComponentBehaviorFactory`.
 * A `Supervisor` actor will be created to manage the assembly, which includes registering it with the
 * location service, using the given name and prefix. The prefix can be used to distribute parts of the
 * configurations to different HCDs. HCDs register themselves with the Location Service and specify a unique
 * prefix that can be used for this purpose.
 *
 * {{{
 *
 *    name = "IRIS_Container"
 *    components: [
 *      {
 *        name = "Filter"
 *        componentType = assembly
 *        behaviorFactoryClassName = csw.common.components.framework.SampleComponentBehaviorFactory
 *        prefix = tcs.mobie.blue.filter
 *        locationServiceUsage = RegisterOnly
 *        connections = [
 *          {
 *            name: Instrument_Filter
 *            componentType: hcd
 *            connectionType: akka
 *          },
 *          {
 *            name: Disperser
 *            componentType: hcd
 *            connectionType: akka
 *          }
 *        ]
 *      },
 *      {
 *        name = "Instrument_Filter"
 *        componentType = hcd
 *        behaviorFactoryClassName = csw.common.components.framework.SampleComponentBehaviorFactory
 *        prefix = tcs.mobie.blue.filter
 *        locationServiceUsage = RegisterOnly
 *      },
 *      {
 *        name = "Disperser"
 *        componentType: hcd
 *        behaviorFactoryClassName: csw.common.components.framework.SampleComponentBehaviorFactory
 *        prefix: tcs.mobie.blue.disperser
 *        locationServiceUsage = RegisterOnly
 *      }
 *    ]
 *
 * }}}
 *
 * === Example of Standalone Config File ===
 *
 * {{{
 *
 *    name = "IFS_Detector"
 *    componentType = hcd
 *    behaviorFactoryClassName = csw.common.components.framework.SampleComponentBehaviorFactory
 *    prefix = iris.ifs
 *    locationServiceUsage = RegisterOnly
 *    connections = []
 *
 * }}}
 *
 * - [[csw.framework.deploy.hostconfig.HostConfig]]
 * : This is just a helper to create host configuration application.
 * This support starting multiple containers on a given host machine and each container will have single/multiple components.
 *
 * === Example of Host Config file ===
 *
 * {{{
 *
 *    # This is a host configuration file which contains list of containers to be spawned by host configuration app
 *    containers: [
 *        {
 *          # mode can be one of Container or Standalone
 *          mode: "Container"
 *          # path of individual container configuration file
 *          configFilePath: "/resources/assemblyContainer.conf"
 *          # provide 'Remote' if file needs to fetched from config service else
 *          # provide 'Local' to fetch file from local machine
 *          configFileLocation: "Local"
 *        },
 *        {
 *          mode: "Standalone"
 *          configFilePath: "/resources/hcdStandalone.conf"
 *          configFileLocation: "Local"
 *        }
 *    ]
 *
 * }}}
 *
 */
package object framework {}
