import model.*
//import dont_shoot_the_wall

class MyStrategy {

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
        debug.draw(CustomData.Log("Unit pos: ${unit.position.x}, ${unit.position.y}"))
        debug.draw(CustomData.Log("Tile at unit pos: ${game.level.tiles[unit.position.x.toInt() - 1][unit.position.y.toInt()]}"))
        debug.draw(CustomData.Log("Target pos: ${targetPos.x}, ${targetPos.y}"))
        debug.draw(CustomData.Log("targetPos.x - unit.position.x: ${targetPos.x} - , ${unit.position.x} = ${targetPos.x - unit.position.x}"))
        var aim = Vec2Double(0.0, 0.0)
        if (nearestEnemy != null) {
            aim = Vec2Double(nearestEnemy.position.x - unit.position.x,
                    nearestEnemy.position.y - unit.position.y)
        }
        var jump = targetPos.y > unit.position.y;
        if (targetPos.x > unit.position.x && game.level.tiles[(unit.position.x + 1).toInt()][(unit.position.y).toInt()] == Tile.WALL) {
            jump = true
        }
        if (targetPos.x < unit.position.x && game.level.tiles[(unit.position.x - 1).toInt()][(unit.position.y).toInt()] == Tile.WALL) {
            jump = true
        }
        val action = UnitAction()
        action.velocity = if (targetPos.x - unit.position.x < 0) -game.properties.unitMaxHorizontalSpeed else game.properties.unitMaxHorizontalSpeed
        debug.draw(CustomData.Log("game.properties.... = ${action.velocity}"))
        action.jump = jump
        action.jumpDown = !jump
        action.aim = aim
        action.shoot = dont_shoot_the_wall(unit, nearestEnemy, game)
        action.reload = false
        action.swapWeapon = false
        action.plantMine = false
        return action
    }

    companion object {
        internal fun distanceSqr(a: Vec2Double, b: Vec2Double): Double {
            return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)
        }
    }
}
