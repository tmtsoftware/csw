package csw.params

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class UtilsTest extends AnyFunSpec with Matchers {
  case object Animal
  case class Cat()
  class Dog()

  sealed trait Color {
    def name: String = Utils.getClassName(this)
  }
  case object Red extends Color

  describe("getClassName") {
    it("should strip $ from the name of case object") {
      Utils.getClassName(Animal) shouldBe "Animal"
      Utils.getClassName(Red) shouldBe "Red"
    }

    it("should get name of case class") {
      Utils.getClassName(Cat()) shouldBe "Cat"
    }

    it("should get name of simple class") {
      Utils.getClassName(new Dog()) shouldBe "Dog"
    }
  }

}
