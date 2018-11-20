package csw.auth.adapters.nativeapp

import java.io.File

import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest.{BeforeAndAfterEach, FunSuite}
import os.Path

class FileAuthStoreTest extends FunSuite with BeforeAndAfterEach {

  private val path: os.Path    = os.Path(new File("test-tokens").getAbsolutePath)
  private val testAccessToken  = "test-access-token"
  private val testIdToken      = "test-id-token"
  private val testRefreshToken = "test-refresh-token"

  private val accessTokenPath: Path  = path / "access_token"
  private val idTokenPath: Path      = path / "id_token"
  private val refreshTokenPath: Path = path / "refresh_token"

  val store = new FileAuthStore(path.toNIO)

  override protected def beforeEach(): Unit = {

    os.write(accessTokenPath, testAccessToken)
    os.write(idTokenPath, testIdToken)
    os.write(refreshTokenPath, testRefreshToken)
  }

  override protected def afterEach(): Unit = {
    os.remove.all(path)
  }

  test("getAccessTokenString should read the access token from the respective file") {
    store.getAccessTokenString shouldBe Option(testAccessToken)
  }

  test("getIdTokenString should read the id token from the respective file") {
    store.getIdTokenString shouldBe Option(testIdToken)
  }

  test("getRefreshTokenString should read refresh the token from the respective file") {
    store.getRefreshTokenString shouldBe Option(testRefreshToken)
  }

  test("clear should remove all the token files") {
    store.clearStorage()
    os.exists(accessTokenPath) shouldBe false
    os.exists(idTokenPath) shouldBe false
    os.exists(refreshTokenPath) shouldBe false
  }

  test("saveTokens should save the given token to respective files") {
    val accessToken  = "refreshed-access-token"
    val idToken      = "refreshed-id-token"
    val refreshToken = "refreshed-refresh-token"

    store.saveTokens(idToken, accessToken, refreshToken)

    os.read(accessTokenPath) shouldBe accessToken
    os.read(idTokenPath) shouldBe idToken
    os.read(refreshTokenPath) shouldBe refreshToken
  }
}
