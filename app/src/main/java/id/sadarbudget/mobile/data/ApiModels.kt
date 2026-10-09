package id.sadarbudget.mobile.data

data class User(
    val id: Int = 0,
    val name: String = "",
    val email: String = "",
    val profilePhoto: String? = null,
    val passwordChangedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class AuthData(val token: String = "", val user: User = User())

data class ChartPoint(
    val label: String = "",
    val balance: Double = 0.0,
    val income: Double = 0.0,
    val expense: Double = 0.0,
)

data class Transaction(
    val id: Long = 0,
    val transactionDate: String = "",
    val createdAt: String? = null,
    val type: String = "",
    val amount: Double = 0.0,
    val description: String? = null,
    val categoryName: String = "Tanpa kategori",
    val categoryId: Int? = null,
    val balanceAfter: Double? = null,
)

data class DashboardData(
    val balance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val monthLabel: String = "",
    val expenseRatio: Double? = null,
    val expenseRatioProgress: Double = 0.0,
    val expenseRatioState: String = "neutral",
    val expenseRatioNote: String = "",
    val chart: List<ChartPoint> = emptyList(),
    val recent: List<Transaction> = emptyList(),
)

data class TransactionsData(
    val rows: List<Transaction> = emptyList(),
    val page: Int = 1,
    val perPage: Int = 12,
    val total: Int = 0,
    val totalPages: Int = 1,
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val net: Double = 0.0,
    val currentBalance: Double = 0.0,
    val month: String = "",
    val type: String = "",
)

data class Category(
    val id: Int = 0,
    val name: String = "",
    val type: String = "",
    val isActive: Boolean = true,
    val transactionCount: Int = 0,
)

data class TransactionGetData(val transaction: Transaction = Transaction(), val categories: List<Category> = emptyList())

data class TransactionSaveRequest(
    val id: Long? = null,
    val type: String? = null,
    val amount: String,
    val transactionDate: String,
    val categoryId: Int?,
    val description: String,
)

data class AnnualMonth(
    val month: Int = 0,
    val label: String = "",
    val fullLabel: String = "",
    val hasActivity: Boolean = false,
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val moneyNet: Double = 0.0,
    val assetNet: Double = 0.0,
)

data class AnnualTotals(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val moneyNet: Double = 0.0,
    val assetNet: Double = 0.0,
)

data class TopCategory(val categoryName: String = "", val totalAmount: Double = 0.0)

data class AnnualRecap(
    val year: Int = 0,
    val months: List<AnnualMonth> = emptyList(),
    val totals: AnnualTotals = AnnualTotals(),
    val topCategories: List<TopCategory> = emptyList(),
    val activeMonths: Int = 0,
    val bestMonth: AnnualMonth? = null,
)

data class AnnualData(val availableYears: List<Int> = emptyList(), val recap: AnnualRecap = AnnualRecap())

data class ProfileData(val user: User = User())

data class DataCounts(val transactions: Int = 0, val categories: Int = 0)

data class BackupItem(
    val filename: String = "",
    val createdAt: String? = null,
    val reason: String? = null,
    val reasonLabel: String? = null,
    val size: Long = 0,
)

data class AutoBackupStatus(
    val enabled: Boolean = true,
    val count: Int = 0,
    val maxFiles: Int = 5,
    val totalSize: Long = 0,
    val totalSizeLabel: String = "0 B",
    val intervalMinutes: Int = 0,
    val latest: BackupItem? = null,
    val items: List<BackupItem> = emptyList(),
    val lastError: String? = null,
)

data class DataStatus(val counts: DataCounts = DataCounts(), val autoBackup: AutoBackupStatus = AutoBackupStatus())
