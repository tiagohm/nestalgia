package br.tiagohm.nestalgia.ui

import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.JSpinner.DefaultEditor
import javax.swing.event.ListDataListener

val APP_ICON: BufferedImage? by lazy {
    Thread.currentThread().contextClassLoader.getResourceAsStream("logo.png")?.use {
        ImageIO.read(it)
    }
}

fun margin(
    top: Int = 0,
    left: Int = 0,
    bottom: Int = 0,
    right: Int = 0,
) = Insets(top, left, bottom, right)

fun margin(
    horizontal: Int = 0,
    vertical: Int = 0,
) = margin(vertical, horizontal, vertical, horizontal)

fun margin(all: Int) = margin(all, all)

val ZERO_MARGIN = margin(0)

enum class HorizontalAlignment(val value: Int) {
    LEFT(SwingConstants.LEFT),
    CENTER(SwingConstants.CENTER),
    RIGHT(SwingConstants.RIGHT),
    LEADING(SwingConstants.LEADING),
    TRAILING(SwingConstants.TRAILING),
}

enum class VerticalAlignment(val value: Int) {
    TOP(SwingConstants.TOP),
    CENTER(SwingConstants.CENTER),
    BOTTOM(SwingConstants.BOTTOM),
}

enum class Anchor(val value: Int) {
    CENTER(GridConstraints.ANCHOR_CENTER),
    NORTH(GridConstraints.ANCHOR_NORTH),
    NORTHEAST(GridConstraints.ANCHOR_NORTHEAST),
    EAST(GridConstraints.ANCHOR_EAST),
    SOUTHEAST(GridConstraints.ANCHOR_SOUTHEAST),
    SOUTH(GridConstraints.ANCHOR_SOUTH),
    SOUTH_WEST(GridConstraints.ANCHOR_SOUTHWEST),
    WEST(GridConstraints.ANCHOR_WEST),
    NORTH_WEST(GridConstraints.ANCHOR_NORTHWEST),
}

enum class Fill(val value: Int) {
    NONE(GridConstraints.FILL_NONE),
    HORIZONTAL(GridConstraints.FILL_HORIZONTAL),
    VERTICAL(GridConstraints.FILL_VERTICAL),
    BOTH(GridConstraints.FILL_BOTH),
}

data class SizePolicy(
    val canShrink: Boolean = true,
    val canGrow: Boolean = true,
    val wantGrow: Boolean = false,
) {
    val value = (if (canShrink) GridConstraints.SIZEPOLICY_CAN_SHRINK else 0) or
            (if (canGrow) GridConstraints.SIZEPOLICY_CAN_GROW else 0) or
            (if (wantGrow) GridConstraints.SIZEPOLICY_WANT_GROW else 0)

    companion object {
        val DEFAULT = SizePolicy()

        val FIXED = SizePolicy(canShrink = false, canGrow = false)
    }
}

data class Size(val width: Int = -1, val height: Int = -1) : Dimension(width, height) {
    companion object {
        val DEFAULT = Size()
    }
}

enum class CursorType(val value: Int) {
    DEFAULT(Cursor.DEFAULT_CURSOR),
    CROSS_HAIR(Cursor.CROSSHAIR_CURSOR),
    TEXT(Cursor.TEXT_CURSOR),
    WAIT(Cursor.WAIT_CURSOR),
    SW_RESIZE(Cursor.SW_RESIZE_CURSOR),
    SE_RESIZE(Cursor.SE_RESIZE_CURSOR),
    NW_RESIZE(Cursor.NW_RESIZE_CURSOR),
    NE_RESIZE(Cursor.NE_RESIZE_CURSOR),
    N_RESIZE(Cursor.N_RESIZE_CURSOR),
    S_RESIZE(Cursor.S_RESIZE_CURSOR),
    W_RESIZE(Cursor.W_RESIZE_CURSOR),
    E_RESIZE(Cursor.E_RESIZE_CURSOR),
    HAND(Cursor.HAND_CURSOR),
    MOVE(Cursor.MOVE_CURSOR),
}

// Components

