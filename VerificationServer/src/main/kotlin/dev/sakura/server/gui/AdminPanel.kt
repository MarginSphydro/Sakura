package dev.sakura.server.gui

import dev.sakura.server.data.UserDatabase
import java.awt.*
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import kotlin.concurrent.fixedRateTimer

class AdminPanel : JFrame("Sakura Server 管理面板") {

    private val usersTableModel = object : DefaultTableModel(arrayOf("用户名", "密码", "HWID", "用户组"), 0) {
        override fun isCellEditable(row: Int, column: Int) = false
    }

    private val groupNames = mapOf(
        1 to "普通用户",
        2 to "高级用户",
        3 to "VIP",
        4 to "管理员"
    )
    private val pendingTableModel = object : DefaultTableModel(arrayOf("用户名", "密码", "HWID", "注册时间"), 0) {
        override fun isCellEditable(row: Int, column: Int) = false
    }

    private val usersTable = JTable(usersTableModel)
    private val pendingTable = JTable(pendingTableModel)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        size = Dimension(900, 600)
        setLocationRelativeTo(null)

        layout = BorderLayout(10, 10)

        // Title
        val titleLabel = JLabel("Sakura Verification Server 管理面板", SwingConstants.CENTER)
        titleLabel.font = Font("Microsoft YaHei", Font.BOLD, 20)
        titleLabel.border = BorderFactory.createEmptyBorder(15, 0, 10, 0)
        add(titleLabel, BorderLayout.NORTH)

        // Main content - split pane
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
        splitPane.dividerLocation = 450
        splitPane.resizeWeight = 0.5

        // Left panel - Registered users
        splitPane.leftComponent = createUsersPanel()

        // Right panel - Pending users
        splitPane.rightComponent = createPendingPanel()

        add(splitPane, BorderLayout.CENTER)

        // Status bar
        val statusBar = JLabel("就绪")
        statusBar.border = BorderFactory.createEmptyBorder(5, 10, 5, 10)
        add(statusBar, BorderLayout.SOUTH)

        refreshData()

