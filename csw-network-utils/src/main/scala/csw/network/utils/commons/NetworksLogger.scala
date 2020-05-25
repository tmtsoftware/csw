package csw.network.utils.commons

/**
 * All the logs generated from networks util will have a fixed prefix, which is "networks-util".
 * The prefix helps in production to filter out logs from a particular component and this case, it helps to filter out logs
 * generated from location service.
 */
private[network] object NetworksLogger
