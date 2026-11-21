package dev.ahmeddawood.elghassala.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "days", indices = [Index(value = ["title"], unique = true)])
data class DayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dateMillis: Long,
    val isClosed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "people", indices = [Index(value = ["name"], unique = true)])
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val origin: String,
    val destination: String,
    val defaultFareCents: Long,
    val usageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null,
)

@Entity(tableName = "transports", indices = [Index("dayId")])
data class TransportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: Long?,
    val origin: String,
    val destination: String,
    val fareCents: Long,
    val dateMillis: Long,
    val notes: String = "",
    val routeId: Long? = null,
)

@Entity(tableName = "transport_participants", indices = [Index("transportId"), Index("personId")])
data class TransportParticipantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transportId: Long,
    val personId: Long,
    val paidCents: Long,
)

@Entity(tableName = "driver_refunds", indices = [Index("transportId"), Index("personId")])
data class DriverRefundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transportId: Long,
    val personId: Long,
    val amountCents: Long,
    val collected: Boolean = false,
    val collectedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "expenses", indices = [Index("dayId")])
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: Long?,
    val title: String,
    val totalCents: Long,
    val dateMillis: Long,
    val notes: String = "",
)

@Entity(tableName = "expense_payers", indices = [Index("expenseId"), Index("personId")])
data class ExpensePayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expenseId: Long,
    val personId: Long,
    val paidCents: Long,
)

@Entity(tableName = "expense_beneficiaries", indices = [Index("expenseId"), Index("personId")])
data class ExpenseBeneficiaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expenseId: Long,
    val personId: Long,
    val shareCents: Long,
)

enum class TransferKind { TRANSFER, SETTLEMENT }

@Entity(tableName = "transfers", indices = [Index("dayId"), Index("fromPersonId"), Index("toPersonId")])
data class TransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: Long?,
    val fromPersonId: Long,
    val toPersonId: Long,
    val amountCents: Long,
    val kind: TransferKind = TransferKind.TRANSFER,
    val dateMillis: Long,
    val notes: String = "",
)
