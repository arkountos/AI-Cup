    private fun ponaw(unit: Unit, game: Game, debug: Debug): Boolean{
        if (game.bullets.isEmpty()){
            return false
        }
//        if (game.bullets[0].unitId != unit.id){
//            return false
//        }
        var topleft: Vec2Double = Vec2Double(unit.position.x - (unit.size.x / 2), unit.position.y + unit.size.y)
        var topright: Vec2Double = Vec2Double(unit.position.x + (unit.size.x / 2), unit.position.y + unit.size.y)
        var bottomleft: Vec2Double = Vec2Double(unit.position.x - (unit.size.x / 2), unit.position.y)
        var bottomright: Vec2Double? = Vec2Double(unit.position.x + (unit.size.x / 2), unit.position.y)


d        var lambda = game.bullets[0].velocity.y / game.bullets[0].velocity.x
        var beta = game.bullets[0].position.y - lambda * game.bullets[0].position.x
        // y = lambda * x + beta
        // x = x0, x0 = unit.position.x
        // CHECK LEFT
        var y = findY_from_line(lambda, beta, unit.position.x - (unit.size.x / 2))
        debug.draw(CustomData.Log("Y: ${y}, topleft.y: ${topleft.y}, bottomleft.y: ${bottomleft.y}"))

        if (y < topleft.y && y > bottomleft.y){
            print("HIT!")
            return true
        }

        return false
    }

