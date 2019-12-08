import model.Game
import model.Tile
import model.Vec2Double

//This function prevents shooting if the enemy is not in line of sight. Returns true if enemy is in line of sight, otherwise return false.
fun dont_shoot_the_wall(unit: model.Unit, nearestEnemy: model.Unit?, game: Game): Boolean{
    // Example usage: action.shoot = dont_shoot_the_wall(unit, nearestEnemy, game)

    // If there is a tile between you and the enemy, don't shoot man!
    // Which way is the enemy?
    if (nearestEnemy == null){
        // There's no-one left!
        return false
    }
    val whichWay = if (unit.position.x < nearestEnemy.position.x) "right" else "left"
    val upOrDown = if (unit.position.y < nearestEnemy.position.y) "above" else "below"

    //Find out if there are WALL tiles between the units:
    if( kotlin.math.abs(unit.position.x - nearestEnemy.position.x) >= kotlin.math.abs(unit.position.y - nearestEnemy.position.y)){ //The target is on your right or left (more or less) => iterate at x
        var x = unit.position.x
        if(whichWay=="right"){
            while (x < nearestEnemy.position.x) {
                x++
                if (game.level.tiles[x.toInt()][findY(unit.position, nearestEnemy.position, x).toInt()] == Tile.WALL) {
                    return false
                }
            }
        }
        else{
            while (x > nearestEnemy.position.x) {
                x--
                if (game.level.tiles[x.toInt()][findY(unit.position, nearestEnemy.position, x).toInt()] == Tile.WALL) {
                    return false
                }
            }
        }
    }
    else{  //The target is above or below you (more or less) => iterate at y
        var y = unit.position.y
        if(upOrDown=="above"){
            while (y < nearestEnemy.position.y) {
                y++
                if (game.level.tiles[y.toInt()][findX(unit.position, nearestEnemy.position, y).toInt()] == Tile.WALL) {
                    return false
                }
            }
        }
        else{
            while (y > nearestEnemy.position.y) {
                y--
                if (game.level.tiles[y.toInt()][findX(unit.position, nearestEnemy.position, y).toInt()] == Tile.WALL) {
                    return false
                }
            }
        }
    }
    
    // Otherwise go for it
    return true
}

/*//Returns true if the point (x,y) belongs to the line which connects the unit with the target
fun belongsToLine(unitPosition: Vec2Double?, targetPosition: Vec2Double?, x: Double, y: Double): Boolean{
    if(unitPosition == null || targetPosition==null)
        return false

    if((y - unitPosition.y)*(targetPosition.x - unitPosition.x) == (targetPosition.y - unitPosition.y)*(x - unitPosition.x))
        return true
    return false
}*/

//Line equation: (y-y1)*(x2-x1) = (y2-y1)*(x-x1)
fun findY(unitPosition: Vec2Double, targetPosition: Vec2Double, x: Double): Double{
    //y = (y2-y1)*(x-x1)/(x2-x1) + y1
    return (targetPosition.y - unitPosition.y)*(x - unitPosition.x)/(targetPosition.x - unitPosition.x) + unitPosition.y
}
fun findX(unitPosition: Vec2Double, targetPosition: Vec2Double, y: Double): Double{
    //x = (y-y1)*(x2-x1)/(y2-y1) + x1
    return (y - unitPosition.y)*(targetPosition.x - unitPosition.x)/(targetPosition.y - unitPosition.y) + unitPosition.x
}
