import model.Game
import model.Tile

fun dont_shoot_the_wall(unit: model.Unit, nearestEnemy: model.Unit?, game: Game): Boolean{
    // Example usage: action.shoot = dont_shoot_the_wall(unit, nearestEnemy, game)

    // If there is a tile between you and the enemy, don't shoot man!
    // Which way is the enemy?
    if (nearestEnemy == null){
        // There's no-one left!
        return false
    }
    val whichWay = if (unit.position.x < nearestEnemy.position.x) "right" else "left"
    // Well don't shoot if there's a wall between you!
    if (whichWay == "left" && game.level.tiles[unit.position.x.toInt() - 1][unit.position.y.toInt()] == Tile.WALL || whichWay == "right" && game.level.tiles[unit.position.x.toInt() + 1][unit.position.y.toInt()] == Tile.WALL) {
        return false
    }
    // Otherwise go for it
    return true
}
