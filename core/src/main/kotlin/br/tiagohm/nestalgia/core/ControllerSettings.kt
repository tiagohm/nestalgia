package br.tiagohm.nestalgia.core

data class ControllerSettings(
    @JvmField val keyMapping: KeyMapping = KeyMapping(),
    @JvmField var type: ControllerType = ControllerType.NONE,
) : Snapshotable {

    override fun saveState(s: Snapshot) {
        s.write("keyMapping", keyMapping)
        s.write("type", type)
    }

    override fun restoreState(s: Snapshot) {
        s.readSnapshotable("keyMapping", keyMapping, keyMapping::reset)
        type = s.readEnum("type", ControllerType.NONE)
    }
}
