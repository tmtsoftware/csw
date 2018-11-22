package csw.auth
import csw.auth.Keycloak.deployment
import csw.auth.token.RPT
import org.keycloak.authorization.client.{AuthzClient, Configuration}
import play.api.libs.json.Json

import scala.util.{Failure, Success}

private[auth] object Example extends App {

  private val configuration: Configuration = new Configuration(
    deployment.getAuthServerBaseUrl,
    deployment.getRealm,
    deployment.getResourceName,
    deployment.getResourceCredentials,
    deployment.getClient
  )
  private val authzClient: AuthzClient = AuthzClient.create(configuration)

  val accessToken = RPT(authzClient).create(
    "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJneWlMbndmWWR3SmNENUdnUnJXMUFCNmNzeVBLZWFPb2lMZHp4dnFBeUZjIn0.eyJqdGkiOiJlMGViYzJkMi00YzYwLTRiM2YtYmQ3Yi1hMWZkMjZhYTlhOGEiLCJleHAiOjE1NDI3MjAwMDQsIm5iZiI6MCwiaWF0IjoxNTQyNzE4MjA0LCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvYXV0aC9yZWFsbXMvbWFzdGVyIiwiYXVkIjpbImJyb3dzZXItYXBwIiwiYWtrYS1odHRwLXNlcnZlciJdLCJzdWIiOiIxNTBjYWIzYS0zNWNjLTQ3YWUtYWM2MC05ZGIyNjNkNzMzYmMiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJicm93c2VyLWFwcCIsIm5vbmNlIjoiYTE4NTIxNDQtNTkzNi00NjAyLThiNTUtMTI4YmYxZTViNTkwIiwiYXV0aF90aW1lIjoxNTQyNzE4MjAzLCJzZXNzaW9uX3N0YXRlIjoiNTVkODU2MzQtM2M5NC00MWQxLWE2ODUtNGQ2ODY2MDIzZDZiIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyIqIl0sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJkYl9hZG1pbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImJyb3dzZXItYXBwIjp7InJvbGVzIjpbImJyb3dzZXJfYXBwX3JvbGUiXX19LCJzY29wZSI6Im9wZW5pZCBlbWFpbCBwcm9maWxlIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJuYW1lIjoiam9obiBEYWxlIiwicHJlZmVycmVkX3VzZXJuYW1lIjoiam9obiIsImdpdmVuX25hbWUiOiJqb2huIiwiZmFtaWx5X25hbWUiOiJEYWxlIiwiZW1haWwiOiJqb2huQG1haWwuY29tIn0.imVBsPxpaW-D9hfuq8lC43yZwVIVsJYLjoaldiwpFkOcUwzouM_SWSTheUW3Umh6mZI-Ul_Fr9v-kL1kLaoaEuF3PXBmM-Ct4cveWdGvk3FrpC4uLZ1s5vJOvjon8UKeBU-61uw3uEbNtfqW9wy-DZXJacul_BFGrbcvQVY7SqXt3LkA2K0-gXuxsoNjlg6gvK3mDyvPzbCzAJMYRtrb1GVdmHJVZR7jmxMhb2ioKi6QpmPsnNn7Zc-4cEIJUlWK8ZZpDXri9WrJCxxfkPXUvF3T7Py9W7u574BM1N1xrUAnvCvuNwl2Hyzbzgl5XqU2uNxxtZZnK70cC5AJiJ7E5Q"
  )

  accessToken match {
    case Failure(failure) =>
      println(failure)
//      sys.exit(1)
    case Success(at) =>
      println(s"access token: $at")
      println(Json.prettyPrint(Json.toJson(at)))
  }
}
