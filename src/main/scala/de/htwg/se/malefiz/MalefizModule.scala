package de.htwg.se.malefiz

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import de.htwg.sa.malefiz.model.db.GameConfigDaoSlick
import de.htwg.se.malefiz.controller.{Controller, ControllerInterface}
import de.htwg.se.malefiz.model.db.DaoInterface
import de.htwg.se.malefiz.model.gameboard.{GameBoard, GameBoardInterface}
import net.codingwell.scalaguice.ScalaModule

class MalefizModule extends AbstractModule with ScalaModule {
  val defaultSize: Int = 4
  val small: Int = 3
  val tiny: Int = 2
  def configure(): Unit = {
    bindConstant().annotatedWith(Names.named("DefaultSize")).to(defaultSize)
    //bind[GameBoardInterface].to[GameBoard]
    bind[ControllerInterface].to[Controller]
    bind[DaoInterface].to[GameConfigDaoSlick]
    bind[GameBoardInterface].annotatedWithName("default").toInstance(GameBoard(defaultSize))
    bind[GameBoardInterface].annotatedWithName("tiny").toInstance(GameBoard(tiny))
    bind[GameBoardInterface].annotatedWithName("small").toInstance(GameBoard(small))


  }
}
