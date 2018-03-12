package csw.services.config

/**
 * == Config Client ==
 *
 * Config client wraps the low level communication with ConfigServer and exposes simple to use methods to access and manage configuration files.
 * Component developers are recommended to use csw-config-client library in their code
 * to get the component specific configurations from config server.
 *
 * ConfigClient provides two API's :
 *   1. Client API
 *   2. Admin API
 *
 * === Client API ===
 * Config Client API is limited to obtain the “active” configuration file from the Configuration Service.
 * It is recommended for Component's writer to use client API as we expect component's to just retrieve configurations from config server
 * which then can be used for bootstrapping components or for any other purpose.
 *
 * You can get handle to Config Client API using following code which requires ActorSystem and LocationService:
 *
 * {{{val configClientService = ConfigClientFactory.clientApi(actorSystem, clientLocationService)}}}
 *
 *
 * Config Client Service exposes following two API's:
 *   1. exists: returns true if configuration file with provided id exists
 *
 *   {{{configClientService.exists(path, Some(id))}}}
 *
 *   2. getActive: returns the content's (ConfigData) of active version of provided file path
 *
 *   {{{configClientService.getActive(path)}}}
 *
 * === Admin API ===
 * This is an Admin API which is expected to be used by an administrative tool.
 * These tools would have the ability to create, delete, and update configurations, as well as retrieve past configurations and their history.
 * Any time a new configuration is to be used by a component,
 * the user must use one of these tools (via CLI, perhaps) to set the active configuration for a component.
 * Since a history of active configurations is maintained by the service,
 * the settings of each component each time it is run can be retrieved, and the system configuration at any moment can be recreated.
 *
 * You can get handle to Config Admin API using following code which requires ActorSystem and LocationService:
 *
 * {{{val configAdminService = ConfigClientFactory.adminApi(actorSystem, clientLocationService)}}}
 *
 * Config Admin Service exposes following API's:
 *   1.  create
 *   2.  update
 *   3.  getById
 *   4.  getLatest
 *   5.  getByTime
 *   6.  delete
 *   7.  list
 *   8.  history
 *   9.  historyActive
 *   10. setActiveVersion
 *   11. resetActiveVersion
 *   12. getActiveVersion
 *   13. getActiveByTime
 *   14. getMetadata
 *   15. exists
 *   16. getActive
 *
 * Detailed documentation of Config Client API usage is available at:
 * https://tmtsoftware.github.io/csw-prod/services/config.html
 */
package object client {}
