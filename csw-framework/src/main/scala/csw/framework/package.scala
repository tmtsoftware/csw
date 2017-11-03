package csw

/**
 * This project contains the framework for creating components, such as HCDs and Assemblies.
 *
 * A Component is composed of an actor instance of [[csw.framework.internal.component.ComponentBehavior]] and an
 * implementation of [[csw.framework.scaladsl.ComponentHandlers]] which defines a given component.
 * Each component is created by an actor instance of [[csw.framework.internal.supervisor.SupervisorBehavior]].
 *
 * Components are either created by an actor instance of [[csw.framework.internal.container.ContainerBehavior]] or as a
 * Standalone actor instance of [[csw.framework.internal.supervisor.SupervisorBehavior]] from a configuration file.
 *
 * === Container Config Files ===
 *
 * Here is an example of a config file for creating a container with the name ''Container-1'' that
 * contains one assembly (named ''Assembly-1'') and depends on the services of two HCDs (''HCD-2A'' and ''HCD-2B'').
 * The assembly is implemented by the given class (`csw.services.pkg.TestAssembly`).
 * A `Supervisor` actor will be created to manage the assembly, which includes registering it with the
 * location service, using the given name and prefix. The prefix can be used to distribute parts of the
 * configurations to different HCDs. HCDs register themselves with the Location Service and specify a unique
 * prefix that can be used for this purpose.
 *
 * {{{
 * container {
 * name = Container-1
 * components {
 *   Assembly-1 {
 *     type = Assembly
 *     class = csw.pkgDemo.assembly1.Assembly1
 *     prefix = tcs.base.assembly1
 *     connectionType: [akka]
 *     connections = [
 *       {
 *         name: HCD-2A
 *         type: HCD
 *         connectionType: [akka]
 *       }
 *       {
 *         name: HCD-2B
 *         type: HCD
 *         connectionType: [akka]
 *       }
 *     ]
 *   }
 *  }
 * }
 * }}}
 *
 */
package object framework {}
