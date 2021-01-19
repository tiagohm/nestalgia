package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
class VsSystemActionManager(console: Console) : SystemActionManager(console) {

    private val needInsertCoin = IntArray(4)

    fun insertCoin(port: Int) {
        if (port < 4) {
            console.pause()
            needInsertCoin[port]
            console.resume()
        }
    }
}