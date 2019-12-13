import model.*

fun omg_is_that_a_pistol(unit: model.Unit): Boolean{
    // If the weapon i hold is a pistol
    if (unit.weapon?.typ == WeaponType.PISTOL) {
        return(true)
    }
    return(false)
}