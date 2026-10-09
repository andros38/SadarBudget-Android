package id.sadarbudget.mobile.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Template UI tersimpan lokal per akun, tidak menghasilkan transaksi otomatis. */
data class TransactionTemplate(
    val name: String, val type: String, val amount: String,
    val categoryId: Int, val description: String,
)

class TemplateStore(context: Context) {
    private val prefs = context.getSharedPreferences("sadarbudget_templates_v1", Context.MODE_PRIVATE)
    private fun key(userId: Int) = "templates_$userId"
    fun list(userId: Int, type: String = ""): List<TransactionTemplate> {
        if (userId < 1) return emptyList()
        val array = runCatching { JSONArray(prefs.getString(key(userId), "[]")) }.getOrDefault(JSONArray())
        return (0 until array.length()).mapNotNull { i ->
            runCatching {
                val item = array.getJSONObject(i)
                TransactionTemplate(item.getString("name"), item.getString("type"), item.getString("amount"),
                    item.getInt("categoryId"), item.optString("description"))
            }.getOrNull()
        }.filter { type.isBlank() || it.type == type }
    }
    private fun saveAll(userId: Int, items: List<TransactionTemplate>) {
        val array = JSONArray()
        items.take(20).forEach { entry -> array.put(JSONObject().apply {
            put("name", entry.name); put("type", entry.type); put("amount", entry.amount)
            put("categoryId", entry.categoryId); put("description", entry.description)
        }) }
        prefs.edit().putString(key(userId), array.toString()).apply()
    }
    fun save(userId: Int, template: TransactionTemplate) {
        require(userId > 0) { "Akun tidak ditemukan." }
        require(template.name.trim().length in 2..40) { "Nama template harus 2–40 karakter." }
        require(template.type == "income" || template.type == "expense") { "Jenis template tidak valid." }
        require(template.amount.filter(Char::isDigit).toLongOrNull()?.let { it > 0 } == true) { "Isi nominal lebih dahulu." }
        val items = list(userId).filterNot { it.type == template.type && it.name.equals(template.name.trim(), true) }
        saveAll(userId, listOf(template.copy(name = template.name.trim())) + items)
    }
    fun replace(userId: Int, items: List<TransactionTemplate>) = saveAll(userId, items)
    fun delete(userId: Int, template: TransactionTemplate) = saveAll(userId,
        list(userId).filterNot { it.type == template.type && it.name == template.name })
}
