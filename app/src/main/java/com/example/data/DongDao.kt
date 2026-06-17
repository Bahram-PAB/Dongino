package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DongDao {
    // --- GROUPS ---
    @Query("SELECT * FROM groups ORDER BY createdAt DESC")
    fun getAllGroupsFlow(): Flow<List<DongGroup>>

    @Query("SELECT * FROM groups WHERE id = :groupId LIMIT 1")
    suspend fun getGroupById(groupId: Int): DongGroup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: DongGroup): Long

    @Update
    suspend fun updateGroup(group: DongGroup)

    @Delete
    suspend fun deleteGroup(group: DongGroup)

    // --- MEMBERS ---
    @Query("SELECT * FROM members WHERE groupId = :groupId")
    fun getMembersByGroupFlow(groupId: Int): Flow<List<GroupMember>>

    @Query("SELECT * FROM members WHERE groupId = :groupId")
    suspend fun getMembersByGroup(groupId: Int): List<GroupMember>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: GroupMember): Long

    @Delete
    suspend fun deleteMember(member: GroupMember)

    // --- EXPENSES ---
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpensesFlow(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY date DESC")
    fun getExpensesByGroupFlow(groupId: Int): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY date DESC")
    suspend fun getExpensesByGroup(groupId: Int): List<Expense>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    // --- SETTLEMENTS ---
    @Query("SELECT * FROM settlements WHERE groupId = :groupId ORDER BY date DESC")
    fun getSettlementsByGroupFlow(groupId: Int): Flow<List<Settlement>>

    @Query("SELECT * FROM settlements WHERE groupId = :groupId ORDER BY date DESC")
    suspend fun getSettlementsByGroup(groupId: Int): List<Settlement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: Settlement): Long

    @Update
    suspend fun updateSettlement(settlement: Settlement)

    @Delete
    suspend fun deleteSettlement(settlement: Settlement)
}
