/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw

/**
 * == Framework ==
 *
 * This project contains the framework for creating components, such as HCDs and Assemblies.
 *
 * A component is implemented by extending the [[csw.framework.scaladsl.ComponentHandlers]] base class.
 * These handlers are executed under an Supervisor Actor: `SupervisorBehavior`
 * and TLA Actor (Top Level Actor): `ComponentBehaviour`
 * defined in the framework which handles the lifecycle and supervision of this component.
 *
 * Components are controlled by a `SupervisorBehavior` actor
 * that intercepts common messages (ex. Shutdown, Restart) or lifecycle messages (ex. GoOffline or GoOnline)
 * sent from external entities to determine the state of the component.
 *
 * Components that are not in the `Running` state, do not receive `CommandMessage`.
 *
 * When Component is created using this framework, it guarantees that component (HCD/Assembly) is registered with
 * LocationService only when Component moves to `Running` state.
 * That means, one component can resolve other component only when its in ``Running`` state and ready to accept `CommandMessage`.
 *
 * === Important Actors in Framework ===
 *
 * - `SupervisorBehavior`
 * : Each component created by this framework is supervised by this actor.
 *
 * Default strategy of supervisor is to stop child actor but depending on nature of the failure, the supervisor has a choice of the following two options:
 *  - [[csw.framework.exceptions.FailureRestart]] : Restart the child actor i.e. kill the current child actor that failed and create a new one in its place
 *  - [[csw.framework.exceptions.FailureStop]] : Stop the child actor permanently
 *
 * Supervisor changes its [[csw.command.client.models.framework.SupervisorLifecycleState]] based on the messages it receives.
 * Decision to handle external messages or not is taken by the supervisor based on its current [[csw.command.client.models.framework.SupervisorLifecycleState]].
 * For complete list of supported messages per [[csw.command.client.models.framework.SupervisorLifecycleState]], see `csw-messages` project.
 *
 * - `ComponentBehaviour`
 * : Like Supervisor, evey component is associate with this actor which is known as TLA (Top Level Actor)
 * And it also maintains its own state `ComponentLifecycleState` based on messages it receives.
 *
 * Main purpose of this actor is to invoke component specific code written in their corresponding handlers.
 * This is where framework code meets Component specific code.
 *
 * - `PubSubBehavior`
 * : This actor is created by framework which is wrapped into [[csw.framework.CurrentStatePublisher]] for easy interaction with this actor
 * and then passed to component handlers so that component can publish their [[csw.params.core.states.CurrentState]].
 *
 * If one component (ex. Assembly) is interested in [[csw.params.core.states.CurrentState]] published by other component (ex. HCD)
 * then Assembly can subscribe to HCD's current state.
 *
 * PubSub actor maintains the list of subscribers and keeps publishing [[csw.params.core.states.CurrentState]] to all subscribers.
 *
 * - `ContainerBehavior`
 * : When multiple components needs to be started in container, then this actor is created.
 * Job of this actor is just to logically group multiple components and support [[csw.command.client.messages.SupervisorContainerCommonMessages]].
 * It receives [[csw.command.client.messages.SupervisorContainerCommonMessages.Shutdown]] or [[csw.command.client.messages.SupervisorContainerCommonMessages.Restart]] message
 * and forwards it to all the components residing in this container.
 *
 * == deploy package ==
 *
 * - `ContainerCmd`
 * : ContainerCmd is a helper utility provided by framework to start multiple components in container mode
 * or single component in Standalone mode.
 *
 * === Example of Container Config File ===
 *
 * Here is an example of a config file for creating a container with the name ''IRIS_Container'' that
 * contains one assembly (named ''Filter'') and depends on the services of two HCDs (''Instrument_Filter'' and ''Disperser'').
 * Handler class for assembly is `csw.common.components.framework.SampleComponentHandlers`.
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
 *        componentHandlerClassName = csw.common.components.framework.SampleComponentHandlers
 *        prefix = tcs.mobie.blue.filter
 *        locationServiceUsage = RegisterOnly
 *        connections = [
 *          {
 *            name: Instrument_Filter
 *            componentType: hcd
 *            connectionType: pekko
 *          },
 *          {
 *            name: Disperser
 *            componentType: hcd
 *            connectionType: pekko
 *          }
 *        ]
 *      },
 *      {
 *        name = "Instrument_Filter"
 *        componentType = hcd
 *        componentHandlerClassName = csw.common.components.framework.SampleComponentHandlers
 *        prefix = tcs.mobie.blue.filter
 *        locationServiceUsage = RegisterOnly
 *      },
 *      {
 *        name = "Disperser"
 *        componentType: hcd
 *        componentHandlerClassName: csw.common.components.framework.SampleComponentHandlers
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
 *    componentHandlerClassName = csw.common.components.framework.SampleComponentHandlers
 *    prefix = iris.ifs
 *    locationServiceUsage = RegisterOnly
 *    connections = []
 *
 * }}}
 *
 * - `HostConfig`
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
 *          # path of individual container configuration file
 *          configFilePath: "/resources/assemblyContainer.conf"
 *          # provide 'Remote' if file needs to fetched from config service else
 *          # provide 'Local' to fetch file from local machine
 *          configFileLocation: "Local"
 *        },
 *        {
 *          configFilePath: "/resources/hcdStandalone.conf"
 *          configFileLocation: "Local"
 *        }
 *    ]
 *
 * }}}
 */
package object framework {}
