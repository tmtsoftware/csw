package csw.location.wrapper

import csw.location.impl.internal.{ServerWiring, Settings}

// workaround to access server wiring which is private to csw in example app
class LocationServerWiring(val wiring: ServerWiring = new ServerWiring(Settings("csw-location-server")))