fun panel(
    rowCount: Int,
    columnCount: Int,
    margin: Insets = ZERO_MARGIN,
    hGap: Int = -1,
    vGap: Int = -1,
    sameSizeHorizontally: Boolean = false,
    sameSizeVertically: Boolean = false,
    isDoubleBuffered: Boolean = true,
    isEnabled: Boolean = true,
    isOpaque: Boolean = false,
    init: JPanel.() -> Unit = {},
): JPanel {
    val lm = GridLayoutManager(rowCount, columnCount, margin, hGap, vGap, sameSizeHorizontally, sameSizeVertically)
    val panel = JPanel(lm, isDoubleBuffered)
    panel.isEnabled = isEnabled
    panel.isOpaque = isOpaque
    panel.init()
    return panel
}

fun JPanel.label(
    text: String,
    row: Int,
    column: Int,
    icon: Icon? = null,
    horizontalAlignment: HorizontalAlignment = HorizontalAlignment.CENTER,
    verticalAlignment: VerticalAlignment = VerticalAlignment.CENTER,
    rowSpan: Int = 1,
    colSpan: Int = 1,
    anchor: Anchor = Anchor.WEST,
    fill: Fill = Fill.NONE,
    horizontalSizePolicy: SizePolicy = SizePolicy.DEFAULT,
    verticalSizePolicy: SizePolicy = SizePolicy.DEFAULT,
    minimumSize: Size = Size.DEFAULT,
    preferredSize: Size = Size.DEFAULT,
    maximumSize: Size = Size.DEFAULT,
    indent: Int = 0,
    useParentLayout: Boolean = false,
    isEnabled: Boolean = true,
    background: Color? = null,
    isOpaque: Boolean = false,
    horizontalTextPosition: HorizontalAlignment = HorizontalAlignment.CENTER,
    verticalTextPosition: VerticalAlignment = VerticalAlignment.CENTER,
    iconTextGap: Int = 0,
    isAutoscroll: Boolean = false,
    font: Font? = null,
    foreground: Color? = null,
    toolTipText: String? = null,
    isVisible: Boolean = true,
    cursor: CursorType? = null,
    init: JLabel.() -> Unit = {},
): JLabel {
    val label = JLabel(text, icon, horizontalAlignment.value)

    label.verticalAlignment = verticalAlignment.value
    label.isVisible = isVisible
    label.isOpaque = isOpaque
    label.horizontalTextPosition = horizontalTextPosition.value
    label.verticalTextPosition = verticalTextPosition.value
    label.iconTextGap = iconTextGap
    label.autoscrolls = isAutoscroll
    label.isEnabled = isEnabled

    if (cursor != null) label.cursor = Cursor(cursor.value)
    if (background != null) label.background = background
    if (foreground != null) label.foreground = foreground
    if (font != null) label.font = font
    if (toolTipText != null) label.toolTipText = toolTipText

    val constraints = GridConstraints(
        row, column, rowSpan, colSpan, anchor.value, fill.value,
        horizontalSizePolicy.value, verticalSizePolicy.value,
        minimumSize, preferredSize, maximumSize, indent, useParentLayout
    )

    add(label, constraints)

    label.init()

    return label
}

fun JPanel.button(
    text: String,
    row: Int,
    column: Int,
    icon: Icon? = null,
    onClick: () -> Unit = {},
    horizontalAlignment: HorizontalAlignment = HorizontalAlignment.CENTER,
    verticalAlignment: VerticalAlignment = VerticalAlignment.CENTER,
    rowSpan: Int = 1,
    colSpan: Int = 1,
    anchor: Anchor = Anchor.WEST,
    fill: Fill = Fill.NONE,
    horizontalSizePolicy: SizePolicy = SizePolicy.DEFAULT,
    verticalSizePolicy: SizePolicy = SizePolicy.DEFAULT,
    minimumSize: Size = Size.DEFAULT,
    preferredSize: Size = Size.DEFAULT,
    maximumSize: Size = Size.DEFAULT,
    indent: Int = 0,
    useParentLayout: Boolean = false,
    isEnabled: Boolean = true,
    background: Color? = null,
    isOpaque: Boolean = false,
    horizontalTextPosition: HorizontalAlignment = HorizontalAlignment.CENTER,
    verticalTextPosition: VerticalAlignment = VerticalAlignment.CENTER,
    iconTextGap: Int = 0,
    isAutoscroll: Boolean = false,
    font: Font? = null,
    foreground: Color? = null,
    toolTipText: String? = null,
    isVisible: Boolean = true,
    cursor: CursorType? = CursorType.HAND,
    init: JButton.() -> Unit = {},
): JButton {
    val button = JButton(text, icon)

    button.horizontalAlignment = horizontalAlignment.value
    button.verticalAlignment = verticalAlignment.value
    button.isVisible = isVisible
    button.isOpaque = isOpaque
    button.horizontalTextPosition = horizontalTextPosition.value
    button.verticalTextPosition = verticalTextPosition.value
    button.iconTextGap = iconTextGap
    button.autoscrolls = isAutoscroll
    button.isEnabled = isEnabled

    if (cursor != null) button.cursor = Cursor(cursor.value)
    if (background != null) button.background = background
    if (foreground != null) button.foreground = foreground
    if (font != null) button.font = font
    if (toolTipText != null) button.toolTipText = toolTipText

    button.addActionListener { onClick() }

    val constraints = GridConstraints(
        row, column, rowSpan, colSpan, anchor.value, fill.value,
        horizontalSizePolicy.value, verticalSizePolicy.value,
        minimumSize, preferredSize, maximumSize, indent, useParentLayout
    )

    add(button, constraints)

    button.init()

    return button
}

