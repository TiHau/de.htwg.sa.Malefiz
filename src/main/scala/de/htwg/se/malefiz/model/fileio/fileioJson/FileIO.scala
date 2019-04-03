package de.htwg.se.malefiz.model.fileio.fileioJson

import java.nio.file.{Files, Paths}

import de.htwg.se.malefiz.controller.{ControllerInterface, State}
import de.htwg.se.malefiz.model.fileio.FileIOInterface
import de.htwg.se.malefiz.model.gameboard._
import play.api.libs.json._

import scala.io.Source

class FileIO extends FileIOInterface {
  override def load(controller: ControllerInterface): Unit = {
    if (Files.exists(Paths.get("saveFile.json"))) {
      val source: String = Source.fromFile("saveFile.json").getLines.mkString
      val json: JsValue = Json.parse(source)
      loadBoard(json, controller)
      loadController(json, controller)
      controller.notifyObservers()
    }
  }

  private def loadController(json: JsValue, controller: ControllerInterface): Unit = {
    val activeColor = (json \ "controller" \ "activePlayer").get.toString().toInt
    activeColor match {
      case 1 => controller.activePlayer = controller.gameBoard.player1
      case 2 => controller.activePlayer = controller.gameBoard.player2
      case 3 => controller.activePlayer = controller.gameBoard.player3
      case _ => controller.activePlayer = controller.gameBoard.player4
    }

    controller.diced = (json \ "controller" \ "diced").get.toString().toInt
    controller.state = State.fromString((json \ "controller" \ "state").get.toString().drop(1).dropRight(1)).get
    val startFieldX = (json \ "controller" \ "choosenPlayerStone" \ "startX").get.toString().toInt
    val startFieldY = (json \ "controller" \ "choosenPlayerStone" \ "startY").get.toString().toInt
    val playerStones =
      controller.gameBoard.player1.stones ++
        controller.gameBoard.player2.stones ++
        controller.gameBoard.player3.stones ++
        controller.gameBoard.player4.stones

    for (playerStone <- playerStones) {
      if (playerStone.startField.x == startFieldX && playerStone.startField.y == startFieldY) {
        controller.setChoosenPlayerStone(playerStone)
      }
    }

    controller.setDestField(controller.gameBoard.board(
      (json \ "controller" \ "destField" \ "x").get.toString().toInt)(
      (json \ "controller" \ "destField" \ "y").get.toString().toInt).get)

    controller.needToSetBlockStone = (json \ "controller" \ "needToSetBlockStone").get.toString().toBoolean
  }

  private def loadBoard(json: JsValue, controller: ControllerInterface): Unit = {
    val playerCount = json \ "board" \ "playerCount"
    controller.setPlayerCount(playerCount.get.toString().toInt)
    val jsV: JsValue = Json.parse("" + (json \ "board" \\ "fields").head + "")
    val fieldNodes = jsV.validate[List[JsValue]].get
    for (fieldNode <- fieldNodes) {
      if (!(fieldNode \ "isFreeSpace").get.toString.toBoolean) {
        val x = (fieldNode \ "x").get.toString.toInt
        val y = (fieldNode \ "y").get.toString().toInt
        controller.gameBoard.board(x)(y).get.avariable = (fieldNode \ "avariable").get.toString.toBoolean
        (fieldNode \ "sort").get.toString.charAt(1) match {
          case 'p' =>
            val startFieldX = (fieldNode \ "startFieldX").get.toString.toInt
            val startFieldY = (fieldNode \ "startFieldY").get.toString.toInt

            val playerStones =
              controller.gameBoard.player1.stones ++
                controller.gameBoard.player2.stones ++
                controller.gameBoard.player3.stones ++
                controller.gameBoard.player4.stones

            for (playerStone <- playerStones) {
              if (playerStone.startField.x == startFieldX && playerStone.startField.y == startFieldY) {
                playerStone.actualField = controller.gameBoard.board(x)(y).get
                controller.gameBoard.board(x)(y).get.stone = Some(playerStone)
              }
            }
          case 'b' =>
            controller.gameBoard.board(x)(y).get.stone = Some(BlockStone())
          case 'f' =>
            controller.gameBoard.board(x)(y).get.stone = None
        }
      }
    }
  }

  override def save(controller: ControllerInterface): Unit = {
    import java.io._
    val pw = new PrintWriter(new File("saveFile.json"))
    pw.write(Json.prettyPrint(gameToJson(controller)))
    pw.close()
  }

  def gameToJson(controller: ControllerInterface): JsObject = {
    Json.obj(
      "activePlayer" -> JsNumber(controller.activePlayer.color),
      "diced" -> JsNumber(controller.diced),
      "playerCount" -> JsNumber(controller.gameBoard.playerCount),
      "blockStones" -> Json.toJson(
        (0 to 16) flatMap (x =>
          (0 to 13) filter (y => controller.gameBoard.board(x)(y).isDefined)
            filter (y => controller.gameBoard.board(x)(y).get.stone.isDefined)
            filter (y => controller.gameBoard.board(x)(y).get.stone.get.isInstanceOf[BlockStone])
            map (y => Json.obj("x" -> JsNumber(x), "y" -> JsNumber(y)))
          )
      ),
      "playerStones" -> Json.toJson(
        (0 to 16) flatMap (x =>
          (0 to 15) filter (y => controller.gameBoard.board(x)(y).isDefined)
            filter (y => controller.gameBoard.board(x)(y).get.stone.isDefined)
            filter (y => controller.gameBoard.board(x)(y).get.stone.get.isInstanceOf[PlayerStone])
            map (y => Json.obj(
            "x" -> JsNumber(x),
            "y" -> JsNumber(y),
            "startFieldX" -> JsNumber(controller.gameBoard.board(x)(y).get.stone.get.asInstanceOf[PlayerStone].startField.x),
            "startFieldY" -> JsNumber(controller.gameBoard.board(x)(y).get.stone.get.asInstanceOf[PlayerStone].startField.y),
            "playerColor" -> JsNumber(controller.gameBoard.board(x)(y).get.stone.get.asInstanceOf[PlayerStone].playerColor)
          ))
          )
      ))
  }
}