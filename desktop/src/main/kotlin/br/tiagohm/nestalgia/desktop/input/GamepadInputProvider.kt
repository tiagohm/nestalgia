package br.tiagohm.nestalgia.desktop.input

import br.tiagohm.nestalgia.core.*
import br.tiagohm.nestalgia.desktop.input.GamepadInputAction.*
import com.studiohartman.jamepad.ControllerAxis
import com.studiohartman.jamepad.ControllerButton
import com.studiohartman.jamepad.ControllerManager
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

data class GamepadInputProvider(
    private val console: Console,
    private val listener: GamepadInputListener,
) : InputProvider, Closeable {

    private val jamepad = ControllerManager(ControlDevice.PORT_COUNT)
    private var jamepadThread: Thread? = null
    private val stop = AtomicBoolean(false)

    private val standardControllerButtonState =
        Array(ControlDevice.PORT_COUNT) { BooleanArray(STANDARD_CONTROLLER_BUTTONS.size) }

    init {
        jamepadThread = thread(true, isDaemon = true, name = "Jamepad") {
            jamepad.initSDLGamepad()

            while (!stop.get()) {
                jamepad.update()

                for (i in 0 until ControlDevice.PORT_COUNT) {
                    val controller = jamepad.getControllerIndex(i)

                    if (controller == null || !controller.isConnected) continue

                    val state = standardControllerButtonState[i]

                    for (b in JAMEPAD_BUTTONS) {
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
                        state[StandardControllerButton.RIGHT.bit] = true
                    } else if (lx < -0.7f) {
                        state[StandardControllerButton.LEFT.bit] = true
                    }

                    if (ly > 0.7f) {
                        state[StandardControllerButton.UP.bit] = true
                    } else if (ly < -0.7f) {
                        state[StandardControllerButton.DOWN.bit] = true
                    }
                }

                Thread.sleep(8)
            }
        }
    }

    override fun setInput(device: ControlDevice): Boolean {
        return if (device is StandardController) {
            for (button in STANDARD_CONTROLLER_BUTTONS) {
                if (standardControllerButtonState[device.port][button.bit]) {
                    device.setBit(button)
                } else {
                    device.clearBit(button)
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

        @JvmStatic private val JAMEPAD_BUTTONS = ControllerButton.values()
        @JvmStatic private val STANDARD_CONTROLLER_BUTTONS = StandardControllerButton.values()

        @JvmStatic private val MAP_JAMEPAD_TO_NES = mapOf(
            ControllerButton.A to StandardControllerButton.A,
            ControllerButton.B to StandardControllerButton.B,
            ControllerButton.DPAD_DOWN to StandardControllerButton.DOWN,
            ControllerButton.DPAD_UP to StandardControllerButton.UP,
            ControllerButton.DPAD_RIGHT to StandardControllerButton.RIGHT,
            ControllerButton.DPAD_LEFT to StandardControllerButton.LEFT,
            ControllerButton.BACK to StandardControllerButton.SELECT,
            ControllerButton.START to StandardControllerButton.START,
        )
    }
}
