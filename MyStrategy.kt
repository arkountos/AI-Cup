import model.*
import model.Unit
import java.lang.Exception

//import dont_shoot_the_wall

class MyStrategy {
    private val LEFT = "left"
    private val RIGHT = "right"
    private val UP = "up"
    private val DOWN = "down"
    private var prevPositionOfEnemy: Vec2Double? = null
    private var divergence: Vec2Double = Vec2Double(1.0, 1.0)

    fun getAction(unit: model.Unit, game: Game, debug: Debug): UnitAction {
        var nearestEnemy: model.Unit? = null
        for (other in game.units) {
            if (other.playerId != unit.playerId) {
                if (nearestEnemy == null || distanceSqr(unit.position,
                                other.position) < distanceSqr(unit.position, nearestEnemy.position)) {
                    nearestEnemy = other
                }
            }
        }
        var nearestWeapon: LootBox? = null
        for (lootBox in game.lootBoxes) {
            if (lootBox.item is Item.Weapon) {
                if (nearestWeapon == null || distanceSqr(unit.position,
                                lootBox.position) < distanceSqr(unit.position, nearestWeapon.position)) {
                    nearestWeapon = lootBox
                }
            }
        }
        var nearestMedkit: LootBox? = null
        for (lootBox in game.lootBoxes) {
            if (lootBox.item is Item.HealthPack) {
                if (nearestMedkit == null || distanceSqr(unit.position,
                                lootBox.position) < distanceSqr(unit.position, nearestMedkit.position)) {
                    nearestMedkit = lootBox
                }
            }
        }

        var targetPos: Vec2Double = unit.position
        if (unit.weapon == null && nearestWeapon != null) {
            targetPos = nearestWeapon.position
        } else if (unit.health < 70 && nearestMedkit != null) {
            targetPos = nearestMedkit.position
        } else if (nearestEnemy != null) {
            targetPos = nearestEnemy.position
        }
        // This needs to happen here instead of the end of the code so that
        // we can adjust the target if we need to change our weapon
        val action = UnitAction()
        action.swapWeapon = omg_is_that_a_pistol(unit)
        if (action.swapWeapon && nearestWeapon != null){
            targetPos = nearestWeapon.position
        }

        var jump = targetPos.y > unit.position.y
        var velocity = targetPos.x - unit.position.x

        //Avoid any planted mines (that need to be avoided):
        val avoidMineResults = avoidMine(unit, game, targetPos)
        if(avoidMineResults != null){
            if(avoidMineResults[0]==1.0)
                jump = true
            velocity = avoidMineResults[1]
            targetPos.x = avoidMineResults[2]
            targetPos.y = avoidMineResults[3]
        }

        if (targetPos.x > unit.position.x && game.level.tiles[(unit.position.x + 1).toInt()][(unit.position.y).toInt()] == Tile.WALL) {
            jump = true
        }
        if (targetPos.x < unit.position.x && game.level.tiles[(unit.position.x - 1).toInt()][(unit.position.y).toInt()] == Tile.WALL) {
            jump = true
        }
        //println(game.level.tiles[unit.position.x.toInt()][(unit.position.y).toInt()-1]==Tile.EMPTY && unit.jumpState.canCancel)

        if((unit.position.y-1).toInt()>=0 && game.level.tiles[unit.position.x.toInt()][(unit.position.y-1).toInt()]==Tile.PLATFORM && unit.jumpState.canCancel) {
            //jump = false
        }

        //At start of game: save the initial position of the nearest enemy for later use
        if(prevPositionOfEnemy==null){
            prevPositionOfEnemy = nearestEnemy?.position
        }

        action.velocity = if (velocity < 0) -game.properties.unitMaxHorizontalSpeed else game.properties.unitMaxHorizontalSpeed
        action.jump = jump
        action.jumpDown = !jump
        action.aim = adaptAim(unit, nearestEnemy)
        action.shoot = false //dont_shoot_the_wall(unit, nearestEnemy, game)
        action.reload = false
        action.plantMine = plantMine(unit, nearestEnemy, game)
        return action
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private fun adaptAim(unit: model.Unit, nearestEnemy: model.Unit?): Vec2Double{
        var aim = Vec2Double(0.0, 0.0)
        if (nearestEnemy != null) {
            aim = Vec2Double(nearestEnemy.position.x - unit.position.x,nearestEnemy.position.y - unit.position.y)
        }





        //TODO divergence




        aim.x *= divergence.x
        aim.y *= divergence.y
        return aim
    }

    private fun plantMine(unit: model.Unit, nearestEnemy: model.Unit?, game: Game): Boolean{
        if(nearestEnemy != null && unit.mines!=0 && distanceSqr(unit.position, nearestEnemy.position) <= game.properties.mineExplosionParams.radius)
            return true
        return false
    }

    /* AvoidMine:
    Return null if there is no need to avoid any mine.
    Else, return a DoubleArray containing the following:
    avoidMine[0] = jump = 1.0 in order to jump, and = 0.0 otherwise
    avoidMine[1] = new velocity
    avoidMine[2] = new targetPos.x
    avoidMine[3] = new targetPos.y */
    private fun avoidMine(unit: model.Unit, game: Game, targetPos: Vec2Double): DoubleArray? {
        var jump = 0.0
        //var velocity = targetPos.x - unit.position.x
        val velocity: Double

        var nearestPlantedMine: Mine? = null
        for (mine in game.mines) {
            if (nearestPlantedMine == null || distanceSqr(unit.position, mine.position) < distanceSqr(unit.position, nearestPlantedMine.position)) {
                nearestPlantedMine = mine
            }
        }

        if (nearestPlantedMine != null && nearestPlantedMine.state == MineState.TRIGGERED && distanceSqr(unit.position, nearestPlantedMine.position) < 10*nearestPlantedMine.explosionParams.radius) {
            if(nearestPlantedMine.position.y <= unit.position.y) //If the mine is below you, jump
                jump = 1.0

            if (unit.position.x > nearestPlantedMine.position.x)
                targetPos.x = nearestPlantedMine.position.x + 100 * nearestPlantedMine.explosionParams.radius
            else
                targetPos.x = nearestPlantedMine.position.x - 100 * nearestPlantedMine.explosionParams.radius
            velocity = targetPos.x

            return doubleArrayOf(jump, velocity, targetPos.x, targetPos.y)
        }
        return null
    }

    /*Checks if there are obstacles in a rectangle area (distance x unit.size.y+1), in specific direction
    - Returns true if there are WALL tiles in the rectangle.
    - Returns null if there is an enemy close to you in that direction
    - Returns false otherwise*/
    private fun hasObstacles(unit: Unit, nearestEnemy: Unit?, game: Game, direction: String, distance: Double): Boolean?{
        val rangeX: List<Int>
        val rangeY: List<Int>

        if(direction == LEFT){
            rangeX = ((unit.position.x.toInt() - unit.size.x/2 - distance).toInt() .. unit.position.x.toInt()).toList()  //Any exceptions from the map edges will be later caught and ignored
            rangeY = ( unit.position.y.toInt() .. (unit.position.y + unit.size.y).toInt()).toList()

            //If the enemy is on your left, even if they're outside the rectangle, return true because of their unpredictable movement (provided that they are relatively close).
            if( nearestEnemy!=null && nearestEnemy.position.x < unit.position.x && distanceSqr(unit.position, nearestEnemy.position) <= 1.5*distance)
                return null
        }
        else if(direction == RIGHT){
            rangeX = (unit.position.x.toInt() .. (unit.position.x + unit.size.x/2 + distance).toInt()).toList()
            rangeY = (unit.position.y.toInt() .. (unit.position.y + unit.size.y).toInt()).toList()

            if( nearestEnemy!=null && nearestEnemy.position.x > unit.position.x && distanceSqr(unit.position, nearestEnemy.position) <= 1.5*distance)
                return null
        }
        else if(direction == UP){
            rangeX = ((unit.position.x - unit.size.x/2).toInt() .. (unit.position.x + unit.size.x/2).toInt()).toList()
            rangeY = (unit.position.y.toInt() .. (unit.position.y + unit.size.y + distance).toInt()).toList()

            if( nearestEnemy!=null && nearestEnemy.position.y > (unit.position.y + unit.size.y) && distanceSqr(unit.position, nearestEnemy.position) <= 1.5*distance)
                return null
        }
        else{
            rangeX = ((unit.position.x - unit.size.x/2).toInt() .. (unit.position.x + unit.size.x/2).toInt()).toList()
            rangeY = (unit.position.y.toInt() .. (unit.position.y - distance).toInt()).toList()

            if( nearestEnemy!=null && nearestEnemy.position.y < unit.position.y && distanceSqr(unit.position, nearestEnemy.position) <= 1.5*distance)
                return null
        }

        //Search the square:
        for(x in rangeX){
            for(y in rangeY){
                try{
                    if(game.level.tiles[x][y] == Tile.WALL)
                        return true
                }
                catch(e: ArrayIndexOutOfBoundsException){}
            }
        }
        return false
    }

    companion object {
        internal fun distanceSqr(a: Vec2Double, b: Vec2Double): Double {
            return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)
        }
    }
}