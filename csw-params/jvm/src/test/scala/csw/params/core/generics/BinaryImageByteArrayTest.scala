package csw.params.core.generics

import java.nio.file.{Files, Paths}

import csw.commons.tagobjects.FileSystemSensitive
import csw.params.core.generics.KeyType.ByteArrayKey
import csw.params.core.models.Units.encoder
import csw.params.core.models._
import org.scalatest.{FunSpec, Matchers}

class BinaryImageByteArrayTest extends FunSpec with Matchers {

  // DEOPSCSW-186: Binary value payload
  describe("test ByteArrayKey") {
    // DEOPSCSW-186: Binary value payload
    it("should able to create parameter representing binary image", FileSystemSensitive) {
      val keyName                        = "imageKey"
      val imageKey: Key[ArrayData[Byte]] = ByteArrayKey.make(keyName)

      val imgPath  = Paths.get(getClass.getResource("/smallBinary.bin").getPath)
      val imgBytes = Files.readAllBytes(imgPath)

      val binaryImgData: ArrayData[Byte]          = ArrayData.fromArray(imgBytes)
      val binaryParam: Parameter[ArrayData[Byte]] = imageKey -> binaryImgData withUnits encoder

      binaryParam.head shouldBe binaryImgData
      binaryParam.value(0) shouldBe binaryImgData
      binaryParam.units shouldBe encoder
      binaryParam.keyName shouldBe keyName
      binaryParam.size shouldBe 1
      binaryParam.keyType shouldBe KeyType.ByteArrayKey
    }
  }
}
