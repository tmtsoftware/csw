package csw.network.utils.commons
import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.CSW

/**
 * All the logs generated from networks util will have a fixed prefix, which is "networks-util".
 * The prefix helps in production to filter out logs from a particular component and this case, it helps to filter out logs
 * generated from location service.
 */
private[network] object NetworksLogger extends LoggerFactory(Prefix(CSW, "networks_util"))