fun <T> JPanel.dropdown(
    row: Int,
    column: Int,
    data: List<T>,
    value: T,
    onChanged: (T) -> Unit = {},
    renderer: ListCellRenderer<T>? = null,
    rowSpan: Int = 1,
    colSpan: Int = 1,
    anchor: Anchor = Anchor.WEST,
    fill: Fill = Fill.HORIZONTAL,
    horizontalSizePolicy: SizePolicy = SizePolicy.DEFAULT,
    verticalSizePolicy: SizePolicy = SizePolicy.DEFAULT,
    minimumSize: Size = Size.DEFAULT,
    preferredSize: Size = Size.DEFAULT,
    maximumSize: Size = Size.DEFAULT,
    indent: Int = 0,
    useParentLayout: Boolean = false,
    isEnabled: Boolean = true,
    background: Color? = null,
    isOpaque: Boolean = false,
    isAutoscroll: Boolean = false,
    font: Font? = null,
    foreground: Color? = null,
    toolTipText: String? = null,
    isVisible: Boolean = true,
    cursor: CursorType? = CursorType.HAND,
    init: JComboBox<T>.() -> Unit = {},
): JComboBox<T> {
    val model = DropdownModel(data, value)
    val dropdown = JComboBox(model)

    dropdown.isVisible = isVisible
    dropdown.isOpaque = isOpaque
    dropdown.autoscrolls = isAutoscroll
    dropdown.isEnabled = isEnabled

    if (cursor != null) dropdown.cursor = Cursor(cursor.value)
    if (background != null) dropdown.background = background
    if (foreground != null) dropdown.foreground = foreground
    if (font != null) dropdown.font = font
    if (toolTipText != null) dropdown.toolTipText = toolTipText
    if (renderer != null) dropdown.renderer = renderer

    dropdown.addActionListener { onChanged(model.value) }

    val constraints = GridConstraints(
        row, column, rowSpan, colSpan, anchor.value, fill.value,
        horizontalSizePolicy.value, verticalSizePolicy.value,
        minimumSize, preferredSize, maximumSize, indent, useParentLayout
    )

    add(dropdown, constraints)

    dropdown.init()

    return dropdown
}

@Suppress("UNCHECKED_CAST")
open class DropdownModel<T>(private val data: List<T>, var value: T) : ComboBoxModel<T> {

    override fun getSize() = data.size

    override fun getElementAt(index: Int) = data[index]

    override fun addListDataListener(listener: ListDataListener) {}

    override fun removeListDataListener(listener: ListDataListener) {}

    override fun setSelectedItem(item: Any) {
        value = item as T
    }

    override fun getSelectedItem(): Any = value as Any
}

