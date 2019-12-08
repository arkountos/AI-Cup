import model.*
//import dont_shoot_the_wall

class MyStrategy {

    /* AvoidMine:
    Return null if there is no need to avoid any mine.
    Else, return a DoubleArray containing the following:
    avoidMine[0] = jump = 1.0 in order to jump, and = 0.0 otherwise
    avoidMine[1] = new velocity
    avoidMine[2] = new targetPos.x
    avoidMine[3] = new targetPos.ydd */
    private fun avoidMine(unit: model.Unit, game: Game, targetPos: Vec2Double): DoubleArray? {
        var jump: Double = 0.0;
        var velocity = targetPos.x - unit.position.x;

        var nearestPlantedMine: Mine? = null
        for (mine in game.mines) {
            if (mine is Mine) {
                if (nearestPlantedMine == null || distanceSqr(unit.position, mine.position) < distanceSqr(unit.position, nearestPlantedMine.position)) {
                    nearestPlantedMine = mine
                }
            }
        }

        if (nearestPlantedMine != null && nearestPlantedMine.state == model.MineState.TRIGGERED && distanceSqr(unit.position, nearestPlantedMine.position) < 10*nearestPlantedMine.explosionParams.radius) {
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
        print("Is that a pistol? ${action.swapWeapon}")
        if (action.swapWeapon == true && nearestWeapon != null){
            targetPos = nearestWeapon.position
        }

        debug.draw(CustomData.Log("Unit pos: ${unit.position.x}, ${unit.position.y}"))
        debug.draw(CustomData.Log("Tile at unit pos: ${game.level.tiles[unit.position.x.toInt() - 1][unit.position.y.toInt()]}"))
        debug.draw(CustomData.Log("targetPos.x - unit.position.x: ${targetPos.x} - , ${unit.position.x} = ${targetPos.x - unit.position.x}"))
        var aim = Vec2Double(0.0, 0.0)
        if (nearestEnemy != null) {
            aim = Vec2Double(nearestEnemy.position.x - unit.position.x,
                    nearestEnemy.position.y - unit.position.y)
        }
        
        var jump = targetPos.y > unit.position.y;
        var velocity = targetPos.x - unit.position.x;

        //Avoid any planted mines (that need to be avoided):
        var avoidMineResults: DoubleArray? = avoidMine(unit, game, targetPos);
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
        val avoid_array = listOf(avoidMine(unit, game, targetPos))
        
        debug.draw(CustomData.Log("Target pos: ${targetPos.x}, ${targetPos.y}"))
        action.velocity = if (velocity < 0) -game.properties.unitMaxHorizontalSpeed else game.properties.unitMaxHorizontalSpeed
        debug.draw(CustomData.Log("game.properties.... = ${action.velocity}"))
        action.jump = jump
        action.jumpDown = !jump
        action.aim = aim
        action.shoot = dont_shoot_the_wall(unit, nearestEnemy, game)
        action.reload = false
        action.plantMine = false
        return action
    }

    companion object {
        internal fun distanceSqr(a: Vec2Double, b: Vec2Double): Double {
            return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)
        }
    }
}
