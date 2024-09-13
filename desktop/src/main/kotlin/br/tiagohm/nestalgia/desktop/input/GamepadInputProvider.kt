package br.tiagohm.nestalgia.desktop.input

import br.tiagohm.nestalgia.core.Console
import br.tiagohm.nestalgia.core.ControlDevice
import br.tiagohm.nestalgia.core.ControlDevice.Companion.PORT_COUNT
import br.tiagohm.nestalgia.core.InputProvider
import br.tiagohm.nestalgia.core.StandardController
import br.tiagohm.nestalgia.desktop.input.GamepadInputAction.*
import com.studiohartman.jamepad.ControllerAxis
import com.studiohartman.jamepad.ControllerButton
import com.studiohartman.jamepad.ControllerManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

data class GamepadInputProvider(
    private val console: Console,
    private val listener: GamepadInputListener,
) : InputProvider, AutoCloseable {

    private val jamepad = ControllerManager(PORT_COUNT)
    private var jamepadThread: Thread? = null
    private val stop = AtomicBoolean(false)

    private val standardControllerButtonState =
        Array(PORT_COUNT) { BooleanArray(StandardController.Button.entries.size) }

    init {
        jamepadThread = thread(true, isDaemon = true, name = "Jamepad") {
            jamepad.initSDLGamepad()

            while (!stop.get()) {
                jamepad.update()

                repeat(PORT_COUNT) {
                    val controller = jamepad.getControllerIndex(it)

                    if (controller == null || !controller.isConnected) return@repeat

                    val state = standardControllerButtonState[it]

                    for (b in ControllerButton.entries) {
                        // Y
                        if (b == ControllerButton.Y) {
                            if (controller.isButtonJustPressed(b)) {
                                listener.onAction(Y)
                            }
                        }
                        // X
                        else if (b == ControllerButton.X) {
                            if (controller.isButtonJustPressed(b)) {
                                listener.onAction(X)
                            }
                        }
                        // LB & RB
                        else if (b == ControllerButton.LEFTBUMPER) {
                            if (controller.isButtonJustPressed(b)) {
                                listener.onAction(LB)
                            }
                        } else if (b == ControllerButton.RIGHTBUMPER) {
                            if (controller.isButtonJustPressed(b)) {
                                listener.onAction(RB)
                            }
                        }
                        // A, B, D-Pad Buttons
                        else if (b in MAP_JAMEPAD_TO_NES) {
                            val pressed = controller.isButtonPressed(b)
                            val button = MAP_JAMEPAD_TO_NES[b]!!
                            state[button.bit] = pressed
                        }
                    }

                    val lx = controller.getAxisState(ControllerAxis.LEFTX)
                    val ly = controller.getAxisState(ControllerAxis.LEFTY)

                    if (lx > 0.7f) {
                        state[StandardController.Button.RIGHT.bit] = true
                    } else if (lx < -0.7f) {
                        state[StandardController.Button.LEFT.bit] = true
                    }

                    if (ly > 0.7f) {
                        state[StandardController.Button.UP.bit] = true
                    } else if (ly < -0.7f) {
                        state[StandardController.Button.DOWN.bit] = true
                    }
                }

                Thread.sleep(8)
            }
        }
    }

    override fun setInput(device: ControlDevice): Boolean {
        return if (device is StandardController) {
            for (button in StandardController.Button.entries) {
                if (standardControllerButtonState[device.port][button.bit]) {
                    device.setBit(button)
                }
            }
            true
        } else {
            false
        }
    }

    override fun close() {
        stop.set(true)
        jamepadThread?.interrupt()
        jamepadThread = null
        jamepad.quitSDLGamepad()
    }

    companion object {

        private val MAP_JAMEPAD_TO_NES = mapOf(
            ControllerButton.A to StandardController.Button.A,
            ControllerButton.B to StandardController.Button.B,
            ControllerButton.DPAD_DOWN to StandardController.Button.DOWN,
            ControllerButton.DPAD_UP to StandardController.Button.UP,
            ControllerButton.DPAD_RIGHT to StandardController.Button.RIGHT,
            ControllerButton.DPAD_LEFT to StandardController.Button.LEFT,
            ControllerButton.BACK to StandardController.Button.SELECT,
            ControllerButton.START to StandardController.Button.START,
        )
    }
}
