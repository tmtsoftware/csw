package csw.services

/**
 * == Location Service ==
 *
 * The Location Service implemented in this project is based on CRDT (Conflict Free Replicated Data).
 * The Location Service helps you to find out the hostname and port number for a service,
 * as well as other information for determining the correct URI to use, such as path,
 * actor system name etc.
 *
 * Location Service supports three types of services as follows: Akka, Tcp and HTTP based services.
 *
 * Before calling LocationServiceFactory.make() to get an handle of LocationService,
 * it is recommended to have below environment variables set to System global environments or system properties:
 *  - interfaceName (The network interface where cluster is formed.) Ex. interfaceName=eth0 if not set, first interface by priority will be picked
 *  - clusterSeeds (The host address of the seedNode of the cluster) Ex. clusterSeeds="192.168.121.10:3552, 192.168.121.11:3552"
 *  - clusterPort (Specify port on which to start this service) Ex. clusterPort=3552 if this property is not set, service will start on random port
 *
 * To use location service, first you have to get an instance of location service using below code:
 * {{{val locationService: LocationService = LocationServiceFactory.make()}}}
 *
 * To register an Akka actor based service, you can use code like this:
 *
 *     {{{locationService.register(AkkaRegistration(AkkaConnection(componentId), self))}}}
 *
 * Where self is a reference to the services own actorRef.
 *
 * To register an HTTP based service, you can make a call like this:
 *
 *     {{{locationService.register(HttpRegistration(HttpConnection(componentId), Port, Path))}}}
 *
 * Here you specify port and path like "/hcd/trombone", hostname for the local host is automatically determined.
 *
 * To register an Tcp based service, you can make a call like this:
 *
 *     {{{locationService.register(TcpRegistration(TcpConnection(componentId), Port))}}}
 *
 * Here you specify port and hostname for the local host is automatically determined.
 *
 */
package object location {

}
