package com.example.data

import kotlinx.coroutines.flow.Flow

class DongRepository(private val dongDao: DongDao) {
    // --- GROUPS ---
    val allGroups: Flow<List<DongGroup>> = dongDao.getAllGroupsFlow()

    suspend fun getGroupById(groupId: Int): DongGroup? = dongDao.getGroupById(groupId)

    suspend fun insertGroup(group: DongGroup): Long = dongDao.insertGroup(group)

    suspend fun updateGroup(group: DongGroup) = dongDao.updateGroup(group)

    suspend fun deleteGroup(group: DongGroup) = dongDao.deleteGroup(group)

    // --- MEMBERS ---
    fun getMembersByGroup(groupId: Int): Flow<List<GroupMember>> = dongDao.getMembersByGroupFlow(groupId)

    suspend fun getMembersByGroupList(groupId: Int): List<GroupMember> = dongDao.getMembersByGroup(groupId)

    suspend fun insertMember(member: GroupMember): Long = dongDao.insertMember(member)

    suspend fun deleteMember(member: GroupMember) = dongDao.deleteMember(member)

    // --- EXPENSES ---
    val allExpenses: Flow<List<Expense>> = dongDao.getAllExpensesFlow()

    fun getExpensesByGroup(groupId: Int): Flow<List<Expense>> = dongDao.getExpensesByGroupFlow(groupId)

    suspend fun getExpensesByGroupList(groupId: Int): List<Expense> = dongDao.getExpensesByGroup(groupId)

    suspend fun insertExpense(expense: Expense): Long = dongDao.insertExpense(expense)

    suspend fun updateExpense(expense: Expense) = dongDao.updateExpense(expense)

    suspend fun deleteExpense(expense: Expense) = dongDao.deleteExpense(expense)

    // --- SETTLEMENTS ---
    fun getSettlementsByGroup(groupId: Int): Flow<List<Settlement>> = dongDao.getSettlementsByGroupFlow(groupId)

    suspend fun getSettlementsByGroupList(groupId: Int): List<Settlement> = dongDao.getSettlementsByGroup(groupId)

    suspend fun insertSettlement(settlement: Settlement): Long = dongDao.insertSettlement(settlement)

    suspend fun updateSettlement(settlement: Settlement) = dongDao.updateSettlement(settlement)

    suspend fun deleteSettlement(settlement: Settlement) = dongDao.deleteSettlement(settlement)
}
