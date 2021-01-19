package br.tiagohm.nestalgia.ui

import br.tiagohm.nestalgia.core.*
import com.studiohartman.jamepad.ControllerAxis
import com.studiohartman.jamepad.ControllerButton
import com.studiohartman.jamepad.ControllerManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

@ExperimentalUnsignedTypes
class GamepadInputProvider(
    private val console: Console,
    private val onAction: (Action) -> Unit,
) : InputProvider,
    Disposable {

    enum class Action {
        X,
        Y,
        LB,
        RB,
    }

    private val jamepad = ControllerManager(ControlDevice.PORT_COUNT)
    private var jamepadThread: Thread? = null
    private val stop = AtomicBoolean(false)

    // Map each gamepad to NES controller port
    private val jamepadInputPort = IntArray(ControlDevice.PORT_COUNT) { i -> i }

    private val standardControllerButtonState =
        Array(ControlDevice.PORT_COUNT) { BooleanArray(STANDARD_CONTROLLER_BUTTONS.size) }

    init {
        jamepadThread = thread(true, name = "Jamepad") {
            jamepad.initSDLGamepad()

            while (!stop.get()) {
                jamepad.update()

                for (i in 0 until ControlDevice.PORT_COUNT) {
                    val controller = jamepad.getControllerIndex(i)

                    if (controller == null || !controller.isConnected) continue

                    val state = standardControllerButtonState[jamepadInputPort[i]]

                    for (b in JAMEPAD_BUTTONS) {
                        // Y
                        if (b == ControllerButton.Y) {
                            if (controller.isButtonJustPressed(b)) {
                                onAction(Action.Y)
                            }
                        }
                        // X
                        else if (b == ControllerButton.X) {
                            if (controller.isButtonJustPressed(b)) {
                                onAction(Action.X)
                            }
                        }
                        // LB & RB
                        else if (b == ControllerButton.LEFTBUMPER) {
                            if (controller.isButtonJustPressed(b)) {
                                onAction(Action.LB)
                            }
                        } else if (b == ControllerButton.RIGHTBUMPER) {
                            if (controller.isButtonJustPressed(b)) {
                                onAction(Action.RB)
                            }
                        }
                        // A, B, D-Pad Buttons
                        else if (MAP_JAMEPAD_TO_NES.containsKey(b)) {
                            val isPressed = controller.isButtonPressed(b)
                            val button = MAP_JAMEPAD_TO_NES[b]!!
                            state[button.bit] = isPressed
                        }
                    }

                    val lx = controller.getAxisState(ControllerAxis.LEFTX)
                    val ly = controller.getAxisState(ControllerAxis.LEFTY)

                    if (lx > 0.7f) {
                        state[StandardController.Buttons.RIGHT.bit] = true
                    } else if (lx < -0.7f) {
                        state[StandardController.Buttons.LEFT.bit] = true
                    }

                    if (ly > 0.7f) {
                        state[StandardController.Buttons.UP.bit] = true
                    } else if (ly < -0.7f) {
                        state[StandardController.Buttons.DOWN.bit] = true
                    }
                }

                try {
                    Thread.sleep(8)
                } catch (e: Exception) {
                    // nada
                }
            }
        }
    }

    override fun setInput(device: ControlDevice): Boolean {
        return if (device is StandardController) {
            for (button in STANDARD_CONTROLLER_BUTTONS) {
                if (standardControllerButtonState[device.port][button.bit]) {
                    device.buttonDown(button)
                } else {
                    device.buttonUp(button)
                }
            }
            true
        } else {
            false
        }
    }

    override fun dispose() {
        stop.set(true)
        jamepadThread?.interrupt()
        jamepadThread = null
        jamepad.quitSDLGamepad()
    }

    companion object {
        private val JAMEPAD_BUTTONS = ControllerButton.values()
        private val STANDARD_CONTROLLER_BUTTONS = StandardController.Buttons.values()
        private val MAP_JAMEPAD_TO_NES = mapOf(
            ControllerButton.A to StandardController.Buttons.A,
            ControllerButton.B to StandardController.Buttons.B,
            ControllerButton.DPAD_DOWN to StandardController.Buttons.DOWN,
            ControllerButton.DPAD_UP to StandardController.Buttons.UP,
            ControllerButton.DPAD_RIGHT to StandardController.Buttons.RIGHT,
            ControllerButton.DPAD_LEFT to StandardController.Buttons.LEFT,
            ControllerButton.BACK to StandardController.Buttons.SELECT,
            ControllerButton.START to StandardController.Buttons.START,
        )
    }
}