fun JPanel.checkbox(
    row: Int,
    column: Int,
    isChecked: Boolean,
    text: String,
    icon: Icon? = null,
    onChanged: (Boolean) -> Unit = {},
    horizontalAlignment: HorizontalAlignment = HorizontalAlignment.LEFT,
    verticalAlignment: VerticalAlignment = VerticalAlignment.CENTER,
    rowSpan: Int = 1,
    colSpan: Int = 1,
    anchor: Anchor = Anchor.WEST,
    fill: Fill = Fill.NONE,
    horizontalSizePolicy: SizePolicy = SizePolicy.DEFAULT,
    verticalSizePolicy: SizePolicy = SizePolicy.DEFAULT,
    minimumSize: Size = Size.DEFAULT,
    preferredSize: Size = Size.DEFAULT,
    maximumSize: Size = Size.DEFAULT,
    indent: Int = 0,
    useParentLayout: Boolean = false,
    isEnabled: Boolean = true,
    background: Color? = null,
    isOpaque: Boolean = false,
    horizontalTextPosition: HorizontalAlignment = HorizontalAlignment.RIGHT,
    verticalTextPosition: VerticalAlignment = VerticalAlignment.CENTER,
    isAutoscroll: Boolean = false,
    font: Font? = null,
    foreground: Color? = null,
    toolTipText: String? = null,
    isVisible: Boolean = true,
    cursor: CursorType? = CursorType.HAND,
    init: JCheckBox.() -> Unit = {},
): JCheckBox {
    val checkbox = JCheckBox(text, icon, isChecked)

    checkbox.horizontalAlignment = horizontalAlignment.value
    checkbox.verticalAlignment = verticalAlignment.value
    checkbox.isVisible = isVisible
    checkbox.isOpaque = isOpaque
    checkbox.horizontalTextPosition = horizontalTextPosition.value
    checkbox.verticalTextPosition = verticalTextPosition.value
    checkbox.autoscrolls = isAutoscroll
    checkbox.isEnabled = isEnabled
    checkbox.iconTextGap = 8

    if (cursor != null) checkbox.cursor = Cursor(cursor.value)
    if (background != null) checkbox.background = background
    if (foreground != null) checkbox.foreground = foreground
    if (font != null) checkbox.font = font
    if (toolTipText != null) checkbox.toolTipText = toolTipText

    checkbox.addActionListener { onChanged(checkbox.isSelected) }

    val constraints = GridConstraints(
        row, column, rowSpan, colSpan, anchor.value, fill.value,
        horizontalSizePolicy.value, verticalSizePolicy.value,
        minimumSize, preferredSize, maximumSize, indent, useParentLayout
    )

    add(checkbox, constraints)

    checkbox.init()

    return checkbox
}

fun JPanel.spinner(
    row: Int,
    column: Int,
    model: SpinnerModel,
    rowSpan: Int = 1,
    colSpan: Int = 1,
    isEditable: Boolean = true,
    anchor: Anchor = Anchor.WEST,
    fill: Fill = Fill.NONE,
    horizontalSizePolicy: SizePolicy = SizePolicy.DEFAULT,
    verticalSizePolicy: SizePolicy = SizePolicy.DEFAULT,
    minimumSize: Size = Size.DEFAULT,
    preferredSize: Size = Size.DEFAULT,
    maximumSize: Size = Size.DEFAULT,
    indent: Int = 0,
    useParentLayout: Boolean = false,
    isEnabled: Boolean = true,
    background: Color? = null,
    isOpaque: Boolean = false,
    isAutoscroll: Boolean = false,
    font: Font? = null,
    foreground: Color? = null,
    toolTipText: String? = null,
    isVisible: Boolean = true,
    cursor: CursorType? = CursorType.HAND,
    init: JSpinner.() -> Unit = {},
): JSpinner {
    val spinner = JSpinner(model)

    spinner.isVisible = isVisible
    spinner.isOpaque = isOpaque
    spinner.autoscrolls = isAutoscroll
    spinner.isEnabled = isEnabled
    (spinner.editor as DefaultEditor).textField.isEditable = isEditable

    if (cursor != null) spinner.cursor = Cursor(cursor.value)
    if (background != null) spinner.background = background
    if (foreground != null) spinner.foreground = foreground
    if (font != null) spinner.font = font
    if (toolTipText != null) spinner.toolTipText = toolTipText

    val constraints = GridConstraints(
        row, column, rowSpan, colSpan, anchor.value, fill.value,
        horizontalSizePolicy.value, verticalSizePolicy.value,
        minimumSize, preferredSize, maximumSize, indent, useParentLayout
    )

    add(spinner, constraints)

    spinner.init()

    return spinner
}

