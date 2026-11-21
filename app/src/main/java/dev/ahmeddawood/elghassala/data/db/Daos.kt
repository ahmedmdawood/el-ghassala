package dev.ahmeddawood.elghassala.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DayDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(day: DayEntity): Long

    @Update
    suspend fun update(day: DayEntity)

    @Query("SELECT * FROM days ORDER BY dateMillis DESC")
    fun observeAll(): Flow<List<DayEntity>>

    @Query("SELECT * FROM days WHERE id = :id")
    suspend fun getById(id: Long): DayEntity?

    @Query("SELECT * FROM days WHERE id = :id")
    fun observeById(id: Long): Flow<DayEntity?>

    @Query("DELETE FROM days WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM days ORDER BY dateMillis DESC")
    suspend fun all(): List<DayEntity>

    @Query("SELECT COUNT(*) FROM days WHERE title = :title AND id != :excludeId")
    suspend fun countWithTitle(title: String, excludeId: Long): Int
}

@Dao
interface PersonDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(person: PersonEntity): Long

    @Update
    suspend fun update(person: PersonEntity)

    @Query("SELECT * FROM people ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people WHERE NOT archived ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people WHERE id = :id")
    suspend fun getById(id: Long): PersonEntity?

    @Query("SELECT * FROM people ORDER BY name COLLATE NOCASE")
    suspend fun all(): List<PersonEntity>

    @Query("SELECT COUNT(*) FROM people WHERE name = :name AND id != :excludeId")
    suspend fun countWithName(name: String, excludeId: Long): Int
}

@Dao
interface RouteDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(route: RouteEntity): Long

    @Update
    suspend fun update(route: RouteEntity)

    @Query("SELECT * FROM routes ORDER BY usageCount DESC, lastUsedAt DESC")
    fun observeAll(): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE id = :id")
    suspend fun getById(id: Long): RouteEntity?

    @Query("DELETE FROM routes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM routes ORDER BY usageCount DESC")
    suspend fun all(): List<RouteEntity>

    @Query("SELECT COUNT(*) FROM routes WHERE lower(origin) = lower(:origin) AND lower(destination) = lower(:destination) AND id != :excludeId")
    suspend fun countSimilar(origin: String, destination: String, excludeId: Long): Int
}

@Dao
interface TransportDao {
    @Insert
    suspend fun insert(transport: TransportEntity): Long

    @Update
    suspend fun update(transport: TransportEntity)

    @Insert
    suspend fun insertParticipants(participants: List<TransportParticipantEntity>)

    @Query("DELETE FROM transport_participants WHERE transportId = :transportId")
    suspend fun deleteParticipants(transportId: Long)

    @Query("SELECT * FROM transports ORDER BY dateMillis DESC")
    fun observeAll(): Flow<List<TransportEntity>>

    @Query("SELECT * FROM transports WHERE dayId = :dayId ORDER BY dateMillis DESC")
    fun observeForDay(dayId: Long): Flow<List<TransportEntity>>

    @Query("SELECT * FROM transports WHERE id = :id")
    suspend fun getById(id: Long): TransportEntity?

    @Query("SELECT * FROM transport_participants WHERE transportId = :transportId")
    suspend fun participantsFor(transportId: Long): List<TransportParticipantEntity>

    @Query("DELETE FROM transports WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transports ORDER BY dateMillis DESC")
    suspend fun all(): List<TransportEntity>

    @Query("SELECT * FROM transport_participants")
    suspend fun allParticipants(): List<TransportParticipantEntity>

    @Query("SELECT * FROM transport_participants")
    fun observeAllParticipants(): Flow<List<TransportParticipantEntity>>

    @Query("SELECT * FROM transport_participants WHERE personId = :personId")
    suspend fun participantsForPerson(personId: Long): List<TransportParticipantEntity>
}

@Dao
interface RefundDao {
    @Insert
    suspend fun insert(refund: DriverRefundEntity): Long

    @Insert
    suspend fun insertAll(refunds: List<DriverRefundEntity>)

    @Update
    suspend fun update(refund: DriverRefundEntity)

    @Query("SELECT * FROM driver_refunds ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DriverRefundEntity>>

    @Query("SELECT * FROM driver_refunds WHERE transportId = :transportId")
    suspend fun forTransport(transportId: Long): List<DriverRefundEntity>

    @Query("DELETE FROM driver_refunds WHERE transportId = :transportId")
    suspend fun deleteForTransport(transportId: Long)

    @Query("SELECT * FROM driver_refunds ORDER BY createdAt DESC")
    suspend fun all(): List<DriverRefundEntity>

    @Query("SELECT * FROM driver_refunds WHERE id = :id")
    suspend fun getById(id: Long): DriverRefundEntity?
}

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Insert
    suspend fun insertPayers(payers: List<ExpensePayerEntity>)

    @Insert
    suspend fun insertBeneficiaries(beneficiaries: List<ExpenseBeneficiaryEntity>)

    @Query("DELETE FROM expense_payers WHERE expenseId = :expenseId")
    suspend fun deletePayers(expenseId: Long)

    @Query("DELETE FROM expense_beneficiaries WHERE expenseId = :expenseId")
    suspend fun deleteBeneficiaries(expenseId: Long)

    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE dayId = :dayId ORDER BY dateMillis DESC")
    fun observeForDay(dayId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expense_payers WHERE expenseId = :expenseId")
    suspend fun payersFor(expenseId: Long): List<ExpensePayerEntity>

    @Query("SELECT * FROM expense_beneficiaries WHERE expenseId = :expenseId")
    suspend fun beneficiariesFor(expenseId: Long): List<ExpenseBeneficiaryEntity>

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC")
    suspend fun all(): List<ExpenseEntity>

    @Query("SELECT * FROM expense_payers")
    suspend fun allPayers(): List<ExpensePayerEntity>

    @Query("SELECT * FROM expense_payers")
    fun observeAllPayers(): Flow<List<ExpensePayerEntity>>

    @Query("SELECT * FROM expense_beneficiaries")
    suspend fun allBeneficiaries(): List<ExpenseBeneficiaryEntity>

    @Query("SELECT * FROM expense_beneficiaries")
    fun observeAllBeneficiaries(): Flow<List<ExpenseBeneficiaryEntity>>
}

@Dao
interface TransferDao {
    @Insert
    suspend fun insert(transfer: TransferEntity): Long

    @Update
    suspend fun update(transfer: TransferEntity)

    @Query("SELECT * FROM transfers ORDER BY dateMillis DESC")
    fun observeAll(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers WHERE dayId = :dayId ORDER BY dateMillis DESC")
    fun observeForDay(dayId: Long): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers WHERE id = :id")
    suspend fun getById(id: Long): TransferEntity?

    @Query("DELETE FROM transfers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transfers ORDER BY dateMillis DESC")
    suspend fun all(): List<TransferEntity>
}
