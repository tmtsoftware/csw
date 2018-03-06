package csw.services

/**
 * == Location Service ==
 *
 * The Location Service implemented in this project is based on CRDT (Conflict Free Replicated Data).
 * The Location Service helps you resolve the hostname and port number for a services which can be used for further communication,
 * In case of Akka connection, It helps you to resolve component reference with which you can send messages
 * as well as other information for example, logAdminActorRef which is used to dynamically change the log level of component.
 *
 * Location Service supports three types of services as follows: Akka, Tcp and HTTP based services.
 *
 * Before calling LocationServiceFactory.make() to get an handle of LocationService,
 * it is recommended to have below environment variables set to System global environments or system properties:
 *  - interfaceName (The network interface where akka cluster is formed.) Ex. interfaceName=eth0 if not set, first inet4 interface by priority will be picked
 *  - clusterSeeds (The host address and port of the seedNodes of the cluster) Ex. clusterSeeds="192.168.121.10:3552, 192.168.121.11:3552"
 *  - clusterPort (Specify port on which to start this service) Ex. clusterPort=3552 if this property is not set, service will start on random port.
 *
 * To use location service, first you have to get an instance of location service using below code:
 * {{{val locationService: LocationService = LocationServiceFactory.make()}}}
 *
 * === Register API ===
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
 * === Resolve/Find API ===
 * If you are unsure of whether service/component entry exists in location service or not and fine to wait for its availability for some duration,
 * then you can use below API to resolve Akka connection:
 *
 *     {{{locationService.resolve(TcpConnection(componentId), 2.seconds))}}}
 *
 * If you are sure that service/component entry exists in location service,
 * then you can use below API to find Akka connection from local replicated database:
 *
 *     {{{locationService.find(TcpConnection(componentId))}}}
 *
 * === Resolve and Communicate with Component ===
 * If you want to resolve some component and then want to send messages to that component,
 * then you should use resolve/find API which gives Location.
 * Using this Location, you can create command service like
 *
 * {{{new CommandService(akkaLocation)}}
 *
 * then using this instance of command service, you can send messages to component.
 * For more details, visit command service documentation.
 */
package object location {}