@Suppress("UNCHECKED_CAST")
fun JPanel.spinnerNumber(
    row: Int,
    column: Int,
    value: Int,
    min: Int,
    max: Int,
    onChanged: (Int) -> Unit,
    stepSize: Int = 1,
    isEditable: Boolean = true,
    rowSpan: Int = 1,
    colSpan: Int = 1,
    anchor: Anchor = Anchor.WEST,
    fill: Fill = Fill.HORIZONTAL,
    horizontalSizePolicy: SizePolicy = SizePolicy.DEFAULT,
    verticalSizePolicy: SizePolicy = SizePolicy.DEFAULT,
    minimumSize: Size = Size.DEFAULT,
    preferredSize: Size = Size.DEFAULT,
    maximumSize: Size = Size.DEFAULT,
    indent: Int = 0,
    useParentLayout: Boolean = false,
    isEnabled: Boolean = true,
    background: Color? = null,
    isOpaque: Boolean = false,
    isAutoscroll: Boolean = false,
    font: Font? = null,
    foreground: Color? = null,
    toolTipText: String? = null,
    isVisible: Boolean = true,
    cursor: CursorType? = CursorType.HAND,
    init: JSpinner.() -> Unit = {},
): JSpinner {
    val model = SpinnerNumberModel(value, min, max, stepSize)
    model.addChangeListener { onChanged(model.value as Int) }

    return spinner(
        row, column, model,
        rowSpan, colSpan, isEditable, anchor, fill, horizontalSizePolicy,
        verticalSizePolicy, minimumSize, preferredSize,
        maximumSize, indent, useParentLayout, isEnabled,
        background, isOpaque, isAutoscroll, font,
        foreground, toolTipText, isVisible, cursor, init
    )
}

fun JPanel.scrollPane(
    row: Int,
    column: Int,
    rowSpan: Int = 1,
    colSpan: Int = 1,
    anchor: Anchor = Anchor.WEST,
    fill: Fill = Fill.HORIZONTAL,
    horizontalSizePolicy: SizePolicy = SizePolicy.DEFAULT,
    verticalSizePolicy: SizePolicy = SizePolicy.DEFAULT,
    minimumSize: Size = Size.DEFAULT,
    preferredSize: Size = Size.DEFAULT,
    maximumSize: Size = Size.DEFAULT,
    indent: Int = 0,
    useParentLayout: Boolean = false,
    isEnabled: Boolean = true,
    background: Color? = null,
    isOpaque: Boolean = false,
    isAutoscroll: Boolean = false,
    font: Font? = null,
    foreground: Color? = null,
    toolTipText: String? = null,
    isVisible: Boolean = true,
    cursor: CursorType? = CursorType.HAND,
    init: JPanel.() -> JComponent,
): JScrollPane {
    val scrollpane = JScrollPane(init())

    scrollpane.isVisible = isVisible
    scrollpane.isOpaque = isOpaque
    scrollpane.autoscrolls = isAutoscroll
    scrollpane.isEnabled = isEnabled

    if (cursor != null) scrollpane.cursor = Cursor(cursor.value)
    if (background != null) scrollpane.background = background
    if (foreground != null) scrollpane.foreground = foreground
    if (font != null) scrollpane.font = font
    if (toolTipText != null) scrollpane.toolTipText = toolTipText

    val constraints = GridConstraints(
        row, column, rowSpan, colSpan, anchor.value, fill.value,
        horizontalSizePolicy.value, verticalSizePolicy.value,
        minimumSize, preferredSize, maximumSize, indent, useParentLayout
    )

    add(scrollpane, constraints)

    return scrollpane
}

enum class SelectionMode(val value: Int) {
    SINGLE(ListSelectionModel.SINGLE_SELECTION),
    SINGLE_INTERVAL(ListSelectionModel.SINGLE_INTERVAL_SELECTION),
    MULTIPLE(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION),
}

