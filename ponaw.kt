private fun ponaw(unit: Unit, game: Game, debug: Debug): Vec2Double{
    if (game.bullets.isEmpty()){
        return Vec2Double(unit.position.x, unit.position.y)
    }

    var topleft: Vec2Double = Vec2Double(unit.position.x - (unit.size.x / 2), unit.position.y + unit.size.y)
    var topright: Vec2Double = Vec2Double(unit.position.x + (unit.size.x / 2), unit.position.y + unit.size.y)
    var bottomleft: Vec2Double = Vec2Double(unit.position.x - (unit.size.x / 2), unit.position.y)
    var bottomright: Vec2Double = Vec2Double(unit.position.x + (unit.size.x / 2), unit.position.y)

    var lambda = game.bullets[0].velocity.y / game.bullets[0].velocity.x
    var beta = game.bullets[0].position.y - lambda * game.bullets[0].position.x
    // y = lambda * x + beta
    // x = x0, x0 = unit.position.x
    // CHECK LEFT
    var y1 = findY_from_line(lambda, beta, unit.position.x - (unit.size.x / 2))
    debug.draw(CustomData.Log("Y: ${y1}, topleft.y: ${topleft.y}, bottomleft.y: ${bottomleft.y}"))

    var where_to_go= Vec2Double((-game.bullets[0].velocity.y / 60) + unit.position.x, (game.bullets[0].velocity.x) + unit.position.y)

    if (y1 < topleft.y && y1 > bottomleft.y){
        // When the bullet is about to hit, move!
        debug.draw(CustomData.Line(Vec2Float(unit.position.x.toFloat(), unit.position.y.toFloat()), Vec2Float(where_to_go.x.toFloat(), where_to_go.y.toFloat()), width = 0.5F, color = ColorFloat(100.0F, 0.0F, 0.0F, 0.0F)))
        return where_to_go
    }

    var y2 = findY_from_line(lambda, beta,unit.position.x + (unit.size.x / 2))
    if (y2 < topright.y && y2 > bottomright.y){
        return where_to_go
    }

    var x1 = findX_from_line(lambda, beta, unit.position.y + unit.size.y)
    if (x1 > topleft.x && x1 < topright.x){
        return where_to_go
    }

    var x2 = findX_from_line(lambda, beta, unit.position.y)
    if (x2 > bottomleft.x && x2 < bottomright.x){
        return where_to_go
    }

    return Vec2Double(unit.position.x, unit.position.y)
}


private fun findY_from_line(lambda: Double, beta: Double, x: Double): Double{
        return(lambda * x + beta)
    }
    private fun findX_from_line(lambda: Double, beta: Double, y: Double): Double{
        return ((y - beta) / lambda)
    }


