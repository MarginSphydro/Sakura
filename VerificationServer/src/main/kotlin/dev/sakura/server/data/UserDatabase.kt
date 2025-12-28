package dev.sakura.server.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Serializable
data class User(
    val username: String,
    val password: String,
    val hwid: String,
    val group: Int = 1  // 用户组等级：1=普通, 2=高级, 3=VIP, 4=管理员
)

@Serializable
data class PendingUser(
    val username: String,
    val password: String,
    val hwid: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class UserData(
    val users: MutableList<User> = mutableListOf()
)

@Serializable
data class PendingData(
    val pending: MutableList<PendingUser> = mutableListOf()
)

@Serializable
data class GroupRestrictions(
    // Key: 最低所需等级, Value: 需要该等级才能加载的类名列表
    val restrictions: MutableMap<Int, MutableList<String>> = mutableMapOf(
        2 to mutableListOf("dev.sakura.module.impl.hud.NotificationHud"),  // 等级2才能用
        3 to mutableListOf(),  // 等级3(VIP)才能用
        4 to mutableListOf()   // 等级4(管理员)才能用
    )
)

object UserDatabase {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val usersFile = File("users.json")
    private val pendingFile = File("pending.json")
    private val restrictionsFile = File("restrictions.json")

    private var userData = UserData()
    private var pendingData = PendingData()
    private var groupRestrictions = GroupRestrictions()

    fun load() {
        if (usersFile.exists()) {
            runCatching {
                userData = json.decodeFromString<UserData>(usersFile.readText())
                println("[Database] Loaded ${userData.users.size} users")
            }.onFailure {
                println("[Database] Failed to load users: ${it.message}")
            }
        } else {
            save()
            println("[Database] Created new users.json")
        }

        if (pendingFile.exists()) {
            runCatching {
                pendingData = json.decodeFromString<PendingData>(pendingFile.readText())
                println("[Database] Loaded ${pendingData.pending.size} pending registrations")
            }.onFailure {
                println("[Database] Failed to load pending: ${it.message}")
            }
        } else {
            savePending()
            println("[Database] Created new pending.json")
        }

        if (restrictionsFile.exists()) {
            runCatching {
                groupRestrictions = json.decodeFromString<GroupRestrictions>(restrictionsFile.readText())
                println("[Database] Loaded group restrictions config")
            }.onFailure {
                println("[Database] Failed to load restrictions: ${it.message}")
            }
        } else {
            saveRestrictions()
            println("[Database] Created new restrictions.json")
        }

        //MappingsLoader.load()
    }

    fun save() {
        usersFile.writeText(json.encodeToString(userData))
    }

    fun savePending() {
        pendingFile.writeText(json.encodeToString(pendingData))
    }

    fun saveRestrictions() {
        restrictionsFile.writeText(json.encodeToString(groupRestrictions))
    }

    fun verifyUser(username: String, password: String, hwid: String): VerifyResult {
        // Hot reload users before verification
        load()

        val user = userData.users.find { it.username == username }

        if (user == null) {
            return VerifyResult.USER_NOT_FOUND
        }

        if (user.password != password) {
            return VerifyResult.WRONG_PASSWORD
        }

        // 如果用户的 HWID 为空（已被重置），则绑定新的 HWID
        if (user.hwid.isEmpty()) {
            val index = userData.users.indexOf(user)
            userData.users[index] = user.copy(hwid = hwid)
            save()
            println("[Login] Bound new HWID for user: $username")
            return VerifyResult.SUCCESS
        }

        if (user.hwid != hwid) {
            return VerifyResult.HWID_MISMATCH
        }

        return VerifyResult.SUCCESS
    }

    fun registerUser(username: String, password: String, hwid: String): RegisterResult {
        if (userData.users.any { it.username == username }) {
            return RegisterResult.USERNAME_EXISTS
        }

        if (pendingData.pending.any { it.username == username }) {
            return RegisterResult.PENDING_EXISTS
        }

        pendingData.pending.add(PendingUser(username, password, hwid))
        savePending()

        println("[Register] New pending registration: $username (HWID: $hwid)")
        return RegisterResult.SUCCESS
    }

    fun usernameExists(username: String): Boolean {
        return userData.users.any { it.username == username } ||
                pendingData.pending.any { it.username == username }
    }

    // Admin functions
    fun getUsers(): List<User> = userData.users.toList()

    fun getPendingUsers(): List<PendingUser> = pendingData.pending.toList()

    fun approvePendingUser(username: String, group: Int = 1): Boolean {
        val pending = pendingData.pending.find { it.username == username } ?: return false
        userData.users.add(User(pending.username, pending.password, pending.hwid, group))
        pendingData.pending.remove(pending)
        save()
        savePending()
        println("[Admin] Approved user: $username (Group: $group)")
        return true
    }

    fun approveAllPending(): Int {
        val count = pendingData.pending.size
        pendingData.pending.forEach { pending ->
            userData.users.add(User(pending.username, pending.password, pending.hwid))
        }
        pendingData.pending.clear()
        save()
        savePending()
        println("[Admin] Approved all $count pending users")
        return count
    }

    fun clearAllPending(): Int {
        val count = pendingData.pending.size
        pendingData.pending.clear()
        savePending()
        println("[Admin] Cleared all $count pending users")
        return count
    }

    fun resetUserHwid(username: String): Boolean {
        val user = userData.users.find { it.username == username } ?: return false
        val index = userData.users.indexOf(user)
        userData.users[index] = user.copy(hwid = "")
        save()
        println("[Admin] Reset HWID for user: $username")
        return true
    }

    fun setUserGroup(username: String, group: Int): Boolean {
        val user = userData.users.find { it.username == username } ?: return false
        val index = userData.users.indexOf(user)
        userData.users[index] = user.copy(group = group)
        save()
        println("[Admin] Set user '$username' group to $group")
        return true
    }

    fun getUserGroup(username: String): Int {
        return userData.users.find { it.username == username }?.group ?: 1
    }

    /**
     * 根据用户组等级获取该用户无权加载的类列表
     * 返回逗号分隔的类名（混淆后的类名）
     */
    fun getRestrictedClassesForGroup(group: Int): String {
        // 从 JSON 配置读取
        load()  // 热重载配置（包含映射）

        val restrictions = mutableListOf<String>()

        // 遍历所有等级限制，收集用户无权访问的类
        for ((requiredLevel, classes) in groupRestrictions.restrictions) {
            if (group < requiredLevel) {
                // TODO:mapping转换
                //restrictions.addAll(MappingsLoader.mapClassNames(classes))
                restrictions.addAll(classes)
            }
        }

        return restrictions.joinToString(",")
    }

    fun getGroupRestrictions() = groupRestrictions

    // 签名密钥（与客户端共享）
    private const val HMAC_KEY = "SakuraVerify2024!@#"

    /**
     * 为用户组生成 HMAC 签名
     */
    fun signGroup(group: Int): String {
        return try {
            val data = "group:$group"
            val mac = Mac.getInstance("HmacSHA256")
            val key = SecretKeySpec(HMAC_KEY.toByteArray(Charsets.UTF_8), "HmacSHA256")
            mac.init(key)
            val hash = mac.doFinal(data.toByteArray(Charsets.UTF_8))
            Base64.getEncoder().encodeToString(hash)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 为限制类列表生成签名
     */
    fun signRestrictions(group: Int, restrictions: String): String {
        return try {
            val data = "group:$group|restrictions:$restrictions"
            val mac = Mac.getInstance("HmacSHA256")
            val key = SecretKeySpec(HMAC_KEY.toByteArray(Charsets.UTF_8), "HmacSHA256")
            mac.init(key)
            val hash = mac.doFinal(data.toByteArray(Charsets.UTF_8))
            Base64.getEncoder().encodeToString(hash)
        } catch (e: Exception) {
            ""
        }
    }

    fun deleteUser(username: String): Boolean {
        val removed = userData.users.removeIf { it.username == username }
        if (removed) {
            save()
            println("[Admin] Deleted user: $username")
        }
        return removed
    }

    fun deletePendingUser(username: String): Boolean {
        val removed = pendingData.pending.removeIf { it.username == username }
        if (removed) {
            savePending()
            println("[Admin] Deleted pending user: $username")
        }
        return removed
    }

    enum class VerifyResult {
        SUCCESS,
        USER_NOT_FOUND,
        WRONG_PASSWORD,
        HWID_MISMATCH
    }

    enum class RegisterResult {
        SUCCESS,
        USERNAME_EXISTS,
        PENDING_EXISTS
    }
}