fun <T> JPanel.checkboxList(
    row: Int,
    column: Int,
    items: List<T>,
    selectedItems: List<T> = emptyList(),
    onChanged: (Boolean, Int, T) -> Unit = { _, _, _ -> },
    rowSpan: Int = 1,
    colSpan: Int = 1,
    anchor: Anchor = Anchor.WEST,
    fill: Fill = Fill.NONE,
    horizontalSizePolicy: SizePolicy = SizePolicy.DEFAULT,
    verticalSizePolicy: SizePolicy = SizePolicy.DEFAULT,
    minimumSize: Size = Size.DEFAULT,
    preferredSize: Size = Size.DEFAULT,
    maximumSize: Size = Size.DEFAULT,
    indent: Int = 0,
    useParentLayout: Boolean = false,
    isEnabled: Boolean = true,
    background: Color? = null,
    isOpaque: Boolean = false,
    isAutoscroll: Boolean = true,
    font: Font? = null,
    foreground: Color? = null,
    toolTipText: String? = null,
    isVisible: Boolean = true,
    cursor: CursorType? = null,
    selectionMode: SelectionMode = SelectionMode.SINGLE,
    init: JList<CheckboxListItem<T>>.() -> Unit = {},
): JList<CheckboxListItem<T>> {
    val checkboxItems = items.map { CheckboxListItem(it, null, selectedItems.contains(it)) }.toTypedArray()
    val list = JList(checkboxItems)

    list.cellRenderer = CheckboxListRenderer()
    list.selectionMode = selectionMode.value

    list.addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            val index = list.locationToIndex(e.point)
            val item = items[index]
            val checkboxItem = checkboxItems[index]
            checkboxItem.isSelected = !checkboxItem.isSelected
            onChanged(checkboxItem.isSelected, index, item)
            list.repaint(list.getCellBounds(index, index))
        }
    })

    list.isVisible = isVisible
    list.isOpaque = isOpaque
    list.autoscrolls = isAutoscroll
    list.isEnabled = isEnabled

    if (cursor != null) list.cursor = Cursor(cursor.value)
    if (background != null) list.background = background
    if (foreground != null) list.foreground = foreground
    if (font != null) list.font = font
    if (toolTipText != null) list.toolTipText = toolTipText

    val constraints = GridConstraints(
        row, column, rowSpan, colSpan, anchor.value, fill.value,
        horizontalSizePolicy.value, verticalSizePolicy.value,
        minimumSize, preferredSize, maximumSize, indent, useParentLayout
    )

    add(list, constraints)

    list.init()

    return list
}

data class CheckboxListItem<T>(
    val value: T,
    val text: String? = null,
    var isSelected: Boolean = false,
)

class CheckboxListRenderer<T> : JPanel(), ListCellRenderer<CheckboxListItem<T>> {

    override fun getListCellRendererComponent(
        list: JList<out CheckboxListItem<T>>,
        value: CheckboxListItem<T>,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        return checkbox(
            0, 0,
            value.isSelected,
            value.text ?: value.value.toString(),
            fill = Fill.HORIZONTAL
        )
    }
}

// Menu

fun JFrame.menuBar(init: JMenuBar.() -> Unit) {
    val menuBar = JMenuBar()
    menuBar.init()
    jMenuBar = menuBar
}

fun JMenuBar.menu(caption: String, init: JMenu.() -> Unit = {}) {
    val menu = JMenu(caption)
    menu.init()
    add(menu)
}

fun JMenu.menuItem(
    caption: String,
    key: KeyStroke? = null,
    action: () -> Unit,
) {
    val menuItem = JMenuItem(caption)
    menuItem.addActionListener { action() }

    if (key != null) {
        menuItem.accelerator = key
    }

    add(menuItem)
}

fun JMenu.separator() {
    addSeparator()
}

fun JMenu.radioMenuItem(
    caption: String,
    isSelected: Boolean = false,
    action: () -> Unit,
) {
    val menuItem = JRadioButtonMenuItem(caption)
    menuItem.addActionListener {
        for (i in 0 until itemCount) {
            val item = getItem(i) as? JRadioButtonMenuItem
            item?.isSelected = item == menuItem
        }

        action()
    }
    menuItem.isSelected = isSelected
    add(menuItem)
}

fun JMenu.checkboxMenuItem(
    caption: String,
    isSelected: Boolean = false,
    action: (Boolean) -> Unit,
) {
    val menuItem = JCheckBoxMenuItem(caption)
    menuItem.addActionListener {
        action(menuItem.isSelected)
    }
    menuItem.isSelected = isSelected
    add(menuItem)
}

fun JMenu.menu(
    caption: String,
    init: JMenu.() -> Unit = {},
) {
    val menu = JMenu(caption)
    menu.init()
    add(menu)
}

fun key(
    code: Int,
    isCtrl: Boolean = false,
    isShift: Boolean = false,
    isAlt: Boolean = false,
): KeyStroke {
    val modifiers = (if (isCtrl) Event.CTRL_MASK else 0) or
            (if (isShift) Event.SHIFT_MASK else 0) or
            (if (isAlt) Event.ALT_MASK else 0)

    return KeyStroke.getKeyStroke(code, modifiers)
}

fun ctrlShift(code: Int) = key(code, isCtrl = true, isShift = true)

fun ctrl(code: Int) = key(code, isCtrl = true)

fun shift(code: Int) = key(code, isShift = true)

fun alt(code: Int) = key(code, isAlt = true)

