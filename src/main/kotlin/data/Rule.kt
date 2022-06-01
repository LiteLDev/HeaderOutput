package data

data class Rule(
    var exclusion: Exclusion
) {
    data class Exclusion(
        var generation: Generation, var inclusion: Inclusion
    ) {
        data class Generation(var regex: MutableList<String>)
        data class Inclusion(var regex: MutableList<String>)
    }
}