        // Auto refresh every 3 seconds
        fixedRateTimer("AutoRefresh", daemon = true, period = 3000L) {
            SwingUtilities.invokeLater { refreshData() }
        }
    }

    private fun createUsersPanel(): JPanel {
        val panel = JPanel(BorderLayout(5, 5))
        panel.border = BorderFactory.createTitledBorder("已注册用户")

        // Table
        usersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        usersTable.columnModel.getColumn(2).preferredWidth = 200
        val scrollPane = JScrollPane(usersTable)
        panel.add(scrollPane, BorderLayout.CENTER)

        // Buttons
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 5))

        val setGroupBtn = JButton("设置用户组")
        setGroupBtn.addActionListener {
            val row = usersTable.selectedRow
            if (row >= 0) {
                val username = usersTableModel.getValueAt(row, 0) as String
                val options = arrayOf("普通用户 (1)", "高级用户 (2)", "VIP (3)", "管理员 (4)")
                val choice = JOptionPane.showInputDialog(
                    this,
                    "选择用户 '$username' 的用户组：",
                    "设置用户组",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
                )
                if (choice != null) {
                    val group = options.indexOf(choice) + 1
                    if (UserDatabase.setUserGroup(username, group)) {
                        showMessage("已设置 '$username' 的用户组为 ${groupNames[group]}")
                        refreshData()
                    }
                }
            } else {
                showMessage("请先选择一个用户", true)
            }
        }
        buttonPanel.add(setGroupBtn)

        val resetHwidBtn = JButton("重置 HWID")
        resetHwidBtn.addActionListener {
            val row = usersTable.selectedRow
            if (row >= 0) {
                val username = usersTableModel.getValueAt(row, 0) as String
                val confirm = JOptionPane.showConfirmDialog(
                    this,
                    "确定要重置用户 '$username' 的 HWID 吗？",
                    "确认",
                    JOptionPane.YES_NO_OPTION
                )
                if (confirm == JOptionPane.YES_OPTION) {
                    if (UserDatabase.resetUserHwid(username)) {
                        showMessage("已重置 '$username' 的 HWID")
                        refreshData()
                    }
                }
            } else {
                showMessage("请先选择一个用户", true)
            }
        }
        buttonPanel.add(resetHwidBtn)

        val deleteUserBtn = JButton("删除用户")
        deleteUserBtn.addActionListener {
            val row = usersTable.selectedRow
            if (row >= 0) {
                val username = usersTableModel.getValueAt(row, 0) as String
                val confirm = JOptionPane.showConfirmDialog(
                    this,
                    "确定要删除用户 '$username' 吗？此操作不可撤销！",
                    "确认删除",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                )
                if (confirm == JOptionPane.YES_OPTION) {
                    if (UserDatabase.deleteUser(username)) {
                        showMessage("已删除用户 '$username'")
                        refreshData()
                    }
                }
            } else {
                showMessage("请先选择一个用户", true)
            }
        }
        buttonPanel.add(deleteUserBtn)

        val refreshBtn = JButton("刷新")
        refreshBtn.addActionListener { refreshData() }
        buttonPanel.add(refreshBtn)

        panel.add(buttonPanel, BorderLayout.SOUTH)

        return panel
    }

    private fun createPendingPanel(): JPanel {
        val panel = JPanel(BorderLayout(5, 5))
        panel.border = BorderFactory.createTitledBorder("待审核用户")

        // Table
        pendingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        val scrollPane = JScrollPane(pendingTable)
        panel.add(scrollPane, BorderLayout.CENTER)

        // Buttons
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 5))

        val approveBtn = JButton("批准选中")
        approveBtn.addActionListener {
            val row = pendingTable.selectedRow
            if (row >= 0) {
                val username = pendingTableModel.getValueAt(row, 0) as String
                if (UserDatabase.approvePendingUser(username)) {
                    showMessage("已批准用户 '$username'")
                    refreshData()
                }
            } else {
                showMessage("请先选择一个用户", true)
            }
        }
        buttonPanel.add(approveBtn)

        val approveAllBtn = JButton("批准全部")
        approveAllBtn.background = Color(76, 175, 80)
        approveAllBtn.addActionListener {
            val count = UserDatabase.getPendingUsers().size
            if (count == 0) {
                showMessage("没有待审核的用户", true)
                return@addActionListener
            }
            val confirm = JOptionPane.showConfirmDialog(
                this,
                "确定要批准全部 $count 个待审核用户吗？",
                "确认",
                JOptionPane.YES_NO_OPTION
            )
            if (confirm == JOptionPane.YES_OPTION) {
                val approved = UserDatabase.approveAllPending()
                showMessage("已批准 $approved 个用户")
                refreshData()
            }
        }
        buttonPanel.add(approveAllBtn)

        val rejectBtn = JButton("拒绝选中")
        rejectBtn.addActionListener {
            val row = pendingTable.selectedRow
            if (row >= 0) {
                val username = pendingTableModel.getValueAt(row, 0) as String
                if (UserDatabase.deletePendingUser(username)) {
                    showMessage("已拒绝用户 '$username'")
                    refreshData()
                }
            } else {
                showMessage("请先选择一个用户", true)
            }
        }
        buttonPanel.add(rejectBtn)

        val clearAllBtn = JButton("清除全部")
        clearAllBtn.background = Color(244, 67, 54)
        clearAllBtn.addActionListener {
            val count = UserDatabase.getPendingUsers().size
            if (count == 0) {
                showMessage("没有待审核的用户", true)
                return@addActionListener
            }
            val confirm = JOptionPane.showConfirmDialog(
                this,
                "确定要清除全部 $count 个待审核用户吗？此操作不可撤销！",
                "确认清除",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            )
            if (confirm == JOptionPane.YES_OPTION) {
                val cleared = UserDatabase.clearAllPending()
                showMessage("已清除 $cleared 个待审核用户")
                refreshData()
            }
        }
        buttonPanel.add(clearAllBtn)

        panel.add(buttonPanel, BorderLayout.SOUTH)

        return panel
    }

    private fun refreshData() {
        UserDatabase.load()

        // Refresh users table
        usersTableModel.rowCount = 0
        UserDatabase.getUsers().forEach { user ->
            val hwidShort = if (user.hwid.length > 16) user.hwid.substring(0, 16) + "..." else user.hwid
            val groupName = groupNames[user.group] ?: "普通用户"
            usersTableModel.addRow(arrayOf(user.username, user.password, hwidShort, groupName))
        }

        // Refresh pending table
        pendingTableModel.rowCount = 0
        UserDatabase.getPendingUsers().forEach { pending ->
            val hwidShort = if (pending.hwid.length > 12) pending.hwid.substring(0, 12) + "..." else pending.hwid
            val time = dateFormat.format(Date(pending.timestamp))
            pendingTableModel.addRow(arrayOf(pending.username, pending.password, hwidShort, time))
        }
    }

    private fun showMessage(message: String, isWarning: Boolean = false) {
        JOptionPane.showMessageDialog(
            this,
            message,
            if (isWarning) "提示" else "成功",
            if (isWarning) JOptionPane.WARNING_MESSAGE else JOptionPane.INFORMATION_MESSAGE
        )
    }
}
