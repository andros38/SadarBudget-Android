package id.sadarbudget.mobile.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Base64
import android.util.Patterns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.max

class ApiFailure(message: String, val fieldErrors: Map<String, String> = emptyMap()) : Exception(message)

class AppRepository(
    private val context: Context,
    private val session: SessionStore,
) {
    private val database = LocalDatabase(context.applicationContext)
    private val templates = TemplateStore(context.applicationContext)
    private val idLocale = Locale("id", "ID")
    private val monthFull = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
    private val monthShort = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
    private val backupRoot = File(context.filesDir, "backups").apply { mkdirs() }

    suspend fun login(email: String, password: String): AuthData = withContext(Dispatchers.IO) {
        val normalized = email.trim().lowercase(Locale.ROOT)
        require(normalized.isNotBlank() && password.isNotBlank()) { "Email dan kata sandi wajib diisi." }
        val db = database.readableDatabase
        db.rawQuery(
            "SELECT id, password_hash, password_salt FROM users WHERE email=? COLLATE NOCASE LIMIT 1",
            arrayOf(normalized),
        ).use { cursor ->
            if (!cursor.moveToFirst()) throw ApiFailure("Email atau kata sandi salah.")
            val id = cursor.getInt(0)
            val hash = cursor.getString(1)
            val salt = cursor.getString(2)
            if (!PasswordHasher.verify(password, salt, hash)) throw ApiFailure("Email atau kata sandi salah.")
            session.activeUserId = id
            AuthData("local:$id", getUserById(id) ?: throw ApiFailure("Akun tidak ditemukan."))
        }
    }

    suspend fun register(name: String, email: String, password: String, confirm: String): AuthData = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        if (cleanName.length < 2) throw ApiFailure("Nama minimal 2 karakter.")
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) throw ApiFailure("Format email tidak valid.")
        if (password.length < 8) throw ApiFailure("Kata sandi minimal 8 karakter.")
        if (password != confirm) throw ApiFailure("Konfirmasi kata sandi tidak sama.")

        val (salt, hash) = PasswordHasher.create(password)
        val now = nowDateTime()
        val db = database.writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put("name", cleanName)
                put("email", cleanEmail)
                put("password_hash", hash)
                put("password_salt", salt)
                put("created_at", now)
                put("updated_at", now)
            }
            val id = try {
                db.insertOrThrow("users", null, values).toInt()
            } catch (_: SQLiteConstraintException) {
                throw ApiFailure("Email sudah digunakan pada perangkat ini.")
            }
            seedDefaultCategories(db, id, now)
            db.setTransactionSuccessful()
            session.activeUserId = id
            AuthData("local:$id", getUserById(id) ?: throw ApiFailure("Akun gagal dibuat."))
        } finally {
            db.endTransaction()
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) { session.clearSession() }

    suspend fun me(): User = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        getUserById(userId) ?: throw ApiFailure("Akun tidak ditemukan.")
    }

    suspend fun dashboard(): DashboardData = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        val all = allTransactionsChronological(userId)
        val totalIncome = all.filter { it.type == "income" }.sumOf { it.amount }
        val totalExpense = all.filter { it.type == "expense" }.sumOf { it.amount }
        val balance = totalIncome - totalExpense
        val current = YearMonth.now()
        val monthKey = current.toString()
        val monthRows = all.filter { it.transactionDate.startsWith(monthKey) }
        val monthIncome = monthRows.filter { it.type == "income" }.sumOf { it.amount }
        val monthExpense = monthRows.filter { it.type == "expense" }.sumOf { it.amount }

        val expenseRatio: Double?
        val progress: Double
        val state: String
        val note: String
        if (monthIncome > 0) {
            expenseRatio = monthExpense / monthIncome * 100.0
            progress = expenseRatio.coerceIn(0.0, 100.0)
            when {
                expenseRatio > 100 -> {
                    state = "danger"
                    note = "Pengeluaran melampaui pemasukan bulan ini sebesar ${formatRupiah(monthExpense - monthIncome)}."
                }
                expenseRatio >= 80 -> {
                    state = "warning"
                    note = "Pengeluaran sudah mendekati seluruh pemasukan bulan ini."
                }
                expenseRatio >= 50 -> {
                    state = "attention"
                    note = "Lebih dari separuh pemasukan bulan ini telah digunakan."
                }
                else -> {
                    state = "healthy"
                    note = "Pengeluaran masih di bawah separuh pemasukan bulan ini."
                }
            }
        } else if (monthExpense > 0) {
            expenseRatio = null
            progress = 100.0
            state = "danger"
            note = "Belum ada pemasukan bulan ini, sehingga rasio tidak dapat dihitung."
        } else {
            expenseRatio = null
            progress = 0.0
            state = "neutral"
            note = "Belum ada pemasukan atau pengeluaran pada bulan ini."
        }

        val firstGraphMonth = current.minusMonths(5)
        var running = all.filter { YearMonth.parse(it.transactionDate.substring(0, 7)) < firstGraphMonth }
            .sumOf { if (it.type == "income") it.amount else -it.amount }
        val chart = (5 downTo 0).map { offset ->
            val ym = current.minusMonths(offset.toLong())
            val rows = all.filter { it.transactionDate.startsWith(ym.toString()) }
            val income = rows.filter { it.type == "income" }.sumOf { it.amount }
            val expense = rows.filter { it.type == "expense" }.sumOf { it.amount }
            running += income - expense
            ChartPoint(formatMonth(ym, true), running, income, expense)
        }

        DashboardData(
            balance = balance,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            monthIncome = monthIncome,
            monthExpense = monthExpense,
            monthLabel = formatMonth(current, false),
            expenseRatio = expenseRatio,
            expenseRatioProgress = progress,
            expenseRatioState = state,
            expenseRatioNote = note,
            chart = chart,
            recent = all.asReversed().take(7),
        )
    }

    suspend fun transactions(month: String, type: String, page: Int, search: String = ""): TransactionsData = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        buildTransactionsData(userId, month, type, page, search = search)
    }

    suspend fun transaction(id: Long): TransactionGetData = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        val transaction = findTransaction(userId, id) ?: throw ApiFailure("Transaksi tidak ditemukan.")
        val categories = queryCategories(userId, transaction.type, false)
        TransactionGetData(transaction, categories)
    }

    suspend fun createTransaction(body: TransactionSaveRequest): String = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        val type = body.type?.takeIf { it == "income" || it == "expense" } ?: throw ApiFailure("Jenis transaksi tidak valid.")
        val amount = parseAmount(body.amount)
        validateDate(body.transactionDate)
        if (body.description.length > 255) throw ApiFailure("Keterangan maksimal 255 karakter.")
        val categoryId = body.categoryId ?: throw ApiFailure("Pilih kategori transaksi.")
        val category = findCategory(userId, categoryId) ?: throw ApiFailure("Kategori tidak ditemukan.")
        if (category.type != type || !category.isActive) throw ApiFailure("Kategori tidak sesuai dengan jenis transaksi.")
        val now = nowDateTime()
        val db = database.writableDatabase
        val values = ContentValues().apply {
            put("user_id", userId)
            put("category_id", categoryId)
            put("category_name_snapshot", category.name)
            put("type", type)
            put("amount", amount)
            body.description.trim().takeIf { it.isNotBlank() }?.let { put("description", it) } ?: putNull("description")
            put("transaction_date", body.transactionDate)
            put("created_at", now)
        }
        db.insertOrThrow("transactions", null, values)
        "Transaksi berhasil disimpan."
    }

    suspend fun updateTransaction(body: TransactionSaveRequest): String = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        val id = body.id ?: throw ApiFailure("ID transaksi tidak tersedia.")
        val existing = findTransaction(userId, id) ?: throw ApiFailure("Transaksi tidak ditemukan.")
        val amount = parseAmount(body.amount)
        validateDate(body.transactionDate)
        if (body.description.length > 255) throw ApiFailure("Keterangan maksimal 255 karakter.")

        var categoryId: Int? = existing.categoryId
        var categorySnapshot = existing.categoryName
        if (body.categoryId != null) {
            val category = findCategory(userId, body.categoryId) ?: throw ApiFailure("Kategori tidak ditemukan.")
            if (category.type != existing.type) throw ApiFailure("Kategori tidak sesuai dengan jenis transaksi.")
            categoryId = category.id
            categorySnapshot = category.name
        } else if (existing.categoryId != null) {
            categoryId = null
        }

        createAutomaticBackup(userId, "before_transaction_update")
        val values = ContentValues().apply {
            if (categoryId == null) putNull("category_id") else put("category_id", categoryId)
            put("category_name_snapshot", categorySnapshot)
            put("amount", amount)
            body.description.trim().takeIf { it.isNotBlank() }?.let { put("description", it) } ?: putNull("description")
            put("transaction_date", body.transactionDate)
        }
        val updated = database.writableDatabase.update("transactions", values, "id=? AND user_id=?", arrayOf(id.toString(), userId.toString()))
        if (updated == 0) throw ApiFailure("Transaksi tidak ditemukan.")
        "Transaksi berhasil diperbarui."
    }

    suspend fun deleteTransaction(id: Long): String = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        if (findTransaction(userId, id) == null) throw ApiFailure("Transaksi tidak ditemukan.")
        createAutomaticBackup(userId, "before_transaction_delete")
        database.writableDatabase.delete("transactions", "id=? AND user_id=?", arrayOf(id.toString(), userId.toString()))
        "Transaksi berhasil dihapus."
    }

    /** Pemulihan terbatas untuk aksi Undo, dengan ID dan waktu asli agar saldo/urutan tetap konsisten. */
    suspend fun restoreDeletedTransaction(item: Transaction): String = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        if (findTransaction(userId, item.id) != null) throw ApiFailure("Transaksi sudah ada; pemulihan dibatalkan.")
        val categoryId = item.categoryId?.takeIf { id -> findCategory(userId, id)?.type == item.type }
        val values = ContentValues().apply {
            put("id", item.id); put("user_id", userId)
            if (categoryId == null) putNull("category_id") else put("category_id", categoryId)
            put("category_name_snapshot", item.categoryName)
            put("type", item.type); put("amount", item.amount)
            item.description?.let { put("description", it) } ?: putNull("description")
            put("transaction_date", item.transactionDate)
            put("created_at", item.createdAt ?: nowDateTime())
        }
        database.writableDatabase.insertOrThrow("transactions", null, values)
        "Transaksi berhasil dipulihkan."
    }

    fun transactionTemplates(type: String): List<TransactionTemplate> = templates.list(currentUserId(), type)
    fun saveTransactionTemplate(item: TransactionTemplate) { templates.save(currentUserId(), item) }
    fun deleteTransactionTemplate(item: TransactionTemplate) { templates.delete(currentUserId(), item) }

    suspend fun categories(type: String = "", activeOnly: Boolean = true): List<Category> = withContext(Dispatchers.IO) {
        queryCategories(currentUserId(), type, activeOnly)
    }

    suspend fun createCategory(name: String, type: String): String = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        val clean = name.trim()
        if (clean.length < 2 || clean.length > 100) throw ApiFailure("Nama kategori harus 2–100 karakter.")
        if (type !in listOf("income", "expense")) throw ApiFailure("Jenis kategori tidak valid.")
        val now = nowDateTime()
        val values = ContentValues().apply {
            put("user_id", userId)
            put("name", clean)
            put("type", type)
            put("is_active", 1)
            put("created_at", now)
            put("updated_at", now)
        }
        try {
            database.writableDatabase.insertOrThrow("categories", null, values)
        } catch (_: SQLiteConstraintException) {
            throw ApiFailure("Kategori dengan nama dan jenis yang sama sudah ada.")
        }
        "Kategori berhasil ditambahkan."
    }

    suspend fun deactivateCategory(id: Int): String = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        val category = findCategory(userId, id) ?: throw ApiFailure("Kategori tidak ditemukan.")
        if (!category.isActive) return@withContext "Kategori sudah nonaktif."
        createAutomaticBackup(userId, "before_category_deactivate")
        val values = ContentValues().apply { put("is_active", 0); put("updated_at", nowDateTime()) }
        database.writableDatabase.update("categories", values, "id=? AND user_id=?", arrayOf(id.toString(), userId.toString()))
        "Kategori berhasil dinonaktifkan."
    }

    suspend fun annual(year: Int? = null): AnnualData = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        val all = allTransactionsChronological(userId)
        val currentYear = LocalDate.now().year
        val yearsInData = all.mapNotNull { it.transactionDate.take(4).toIntOrNull() }
        val minYear = yearsInData.minOrNull() ?: currentYear
        val maxYear = yearsInData.maxOrNull() ?: currentYear
        val top = max(maxYear, currentYear)
        val bottom = minOf(minYear, currentYear)
        val available = (top downTo bottom).toList()
        val selected = year?.takeIf { it in 2000..2100 } ?: currentYear
        val rows = all.filter { it.transactionDate.startsWith("%04d-".format(Locale.US, selected)) }
        val months = (1..12).map { month ->
            val prefix = "%04d-%02d".format(Locale.US, selected, month)
            val monthRows = rows.filter { it.transactionDate.startsWith(prefix) }
            val income = monthRows.filter { it.type == "income" }.sumOf { it.amount }
            val expense = monthRows.filter { it.type == "expense" }.sumOf { it.amount }
            val net = income - expense
            AnnualMonth(
                month = month,
                label = "${monthShort[month - 1]} $selected",
                fullLabel = "${monthFull[month - 1]} $selected",
                hasActivity = income > 0 || expense > 0,
                income = income,
                expense = expense,
                moneyNet = net,
                assetNet = net,
            )
        }
        val totalIncome = months.sumOf { it.income }
        val totalExpense = months.sumOf { it.expense }
        val totals = AnnualTotals(totalIncome, totalExpense, totalIncome - totalExpense, totalIncome - totalExpense)
        val topCategories = rows.filter { it.type == "expense" }
            .groupBy { it.categoryName.ifBlank { "Tanpa kategori" } }
            .map { (name, list) -> TopCategory(name, list.sumOf { it.amount }) }
            .sortedWith(compareByDescending<TopCategory> { it.totalAmount }.thenBy { it.categoryName })
            .take(6)
        val active = months.filter { it.hasActivity }
        val best = active.maxByOrNull { it.assetNet }
        AnnualData(available, AnnualRecap(selected, months, totals, topCategories, active.size, best))
    }

    suspend fun updateProfile(name: String, email: String): ProfileData = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        val cleanName = name.trim()
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        if (cleanName.length < 2 || cleanName.length > 100) throw ApiFailure("Nama harus 2–100 karakter.")
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) throw ApiFailure("Format email tidak valid.")
        val db = database.writableDatabase
        db.rawQuery("SELECT COUNT(*) FROM users WHERE email=? COLLATE NOCASE AND id<>?", arrayOf(cleanEmail, userId.toString())).use {
            it.moveToFirst(); if (it.getInt(0) > 0) throw ApiFailure("Email sudah digunakan oleh akun lain di perangkat ini.")
        }
        val values = ContentValues().apply { put("name", cleanName); put("email", cleanEmail); put("updated_at", nowDateTime()) }
        db.update("users", values, "id=?", arrayOf(userId.toString()))
        ProfileData(getUserById(userId) ?: throw ApiFailure("Akun tidak ditemukan."))
    }

    suspend fun updatePassword(current: String, newPassword: String, confirm: String): String = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        if (newPassword.length < 8) throw ApiFailure("Kata sandi baru minimal 8 karakter.")
        if (newPassword != confirm) throw ApiFailure("Konfirmasi kata sandi baru tidak sama.")
        val db = database.readableDatabase
        val credentials = db.rawQuery("SELECT password_hash,password_salt FROM users WHERE id=?", arrayOf(userId.toString())).use {
            if (!it.moveToFirst()) throw ApiFailure("Akun tidak ditemukan.")
            it.getString(0) to it.getString(1)
        }
        if (!PasswordHasher.verify(current, credentials.second, credentials.first)) throw ApiFailure("Kata sandi saat ini salah.")
        val (salt, hash) = PasswordHasher.create(newPassword)
        val now = nowDateTime()
        val values = ContentValues().apply {
            put("password_hash", hash); put("password_salt", salt); put("password_changed_at", now); put("updated_at", now)
        }
        database.writableDatabase.update("users", values, "id=?", arrayOf(userId.toString()))
        "Kata sandi berhasil diperbarui."
    }

    suspend fun removePhoto(): ProfileData = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        getUserById(userId)?.profilePhoto?.let { raw ->
            runCatching { if (raw.startsWith("file:")) File(Uri.parse(raw).path.orEmpty()).delete() }
        }
        val values = ContentValues().apply { putNull("profile_photo"); put("updated_at", nowDateTime()) }
        database.writableDatabase.update("users", values, "id=?", arrayOf(userId.toString()))
        ProfileData(getUserById(userId) ?: throw ApiFailure("Akun tidak ditemukan."))
    }

    suspend fun uploadPhoto(file: File): ProfileData = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        val profileDir = File(context.filesDir, "profiles").apply { mkdirs() }
        val target = File(profileDir, "profile-$userId.jpg")
        file.inputStream().use { input -> target.outputStream().use { output -> input.copyTo(output) } }
        val uri = Uri.fromFile(target).toString()
        val values = ContentValues().apply { put("profile_photo", uri); put("updated_at", nowDateTime()) }
        database.writableDatabase.update("users", values, "id=?", arrayOf(userId.toString()))
        ProfileData(getUserById(userId) ?: throw ApiFailure("Akun tidak ditemukan."))
    }

    suspend fun dataStatus(): DataStatus = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        val transactionCount = count("transactions", "user_id=?", arrayOf(userId.toString()))
        val categoryCount = count("categories", "user_id=?", arrayOf(userId.toString()))
        trimBackups(userId)
        val items = backupFiles(userId).map { file ->
            val reason = when {
                file.name.contains("manual") -> "manual"
                file.name.contains("safety") -> "safety"
                else -> "auto"
            }
            BackupItem(
                filename = file.name,
                createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(file.lastModified())),
                reason = reason,
                reasonLabel = when (reason) { "manual" -> "Versi manual"; "safety" -> "Versi pengaman"; else -> "Versi otomatis" },
                size = file.length(),
            )
        }
        val total = items.sumOf { it.size }
        DataStatus(
            DataCounts(transactionCount, categoryCount),
            AutoBackupStatus(
                enabled = true,
                count = items.size,
                maxFiles = MAX_BACKUPS,
                totalSize = total,
                totalSizeLabel = formatFileSize(total),
                intervalMinutes = AUTO_BACKUP_INTERVAL_MINUTES,
                latest = items.firstOrNull(),
                items = items,
                lastError = null,
            ),
        )
    }

    suspend fun createBackup(): String = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        writeBackupFile(userId, "manual")
        "Versi pemulihan lokal berhasil dibuat."
    }

    suspend fun downloadBackup(file: String = "latest"): ByteArray = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        val target = if (file == "latest") backupFiles(userId).firstOrNull() else File(backupDir(userId), safeFilename(file))
        if (target == null || !target.exists()) throw ApiFailure("Versi pemulihan tidak ditemukan.")
        target.readBytes()
    }

    suspend fun restoreBackup(file: String, password: String, confirmation: String, acknowledge: Boolean): String = withContext(Dispatchers.IO) {
        requireConfirmation(password, confirmation, "PULIHKAN BACKUP", acknowledge)
        val userId = currentUserId()
        val target = File(backupDir(userId), safeFilename(file))
        if (!target.exists()) throw ApiFailure("Versi pemulihan tidak ditemukan.")
        writeBackupFile(userId, "safety")
        restoreSqlText(userId, target.readText(Charsets.UTF_8))
        "Versi data berhasil dipulihkan."
    }

    suspend fun exportSql(password: String): ByteArray = withContext(Dispatchers.IO) {
        verifyCurrentPassword(password)
        buildBackupSql(currentUserId()).toByteArray(StandardCharsets.UTF_8)
    }

    suspend fun importSql(uri: Uri, password: String, confirmation: String, acknowledge: Boolean): String = withContext(Dispatchers.IO) {
        requireConfirmation(password, confirmation, "IMPOR DATA", acknowledge)
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw ApiFailure("Berkas tidak dapat dibuka.")
        if (bytes.size > 10 * 1024 * 1024) throw ApiFailure("Ukuran backup melebihi batas 10 MB.")
        val userId = currentUserId()
        writeBackupFile(userId, "safety")
        restoreSqlText(userId, bytes.toString(StandardCharsets.UTF_8))
        "Backup SQL berhasil diimpor ke perangkat."
    }

    suspend fun exportEncryptedSql(accountPassword: String, archivePassword: String): ByteArray = withContext(Dispatchers.IO) {
        verifyCurrentPassword(accountPassword)
        EncryptedBackup.encrypt(buildBackupSql(currentUserId()).toByteArray(StandardCharsets.UTF_8), archivePassword)
    }

    suspend fun importEncryptedSql(uri: Uri, password: String, archivePassword: String, confirmation: String, acknowledge: Boolean): String = withContext(Dispatchers.IO) {
        requireConfirmation(password, confirmation, "IMPOR DATA", acknowledge)
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw ApiFailure("Berkas tidak dapat dibuka.")
        if (bytes.size > 10 * 1024 * 1024) throw ApiFailure("Ukuran backup melebihi batas 10 MB.")
        val decoded = EncryptedBackup.decrypt(bytes, archivePassword)
        val sql = decoded.toString(StandardCharsets.UTF_8)
        if (!sql.contains("-- SADARBUDGET_PAYLOAD_BEGIN")) throw ApiFailure("Konten backup tidak valid.")
        val userId = currentUserId()
        writeBackupFile(userId, "safety")
        restoreSqlText(userId, sql)
        "Backup terenkripsi berhasil dipulihkan."
    }

    suspend fun verifyAccountPassword(password: String) = withContext(Dispatchers.IO) { verifyCurrentPassword(password) }

    suspend fun cleanData(password: String, confirmation: String, acknowledge: Boolean): String = withContext(Dispatchers.IO) {
        requireConfirmation(password, confirmation, "BERSIHKAN DATA", acknowledge)
        val userId = currentUserId()
        writeBackupFile(userId, "safety")
        val db = database.writableDatabase
        db.beginTransaction()
        try {
            db.delete("transactions", "user_id=?", arrayOf(userId.toString()))
            db.delete("categories", "user_id=?", arrayOf(userId.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        "Seluruh transaksi dan kategori berhasil dibersihkan."
    }

    suspend fun reportPdf(month: String, type: String): ByteArray = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        val user = getUserById(userId) ?: throw ApiFailure("Akun tidak ditemukan.")
        val data = buildTransactionsData(userId, month, type, 1, Int.MAX_VALUE)
        val rows = data.rows.asReversed()
        val document = PdfDocument()

        val pageWidth = 842
        val pageHeight = 595
        val left = 44f
        val right = 798f
        val indigo = android.graphics.Color.rgb(29, 78, 216)
        val teal = android.graphics.Color.rgb(14, 165, 233)
        val blue = android.graphics.Color.rgb(2, 132, 199)
        val blueSoft = android.graphics.Color.rgb(224, 242, 254)
        val red = android.graphics.Color.rgb(220, 38, 38)
        val redSoft = android.graphics.Color.rgb(254, 242, 242)
        val purple = android.graphics.Color.rgb(37, 99, 235)
        val violetSoft = android.graphics.Color.rgb(219, 234, 254)
        val slate900 = android.graphics.Color.rgb(24, 34, 52)
        val slate800 = android.graphics.Color.rgb(36, 48, 67)
        val slate600 = android.graphics.Color.rgb(71, 85, 105)
        val slate500 = android.graphics.Color.rgb(100, 116, 139)
        val slate300 = android.graphics.Color.rgb(203, 213, 225)
        val slate200 = android.graphics.Color.rgb(226, 232, 240)
        val slate100 = android.graphics.Color.rgb(241, 245, 249)
        val slate50 = android.graphics.Color.rgb(248, 250, 252)
        val white = android.graphics.Color.WHITE

        fun paint(size: Float, color: Int, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
            textAlign = align
            isSubpixelText = true
        }

        fun fitText(value: String, maxWidth: Float, textPaint: Paint): String {
            if (textPaint.measureText(value) <= maxWidth) return value
            val ellipsis = "…"
            if (textPaint.measureText(ellipsis) > maxWidth) return ""
            var low = 0
            var high = value.length
            while (low < high) {
                val mid = (low + high + 1) / 2
                val candidate = value.take(mid) + ellipsis
                if (textPaint.measureText(candidate) <= maxWidth) low = mid else high = mid - 1
            }
            return value.take(low).trimEnd() + ellipsis
        }

        fun drawFitText(canvas: android.graphics.Canvas, value: String, x: Float, y: Float, maxWidth: Float, textPaint: Paint) {
            canvas.drawText(fitText(value, maxWidth, textPaint), x, y, textPaint)
        }

        val periodLabel = if (month.isBlank()) "Semua periode" else runCatching { formatMonth(YearMonth.parse(month), false) }.getOrDefault(month)
        val typeLabel = when (type) { "income" -> "Pemasukan"; "expense" -> "Pengeluaran"; else -> "Semua jenis" }
        val createdAt = SimpleDateFormat("dd/MM/yyyy HH:mm", idLocale).format(Date())
        val firstCapacity = 14
        val nextCapacity = 19
        val chunks = mutableListOf<List<Transaction>>()
        var cursor = 0
        if (rows.isEmpty()) {
            chunks.add(emptyList())
        } else {
            chunks.add(rows.take(firstCapacity))
            cursor = minOf(firstCapacity, rows.size)
            while (cursor < rows.size) {
                chunks.add(rows.drop(cursor).take(nextCapacity))
                cursor += nextCapacity
            }
        }
        val pageCount = chunks.size

        fun displayDate(value: String): String = runCatching {
            val d = LocalDate.parse(value)
            "%02d/%02d/%04d".format(d.dayOfMonth, d.monthValue, d.year)
        }.getOrDefault(value)

        chunks.forEachIndexed { pageIndex, pageRows ->
            val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create())
            val canvas = page.canvas
            val compact = pageIndex > 0
            val headerHeight = if (compact) 82f else 90f

            // Header utama.
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), headerHeight, paint(1f, indigo))
            canvas.drawRect(0f, 0f, 8f, headerHeight, paint(1f, teal))
            canvas.drawText("SADARBUDGET", left, 31f, paint(if (compact) 18f else 21f, white, true))
            canvas.drawText("Laporan transaksi keuangan pribadi", left, 51f, paint(9.5f, android.graphics.Color.rgb(218, 228, 255)))
            canvas.drawCircle(left + 3f, 69f, 3f, paint(1f, android.graphics.Color.rgb(56, 189, 248)))
            canvas.drawText("Pengguna: ${user.name.ifBlank { "Pengguna" }}", left + 13f, 72f, paint(9f, android.graphics.Color.rgb(186, 230, 253), true))
            canvas.drawText("Periode: $periodLabel", right, 31f, paint(9.5f, white, true, Paint.Align.RIGHT))
            canvas.drawText("Jenis: $typeLabel", right, 51f, paint(9f, android.graphics.Color.rgb(218, 228, 255), false, Paint.Align.RIGHT))

            var tableTop: Float
            if (!compact) {
                val gap = 10f
                val cardWidth = (right - left - gap * 2) / 3f
                val cardY = 108f
                val cardH = 58f
                val summaries = listOf(
                    Triple("PEMASUKAN", data.income, blue),
                    Triple("PENGELUARAN", data.expense, red),
                    Triple("PERUBAHAN SALDO", data.net, purple),
                )
                summaries.forEachIndexed { index, (label, value, accent) ->
                    val x = left + index * (cardWidth + gap)
                    canvas.drawRoundRect(RectF(x, cardY, x + cardWidth, cardY + cardH), 7f, 7f, paint(1f, white))
                    canvas.drawRect(x, cardY, x + 4f, cardY + cardH, paint(1f, accent))
                    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = slate200 }
                    canvas.drawRoundRect(RectF(x, cardY, x + cardWidth, cardY + cardH), 7f, 7f, border)
                    canvas.drawText(label, x + 13f, cardY + 22f, paint(7.4f, slate500, true))
                    canvas.drawText(formatRupiah(value), x + 13f, cardY + 43f, paint(10.5f, slate900, true))
                }
                canvas.drawText(
                    "Nominal adalah nilai transaksi. Saldo berjalan menunjukkan posisi saldo sesudah transaksi diterapkan.",
                    left, 184f, paint(7.8f, purple, true),
                )
                tableTop = 202f
            } else {
                tableTop = 104f
            }

            // Header tabel.
            val headerH = 30f
            canvas.drawRoundRect(RectF(left, tableTop, right, tableTop + headerH), 5f, 5f, paint(1f, slate100))
            val headerPaint = paint(7.5f, slate600, true)
            canvas.drawText("TANGGAL", 52f, tableTop + 20f, headerPaint)
            canvas.drawText("KATEGORI", 118f, tableTop + 20f, headerPaint)
            canvas.drawText("KETERANGAN", 252f, tableTop + 20f, headerPaint)
            canvas.drawText("JENIS", 548f, tableTop + 20f, paint(7.5f, slate600, true, Paint.Align.CENTER))
            canvas.drawText("NOMINAL", 690f, tableTop + 20f, paint(7.5f, slate600, true, Paint.Align.RIGHT))
            canvas.drawText("SALDO BERJALAN", 790f, tableTop + 20f, paint(7f, slate600, true, Paint.Align.RIGHT))

            val rowH = 22f
            if (pageRows.isEmpty()) {
                val boxTop = tableTop + headerH + 14f
                canvas.drawRoundRect(RectF(left, boxTop, right, boxTop + 48f), 7f, 7f, paint(1f, slate50))
                canvas.drawText("Tidak ada transaksi pada filter laporan ini.", (left + right) / 2f, boxTop + 29f, paint(10f, slate500, false, Paint.Align.CENTER))
            } else {
                pageRows.forEachIndexed { index, row ->
                    val top = tableTop + headerH + index * rowH
                    val bottom = top + rowH
                    if (index % 2 == 1) canvas.drawRect(left, top, right, bottom, paint(1f, slate50))
                    canvas.drawLine(left, bottom, right, bottom, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = slate200; strokeWidth = 0.7f })
                    val baseline = top + 15f
                    val datePaint = paint(7.2f, slate800)
                    val categoryPaint = paint(7.2f, slate800, true)
                    val descriptionPaint = paint(7.1f, slate600)
                    drawFitText(canvas, displayDate(row.transactionDate), 52f, baseline, 58f, datePaint)
                    drawFitText(canvas, row.categoryName, 118f, baseline, 122f, categoryPaint)
                    drawFitText(canvas, row.description?.trim()?.ifBlank { "-" } ?: "-", 252f, baseline, 238f, descriptionPaint)

                    val isIncome = row.type == "income"
                    val badgeColor = if (isIncome) blue else red
                    val badgeBg = if (isIncome) blueSoft else redSoft
                    val badge = RectF(505f, top + 4f, 591f, bottom - 4f)
                    canvas.drawRoundRect(badge, 6f, 6f, paint(1f, badgeBg))
                    canvas.drawText(if (isIncome) "Pemasukan" else "Pengeluaran", 548f, baseline, paint(6.3f, badgeColor, true, Paint.Align.CENTER))

                    val nominalPaint = paint(7.1f, slate800, true, Paint.Align.RIGHT)
                    drawFitText(canvas, formatRupiah(kotlin.math.abs(row.amount)), 690f, baseline, 92f, nominalPaint)
                    val balance = row.balanceAfter ?: 0.0
                    val balancePaint = paint(7.1f, if (balance < 0) red else indigo, true, Paint.Align.RIGHT)
                    drawFitText(canvas, formatRupiah(balance), 790f, baseline, 94f, balancePaint)
                }
            }

            // Footer.
            canvas.drawLine(left, 563f, right, 563f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = slate200; strokeWidth = 0.8f })
            canvas.drawText("Dibuat pada $createdAt", left, 580f, paint(7.5f, slate500))
            canvas.drawText("Halaman ${pageIndex + 1} dari $pageCount", right, 580f, paint(7.5f, slate600, true, Paint.Align.RIGHT))
            document.finishPage(page)
        }

        ByteArrayOutputStream().use { out ->
            document.writeTo(out)
            document.close()
            out.toByteArray()
        }
    }

    suspend fun reportExcel(month: String, type: String): ByteArray = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        val user = getUserById(userId) ?: throw ApiFailure("Akun tidak ditemukan.")
        val data = buildTransactionsData(userId, month, type, 1, Int.MAX_VALUE)
        buildXlsx(data, user.name, month, type)
    }

    private fun currentUserId(): Int = session.activeUserId.takeIf { it > 0 } ?: throw ApiFailure("Silakan masuk ke akun terlebih dahulu.")

    private fun getUserById(id: Int): User? {
        return database.readableDatabase.rawQuery(
            "SELECT id,name,email,profile_photo,password_changed_at,created_at,updated_at FROM users WHERE id=? LIMIT 1",
            arrayOf(id.toString()),
        ).use { cursor -> if (cursor.moveToFirst()) cursor.toUser() else null }
    }

    private fun Cursor.toUser() = User(
        id = getInt(0),
        name = getString(1),
        email = getString(2),
        profilePhoto = if (isNull(3)) null else getString(3),
        passwordChangedAt = if (isNull(4)) null else getString(4),
        createdAt = if (isNull(5)) null else getString(5),
        updatedAt = if (isNull(6)) null else getString(6),
    )

    private fun seedDefaultCategories(db: android.database.sqlite.SQLiteDatabase, userId: Int, now: String) {
        val defaults = listOf(
            "Gaji" to "income", "Bonus" to "income", "Penjualan" to "income", "Lainnya" to "income",
            "Makan" to "expense", "Transport" to "expense", "Belanja" to "expense", "Tagihan" to "expense",
            "Hiburan" to "expense", "Kesehatan" to "expense", "Lainnya" to "expense",
        )
        defaults.forEach { (name, type) ->
            val values = ContentValues().apply {
                put("user_id", userId); put("name", name); put("type", type); put("is_active", 1); put("created_at", now); put("updated_at", now)
            }
            db.insert("categories", null, values)
        }
    }

    private fun allTransactionsChronological(userId: Int): List<Transaction> {
        val rows = mutableListOf<Transaction>()
        var running = 0.0
        database.readableDatabase.rawQuery(
            """
            SELECT id,transaction_date,created_at,type,amount,description,category_name_snapshot,category_id
            FROM transactions WHERE user_id=?
            ORDER BY transaction_date ASC, created_at ASC, id ASC
            """.trimIndent(),
            arrayOf(userId.toString()),
        ).use { c ->
            while (c.moveToNext()) {
                val type = c.getString(3)
                val amount = c.getDouble(4)
                running += if (type == "income") amount else -amount
                rows += Transaction(
                    id = c.getLong(0), transactionDate = c.getString(1), createdAt = c.getString(2), type = type,
                    amount = amount, description = if (c.isNull(5)) null else c.getString(5), categoryName = c.getString(6),
                    categoryId = if (c.isNull(7)) null else c.getInt(7), balanceAfter = running,
                )
            }
        }
        return rows
    }

    private fun buildTransactionsData(userId: Int, month: String, type: String, page: Int, perPage: Int = 12, search: String = ""): TransactionsData {
        val all = allTransactionsChronological(userId)
        val validMonth = month.takeIf { Regex("^\\d{4}-\\d{2}$").matches(it) }.orEmpty()
        val validType = type.takeIf { it == "income" || it == "expense" }.orEmpty()
        val query = search.trim().take(80)
        val filteredChronological = all.filter { row ->
            (validMonth.isBlank() || row.transactionDate.startsWith(validMonth)) &&
                (validType.isBlank() || row.type == validType) &&
                (query.isBlank() || row.categoryName.contains(query, ignoreCase = true) ||
                    row.description.orEmpty().contains(query, ignoreCase = true) ||
                    row.transactionDate.contains(query) ||
                    row.amount.toLong().toString().contains(query.filter(Char::isDigit).ifBlank { query }))
        }
        val income = filteredChronological.filter { it.type == "income" }.sumOf { it.amount }
        val expense = filteredChronological.filter { it.type == "expense" }.sumOf { it.amount }
        val display = filteredChronological.asReversed()
        val total = display.size
        val safePerPage = if (perPage <= 0) 12 else perPage
        val totalPages = max(1, if (safePerPage == Int.MAX_VALUE) 1 else (total + safePerPage - 1) / safePerPage)
        val safePage = page.coerceIn(1, totalPages)
        val rows = if (safePerPage == Int.MAX_VALUE) display else display.drop((safePage - 1) * safePerPage).take(safePerPage)
        return TransactionsData(rows, safePage, safePerPage, total, totalPages, income, expense, income - expense, all.lastOrNull()?.balanceAfter ?: 0.0, validMonth, validType)
    }

    private fun findTransaction(userId: Int, id: Long): Transaction? = allTransactionsChronological(userId).firstOrNull { it.id == id }

    private fun queryCategories(userId: Int, type: String, activeOnly: Boolean): List<Category> {
        val args = mutableListOf(userId.toString())
        val where = StringBuilder("c.user_id=?")
        if (type == "income" || type == "expense") { where.append(" AND c.type=?"); args += type }
        if (activeOnly) where.append(" AND c.is_active=1")
        val result = mutableListOf<Category>()
        database.readableDatabase.rawQuery(
            """
            SELECT c.id,c.name,c.type,c.is_active,COUNT(t.id)
            FROM categories c LEFT JOIN transactions t ON t.category_id=c.id
            WHERE $where
            GROUP BY c.id,c.name,c.type,c.is_active
            ORDER BY c.type ASC,c.name COLLATE NOCASE ASC
            """.trimIndent(), args.toTypedArray(),
        ).use { c ->
            while (c.moveToNext()) result += Category(c.getInt(0), c.getString(1), c.getString(2), c.getInt(3) == 1, c.getInt(4))
        }
        return result
    }

    private fun findCategory(userId: Int, id: Int): Category? = queryCategories(userId, "", false).firstOrNull { it.id == id }

    private fun parseAmount(raw: String): Double {
        val digits = raw.filter(Char::isDigit)
        val value = digits.toDoubleOrNull() ?: throw ApiFailure("Nominal transaksi tidak valid.")
        if (value <= 0 || value > 9_999_999_999_999.99) throw ApiFailure("Nominal transaksi berada di luar batas yang didukung.")
        return value
    }

    private fun validateDate(value: String) {
        runCatching { LocalDate.parse(value) }.getOrElse { throw ApiFailure("Tanggal transaksi tidak valid.") }
    }

    private fun formatMonth(ym: YearMonth, short: Boolean) = "${if (short) monthShort[ym.monthValue - 1] else monthFull[ym.monthValue - 1]} ${ym.year}"

    private fun formatRupiah(value: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(idLocale).apply { maximumFractionDigits = 0; minimumFractionDigits = 0 }
        return formatter.format(value).replace("Rp", "Rp ").replace("  ", " ")
    }

    private fun nowDateTime(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

    private fun count(table: String, where: String, args: Array<String>): Int = database.readableDatabase.rawQuery("SELECT COUNT(*) FROM $table WHERE $where", args).use {
        it.moveToFirst(); it.getInt(0)
    }

    private fun verifyCurrentPassword(password: String) {
        val userId = currentUserId()
        database.readableDatabase.rawQuery("SELECT password_hash,password_salt FROM users WHERE id=?", arrayOf(userId.toString())).use {
            if (!it.moveToFirst() || !PasswordHasher.verify(password, it.getString(1), it.getString(0))) throw ApiFailure("Kata sandi saat ini salah.")
        }
    }

    private fun requireConfirmation(password: String, confirmation: String, required: String, acknowledge: Boolean) {
        verifyCurrentPassword(password)
        if (confirmation != required) throw ApiFailure("Teks konfirmasi tidak sesuai.")
        if (!acknowledge) throw ApiFailure("Konfirmasi persetujuan belum dicentang.")
    }


    private fun createAutomaticBackup(userId: Int, reason: String) {
        runCatching { writeBackupFile(userId, "safety-$reason") }
    }

    private fun writeBackupFile(userId: Int, reason: String): File {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(Date())
        val kind = when { reason.startsWith("manual") -> "manual"; reason.startsWith("safety") -> "safety"; else -> "auto" }
        val file = File(backupDir(userId), "sadarbudget-$kind-$stamp.sql")
        file.writeText(buildBackupSql(userId), Charsets.UTF_8)
        trimBackups(userId)
        return file
    }

    private fun backupDir(userId: Int): File = File(backupRoot, "user-$userId").apply { mkdirs() }

    private fun backupFiles(userId: Int): List<File> = backupDir(userId).listFiles { file ->
        file.isFile && file.extension.lowercase(Locale.ROOT) == "sql"
    }?.sortedByDescending { it.lastModified() }.orEmpty()

    private fun trimBackups(userId: Int) {
        backupFiles(userId).drop(MAX_BACKUPS).forEach { it.delete() }
    }

    private fun safeFilename(value: String): String {
        val name = File(value).name
        if (name != value || !name.endsWith(".sql", true)) throw ApiFailure("Nama versi pemulihan tidak valid.")
        return name
    }

    private fun buildBackupSql(userId: Int): String {
        val user = getUserById(userId) ?: throw ApiFailure("Akun tidak ditemukan.")
        val categories = mutableListOf<JSONObject>()
        database.readableDatabase.rawQuery(
            "SELECT id,name,type,is_active,created_at,updated_at FROM categories WHERE user_id=? ORDER BY id",
            arrayOf(userId.toString()),
        ).use { c ->
            while (c.moveToNext()) categories += JSONObject().apply {
                put("old_id", c.getInt(0)); put("name", c.getString(1)); put("type", c.getString(2)); put("is_active", c.getInt(3)); put("created_at", c.getString(4)); put("updated_at", c.getString(5))
            }
        }
        val transactions = mutableListOf<JSONObject>()
        database.readableDatabase.rawQuery(
            "SELECT category_id,category_name_snapshot,type,amount,description,transaction_date,created_at FROM transactions WHERE user_id=? ORDER BY id",
            arrayOf(userId.toString()),
        ).use { c ->
            while (c.moveToNext()) transactions += JSONObject().apply {
                if (c.isNull(0)) put("category_old_id", JSONObject.NULL) else put("category_old_id", c.getInt(0))
                put("category_name_snapshot", c.getString(1)); put("type", c.getString(2)); put("amount", c.getDouble(3))
                if (c.isNull(4)) put("description", JSONObject.NULL) else put("description", c.getString(4))
                put("transaction_date", c.getString(5)); put("created_at", c.getString(6))
            }
        }
        val generatedAt = nowDateTime()
        val payload = JSONObject().apply {
            put("format", 2); put("application", "SadarBudget"); put("generated_at", generatedAt); put("email", user.email)
            put("categories", JSONArray(categories)); put("transactions", JSONArray(transactions))
            val savedTemplates = JSONArray()
            templates.list(userId).forEach { template -> savedTemplates.put(JSONObject().apply {
                put("name", template.name); put("type", template.type); put("amount", template.amount)
                put("category_old_id", template.categoryId); put("description", template.description)
            }) }
            put("templates", savedTemplates)
        }
        val encoded = Base64.encodeToString(payload.toString().toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        val lines = mutableListOf(
            "-- SadarBudget — backup data pengguna",
            "-- Format: 2",
            "-- Dibuat: $generatedAt",
            "-- Akun: ${user.email}",
            "--",
            "-- Backup dibuat oleh SadarBudget Android Offline.",
            "-- Dapat diimpor kembali melalui Data & Backup.",
            "-- SADARBUDGET_PAYLOAD_BEGIN",
        )
        encoded.chunked(100).forEach { lines += "-- $it" }
        lines += "-- SADARBUDGET_PAYLOAD_END"
        lines += ""
        lines += "SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;"
        lines += "START TRANSACTION;"
        lines += ""
        lines += "SET @sb_email = ${sqlQuote(user.email)};"
        lines += "SET @sb_user_id = (SELECT id FROM users WHERE BINARY email = BINARY @sb_email LIMIT 1);"
        lines += "CREATE TEMPORARY TABLE sb_restore_guard (user_id INT UNSIGNED NOT NULL);"
        lines += "INSERT INTO sb_restore_guard (user_id) VALUES (@sb_user_id);"
        lines += "DROP TEMPORARY TABLE sb_restore_guard;"
        lines += ""
        lines += "DELETE FROM transactions WHERE user_id=@sb_user_id;"
        lines += "DELETE FROM categories WHERE user_id=@sb_user_id;"
        lines += ""
        lines += "-- Kategori"
        categories.forEach { item ->
            val oldId = item.getInt("old_id")
            lines += "INSERT INTO categories (user_id, name, type, is_active, created_at, updated_at) VALUES (@sb_user_id, ${sqlQuote(item.getString("name"))}, ${sqlQuote(item.getString("type"))}, ${item.getInt("is_active")}, ${sqlQuote(item.getString("created_at"))}, ${sqlQuote(item.getString("updated_at"))});"
            lines += "SET @sb_category_$oldId = LAST_INSERT_ID();"
        }
        lines += ""
        lines += "-- Transaksi pemasukan dan pengeluaran"
        transactions.forEach { item ->
            val categoryRef = if (item.isNull("category_old_id")) "NULL" else "@sb_category_${item.getInt("category_old_id")}" 
            val description = if (item.isNull("description")) "NULL" else sqlQuote(item.getString("description"))
            lines += "INSERT INTO transactions (user_id, category_id, category_name_snapshot, type, amount, description, transaction_date, created_at) VALUES (@sb_user_id, $categoryRef, ${sqlQuote(item.getString("category_name_snapshot"))}, ${sqlQuote(item.getString("type"))}, ${String.format(Locale.US, "%.2f", item.getDouble("amount"))}, $description, ${sqlQuote(item.getString("transaction_date"))}, ${sqlQuote(item.getString("created_at"))});"
        }
        lines += ""
        lines += "COMMIT;"
        lines += "SELECT CONCAT(\"Backup SadarBudget berhasil dipulihkan untuk \", @sb_email) AS status;"
        lines += ""
        return lines.joinToString("\n")
    }

    private fun sqlQuote(value: String): String = "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'"

    private fun restoreSqlText(userId: Int, sql: String) {
        if (!sql.contains("SadarBudget")) throw ApiFailure("Berkas bukan backup SadarBudget.")
        val begin = "-- SADARBUDGET_PAYLOAD_BEGIN"
        val end = "-- SADARBUDGET_PAYLOAD_END"
        val start = sql.indexOf(begin)
        val finish = sql.indexOf(end)
        if (start < 0 || finish <= start) throw ApiFailure("Backup tidak memiliki payload SadarBudget 2.x yang dapat dibaca secara offline.")
        val encoded = sql.substring(start + begin.length, finish).lineSequence()
            .map { it.trim().removePrefix("--").trim() }
            .filter { it.isNotBlank() }
            .joinToString("")
        val jsonText = runCatching { String(Base64.decode(encoded, Base64.DEFAULT), StandardCharsets.UTF_8) }
            .getOrElse { throw ApiFailure("Payload backup tidak dapat didekodekan.") }
        val root = runCatching { JSONObject(jsonText) }.getOrElse { throw ApiFailure("Payload backup tidak valid.") }
        if (root.optString("application") != "SadarBudget") throw ApiFailure("Payload bukan milik SadarBudget.")
        val current = getUserById(userId) ?: throw ApiFailure("Akun tidak ditemukan.")
        val backupEmail = root.optString("email").trim()
        if (backupEmail.isNotBlank() && !backupEmail.equals(current.email, ignoreCase = true)) {
            throw ApiFailure("Email backup ($backupEmail) berbeda dari akun saat ini (${current.email}). Gunakan email yang sama agar pemulihan aman.")
        }
        val categoryArray = root.optJSONArray("categories") ?: throw ApiFailure("Daftar kategori tidak ditemukan dalam backup.")
        val transactionArray = root.optJSONArray("transactions") ?: throw ApiFailure("Daftar transaksi tidak ditemukan dalam backup.")
        if (categoryArray.length() > 500 || transactionArray.length() > 100_000) throw ApiFailure("Jumlah data pada backup melebihi batas impor.")

        val importedTemplates = mutableListOf<TransactionTemplate>()
        val db = database.writableDatabase
        db.beginTransaction()
        try {
            db.delete("transactions", "user_id=?", arrayOf(userId.toString()))
            db.delete("categories", "user_id=?", arrayOf(userId.toString()))
            val categoryMap = mutableMapOf<Int, Int>()
            for (i in 0 until categoryArray.length()) {
                val item = categoryArray.getJSONObject(i)
                val oldId = item.optInt("old_id", 0)
                val name = item.optString("name").trim()
                val type = item.optString("type")
                if (oldId <= 0 || name.isBlank() || name.length > 100 || type !in listOf("income", "expense")) throw ApiFailure("Data kategori backup tidak valid.")
                val created = item.optString("created_at").ifBlank { nowDateTime() }
                val updated = item.optString("updated_at").ifBlank { created }
                val values = ContentValues().apply {
                    put("user_id", userId); put("name", name); put("type", type); put("is_active", if (item.optInt("is_active", 1) == 1) 1 else 0); put("created_at", created); put("updated_at", updated)
                }
                val newId = db.insertOrThrow("categories", null, values).toInt()
                categoryMap[oldId] = newId
            }
            for (i in 0 until transactionArray.length()) {
                val item = transactionArray.getJSONObject(i)
                val type = item.optString("type")
                val amount = item.optDouble("amount", 0.0)
                val date = item.optString("transaction_date")
                validateDate(date)
                if (type !in listOf("income", "expense") || amount <= 0) throw ApiFailure("Data transaksi backup tidak valid.")
                val oldCategory = if (item.isNull("category_old_id")) null else item.optInt("category_old_id").takeIf { it > 0 }
                val snapshot = item.optString("category_name_snapshot", "Tanpa kategori").take(100).ifBlank { "Tanpa kategori" }
                val description = if (item.isNull("description")) null else item.optString("description").take(255)
                val created = item.optString("created_at").ifBlank { nowDateTime() }
                val values = ContentValues().apply {
                    put("user_id", userId)
                    val mapped = oldCategory?.let(categoryMap::get)
                    if (mapped == null) putNull("category_id") else put("category_id", mapped)
                    put("category_name_snapshot", snapshot); put("type", type); put("amount", amount)
                    if (description == null) putNull("description") else put("description", description)
                    put("transaction_date", date); put("created_at", created)
                }
                db.insertOrThrow("transactions", null, values)
            }
            // Template disimpan dalam payload aplikasi, tidak dalam SQL MySQL lama.
            root.optJSONArray("templates")?.let { backupTemplates ->
                for (i in 0 until minOf(backupTemplates.length(), 20)) {
                    val entry = backupTemplates.optJSONObject(i) ?: continue
                    val mapped = categoryMap[entry.optInt("category_old_id")] ?: continue
                    val name = entry.optString("name").trim().take(40)
                    val type = entry.optString("type")
                    val amount = entry.optString("amount").filter(Char::isDigit).take(15)
                    if (name.length >= 2 && type in listOf("income", "expense") && amount.toLongOrNull()?.let { it > 0 } == true) {
                        importedTemplates += TransactionTemplate(name, type, amount, mapped, entry.optString("description").take(255))
                    }
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        templates.replace(userId, importedTemplates)
    }

    private fun buildXlsx(data: TransactionsData, userName: String, month: String, type: String): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun entry(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(StandardCharsets.UTF_8))
                zip.closeEntry()
            }
            fun inlineCell(ref: String, value: String, style: Int = 0) =
                "<c r=\"$ref\" t=\"inlineStr\" s=\"$style\"><is><t>${xmlEscape(value)}</t></is></c>"
            fun numberCell(ref: String, value: Double, style: Int = 6) =
                "<c r=\"$ref\" s=\"$style\"><v>${"%.2f".format(Locale.US, value)}</v></c>"

            val periodLabel = if (month.isBlank()) "Semua periode" else runCatching { formatMonth(YearMonth.parse(month), false) }.getOrDefault(month)
            val typeLabel = when (type) { "income" -> "Pemasukan"; "expense" -> "Pengeluaran"; else -> "Semua jenis" }
            val rows = StringBuilder()

            rows.append("<row r=\"1\" ht=\"30\" customHeight=\"1\">").append(inlineCell("A1", "SadarBudget — Laporan Transaksi", 1)).append("</row>")
            rows.append("<row r=\"2\">").append(inlineCell("A2", "Pengguna", 2)).append(inlineCell("B2", userName.ifBlank { "Pengguna" }, 9)).append("</row>")
            rows.append("<row r=\"3\">").append(inlineCell("A3", "Periode", 2)).append(inlineCell("B3", periodLabel, 9)).append("</row>")
            rows.append("<row r=\"4\">").append(inlineCell("A4", "Filter jenis", 2)).append(inlineCell("B4", typeLabel, 9)).append("</row>")
            rows.append("<row r=\"5\">").append(inlineCell("A5", "Total pemasukan", 2)).append(numberCell("B5", data.income, 10)).append("</row>")
            rows.append("<row r=\"6\">").append(inlineCell("A6", "Total pengeluaran", 2)).append(numberCell("B6", data.expense, 11)).append("</row>")
            rows.append("<row r=\"7\">").append(inlineCell("A7", "Perubahan saldo transaksi", 2)).append(numberCell("B7", data.net, 12)).append("</row>")
            rows.append("<row r=\"9\" ht=\"22\" customHeight=\"1\">").append(inlineCell("A9", "Nominal adalah nilai transaksi. Saldo berjalan adalah saldo sesudah transaksi diterapkan.", 18)).append("</row>")
            rows.append("<row r=\"10\" ht=\"27\" customHeight=\"1\">")
            listOf("Tanggal", "Kategori", "Keterangan", "Jenis", "Nominal", "Saldo berjalan").forEachIndexed { i, h -> rows.append(inlineCell("${columnName(i + 1)}10", h, 3)) }
            rows.append("</row>")

            data.rows.asReversed().forEachIndexed { index, row ->
                val r = index + 11
                val alt = index % 2 == 1
                val bodyStyle = if (alt) 15 else 4
                val wrapStyle = if (alt) 17 else 8
                val moneyStyle = if (alt) 16 else 6
                val typeStyle = if (row.type == "income") 13 else 14
                rows.append("<row r=\"$r\" ht=\"23\" customHeight=\"1\">")
                rows.append(inlineCell("A$r", row.transactionDate, bodyStyle))
                rows.append(inlineCell("B$r", row.categoryName, bodyStyle))
                rows.append(inlineCell("C$r", row.description?.trim()?.ifBlank { "-" } ?: "-", wrapStyle))
                rows.append(inlineCell("D$r", if (row.type == "income") "Pemasukan" else "Pengeluaran", typeStyle))
                rows.append(numberCell("E$r", kotlin.math.abs(row.amount), moneyStyle))
                rows.append(numberCell("F$r", row.balanceAfter ?: 0.0, if (alt) 16 else 7))
                rows.append("</row>")
            }
            val lastRow = max(10, data.rows.size + 10)

            val worksheetXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetPr><outlinePr summaryBelow="1" summaryRight="1"/></sheetPr>
                  <dimension ref="A1:F$lastRow"/>
                  <sheetViews><sheetView tabSelected="1" workbookViewId="0"><pane ySplit="10" topLeftCell="A11" activePane="bottomLeft" state="frozen"/><selection pane="bottomLeft" activeCell="A11" sqref="A11"/></sheetView></sheetViews>
                  <sheetFormatPr defaultRowHeight="18"/>
                  <cols><col min="1" max="1" width="16" customWidth="1"/><col min="2" max="2" width="24" customWidth="1"/><col min="3" max="3" width="42" customWidth="1"/><col min="4" max="4" width="18" customWidth="1"/><col min="5" max="5" width="19" customWidth="1"/><col min="6" max="6" width="22" customWidth="1"/></cols>
                  <sheetData>$rows</sheetData>
                  <autoFilter ref="A10:F$lastRow"/>
                  <mergeCells count="8"><mergeCell ref="A1:F1"/><mergeCell ref="B2:F2"/><mergeCell ref="B3:F3"/><mergeCell ref="B4:F4"/><mergeCell ref="B5:F5"/><mergeCell ref="B6:F6"/><mergeCell ref="B7:F7"/><mergeCell ref="A9:F9"/></mergeCells>
                  <pageMargins left="0.3" right="0.3" top="0.5" bottom="0.5" header="0.2" footer="0.2"/><pageSetup orientation="landscape" fitToWidth="1" fitToHeight="0" paperSize="9"/>
                </worksheet>""".trimIndent()

            val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <numFmts count="1"><numFmt numFmtId="164" formatCode="&quot;Rp&quot; #,##0;[Red]-&quot;Rp&quot; #,##0;&quot;Rp&quot; 0"/></numFmts>
                  <fonts count="6">
                    <font><sz val="11"/><name val="Calibri"/><family val="2"/><scheme val="minor"/></font>
                    <font><b/><sz val="16"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
                    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
                    <font><b/><sz val="11"/><color rgb="FF172033"/><name val="Calibri"/></font>
                    <font><b/><sz val="11"/><color rgb="FF0284C7"/><name val="Calibri"/></font>
                    <font><b/><sz val="11"/><color rgb="FFDC2626"/><name val="Calibri"/></font>
                  </fonts>
                  <fills count="9">
                    <fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FF1D4ED8"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFF1F5F9"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFDBEAFE"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFE0F2FE"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFFEF2F2"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFEFF6FF"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFF8FAFC"/><bgColor indexed="64"/></patternFill></fill>
                  </fills>
                  <borders count="2"><border><left/><right/><top/><bottom/><diagonal/></border><border><left style="thin"><color rgb="FFD8E0EB"/></left><right style="thin"><color rgb="FFD8E0EB"/></right><top style="thin"><color rgb="FFD8E0EB"/></top><bottom style="thin"><color rgb="FFD8E0EB"/></bottom><diagonal/></border></borders>
                  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
                  <cellXfs count="19">
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0" applyAlignment="1"><alignment vertical="center"/></xf>
                    <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="3" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
                    <xf numFmtId="0" fontId="2" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
                    <xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
                    <xf numFmtId="164" fontId="3" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
                    <xf numFmtId="164" fontId="4" fillId="5" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
                    <xf numFmtId="164" fontId="5" fillId="6" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
                    <xf numFmtId="164" fontId="3" fillId="4" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="4" fillId="5" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="5" fillId="6" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="0" fillId="8" borderId="1" xfId="0" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
                    <xf numFmtId="164" fontId="0" fillId="8" borderId="1" xfId="0" applyNumberFormat="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="0" fillId="8" borderId="1" xfId="0" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
                    <xf numFmtId="0" fontId="3" fillId="7" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
                  </cellXfs>
                  <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
                </styleSheet>""".trimIndent()

            val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/></Types>"""
            val packageRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""
            val workbook = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Laporan" sheetId="1" r:id="rId1"/></sheets></workbook>"""
            val workbookRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>"""

            entry("[Content_Types].xml", contentTypes)
            entry("_rels/.rels", packageRels)
            entry("xl/workbook.xml", workbook)
            entry("xl/_rels/workbook.xml.rels", workbookRels)
            entry("xl/styles.xml", stylesXml)
            entry("xl/worksheets/sheet1.xml", worksheetXml)
        }
        return out.toByteArray()
    }

    private fun columnName(index: Int): String {
        var n = index
        val sb = StringBuilder()
        while (n > 0) { n--; sb.append(('A'.code + n % 26).toChar()); n /= 26 }
        return sb.reverse().toString()
    }

    private fun xmlEscape(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")

    private fun formatFileSize(bytes: Long): String = when {
        bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / 1024f / 1024f)
        bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024f)
        else -> "$bytes B"
    }

    companion object {
        private const val MAX_BACKUPS = 5
        private const val AUTO_BACKUP_INTERVAL_MINUTES = 0
    }
}
