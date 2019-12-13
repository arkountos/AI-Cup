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
        debug.draw(CustomData.Log("Unit pos: ${unit.position.x}, ${unit.position.y}"))
        debug.draw(CustomData.Log("Unit size: ${unit.size.x}, ${unit.size.y}"))
        if (game.bullets.isNotEmpty()) {
            debug.draw(CustomData.Log("Unit size: ${game.bullets[0].velocity.x}, ${game.bullets[0].velocity.y}"))
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

//        if (targetPos.x > unit.position.x && game.level.tiles[(unit.position.x + 1).toInt()][(unit.position.y).toInt()] == Tile.WALL) {
//            jump = true
//        }
//        if (targetPos.x < unit.position.x && game.level.tiles[(unit.position.x - 1).toInt()][(unit.position.y).toInt()] == Tile.WALL) {
//            jump = true
//        }
        //println(game.level.tiles[unit.position.x.toInt()][(unit.position.y).toInt()-1]==Tile.EMPTY && unit.jumpState.canCancel)

        if((unit.position.y-1).toInt()>=0 && game.level.tiles[unit.position.x.toInt()][(unit.position.y-1).toInt()]==Tile.PLATFORM && unit.jumpState.canCancel) {
            //jump = false
        }

        //At start of game: save the initial position of the nearest enemy for later use
        if(prevPositionOfEnemy==null){
            prevPositionOfEnemy = nearestEnemy?.position
        }

        action.velocity = 0.0/*if (velocity < 0) -game.properties.unitMaxHorizontalSpeed else game.properties.unitMaxHorizontalSpeed*/
        action.jump = avoid_bullets(unit, nearestEnemy, game, debug)
        action.jumpDown = false
        action.aim = adaptAim(unit, nearestEnemy)
        action.shoot = dont_shoot_the_wall(unit, nearestEnemy, game)
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





    private fun avoid_bullets(unit: Unit, nearestEnemy: Unit?, game: Game, debug: Debug): Boolean{
        // We need the line that the nearest bullet is travelling on
        if (game.bullets.isEmpty()){
            return false
        }
        var topleft: Vec2Double = Vec2Double(unit.position.x - (unit.size.x / 2), unit.position.y + unit.size.y)
        var topright: Vec2Double = Vec2Double(unit.position.x + (unit.size.x / 2), unit.position.y + unit.size.y)
        var bottomleft: Vec2Double = Vec2Double(unit.position.x - (unit.size.x / 2), unit.position.y)
        var bottomright: Vec2Double? = Vec2Double(unit.position.x + (unit.size.x / 2), unit.position.y)

        // LEFT
        var my_bullet = game.bullets[0]

        var check_left_Y = findY(my_bullet.velocity, my_bullet.position, unit.position.x - (unit.size.x / 2))
        debug.draw(CustomData.Log("Check_left_y: ${check_left_Y}"))
        debug.draw(CustomData.Log("Bullet velocity: ${my_bullet.velocity.x}, ${my_bullet.velocity.y}"))
        debug.draw(CustomData.Line(p1 = Vec2Float(my_bullet.velocity.x.toFloat(), my_bullet.velocity.y.toFloat()), p2 = Vec2Float(my_bullet.position.x.toFloat(), my_bullet.position.y.toFloat()), width =  1.0.toFloat(), color =  model.ColorFloat(1.0F, 0.5F,0.2F,0.2F)))

        if (check_left_Y < topleft!!.y && check_left_Y > bottomleft!!.y){
            // The bullet will hit, jump just before it hits!
            print("Hit1!")
            if (my_bullet.position.x + my_bullet.velocity.x >= unit.position.x)
                print("Jump!")
                return true
        }
        // RIGHT
        var check_right_Y = findY(my_bullet.velocity, my_bullet.position, unit.position.x + (unit.size.x / 2))
        if (check_right_Y < topright!!.y && check_right_Y > bottomright!!.y){
            // The bullet will hit, jump just before it hits!
            print("Hit2!")
            return true
        }
        // UP
        var check_up_X = findX(my_bullet.velocity, my_bullet.position, unit.position.y + unit.size.y)
        if (check_up_X < topleft!!.x && check_up_X > topright!!.x){
            // The bullet will hit!
            print("Hit3!")
            return true
        }
        var check_down_X = findX(my_bullet.velocity, my_bullet.position, unit.position.y)
        if (check_down_X < bottomleft!!.x && check_down_X > bottomright!!.x){
            // The bullet will hit!
            print("Hit4!")
            return true

        }
        return false
    }






    private fun omg_is_that_a_pistol(unit: model.Unit): Boolean{
        // If the weapon i hold is a pistol
        if (unit.weapon?.typ == WeaponType.PISTOL) {
            return(true)
        }
        return(false)
    }

    //This function prevents shooting if the enemy is not in line of sight. Returns true if enemy is in line of sight, otherwise return false.
    private fun dont_shoot_the_wall(unit: model.Unit, nearestEnemy: model.Unit?, game: Game): Boolean{
        // Example usage: action.shoot = dont_shoot_the_wall(unit, nearestEnemy, game)

        // If there is a tile between you and the enemy, don't shoot man!
        // Which way is the enemy?
        if (nearestEnemy == null){
            // There's no-one left!
            return false
        }
        if(MyStrategy.distanceSqr(unit.position, nearestEnemy.position)<1.0) //If close to enemy, avoid all the calculations. SHOOT THEM IMMEDIATELY.
            return true

        val whichWay = if (unit.position.x < nearestEnemy.position.x) "right" else "left"
        val upOrDown = if (unit.position.y < nearestEnemy.position.y) "above" else "below"

        //Find out if there are WALL tiles between the units:
        if( kotlin.math.abs(unit.position.x - nearestEnemy.position.x) > kotlin.math.abs(unit.position.y - nearestEnemy.position.y)){ //The target is on your right or left (more or less) => iterate at x
            var x = unit.position.x
            if(whichWay=="right"){
                while (x < nearestEnemy.position.x) {
                    try {if (game.level.tiles[x.toInt()][findY(unit.position, nearestEnemy.position, x).toInt()] == Tile.WALL) { return false }}
                    catch (e: ArrayIndexOutOfBoundsException) {}
                    x++
                }
            }
            else{
                while (x > nearestEnemy.position.x) {
                    try {if (game.level.tiles[x.toInt()][findY(unit.position, nearestEnemy.position, x).toInt()] == Tile.WALL) { return false }}
                    catch (e: ArrayIndexOutOfBoundsException) {}
                    x--
                }
            }
        }
        else if( kotlin.math.abs(unit.position.x - nearestEnemy.position.x) < kotlin.math.abs(unit.position.y - nearestEnemy.position.y)){  //The target is above or below you (more or less) => iterate at y
            var y = unit.position.y
            if(upOrDown=="above"){
                while (y < nearestEnemy.position.y) {
                    try {if (game.level.tiles[y.toInt()][findX(unit.position, nearestEnemy.position, y).toInt()] == Tile.WALL) { return false }}
                    catch (e: ArrayIndexOutOfBoundsException) {}
                    y++
                }
            }
            else{
                while (y > nearestEnemy.position.y) {
                    try { if (game.level.tiles[y.toInt()][findX(unit.position, nearestEnemy.position, y).toInt()] == Tile.WALL) { return false }}
                    catch (e: ArrayIndexOutOfBoundsException) {}
                    y--
                }
            }
        }
        /*else{   //?????????????????????
            return false
        }*/

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
    private fun findY(unitPosition: Vec2Double, targetPosition: Vec2Double, x: Double): Double{
        //y = (y2-y1)*(x-x1)/(x2-x1) + y1
        return (targetPosition.y - unitPosition.y)*(x - unitPosition.x)/(targetPosition.x - unitPosition.x) + unitPosition.y
    }
    private fun findX(unitPosition: Vec2Double, targetPosition: Vec2Double, y: Double): Double{
        //x = (y-y1)*(x2-x1)/(y2-y1) + x1
        return (y - unitPosition.y)*(targetPosition.x - unitPosition.x)/(targetPosition.y - unitPosition.y) + unitPosition.x
    }


    companion object {
        internal fun distanceSqr(a: Vec2Double, b: Vec2Double): Double {
            return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)
        }
    }
}