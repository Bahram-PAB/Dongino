package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.min

data class GroupSummaryState(
    val totalExpenses: Long = 0,
    val sharePerPerson: Long = 0,
    val memberBalances: List<MemberBalance> = emptyList(),
    val suggestions: List<SettlementSuggestion> = emptyList()
)

data class MemberBalance(
    val member: GroupMember,
    val netBalance: Long // positive (creditor/green) or negative (debtor/red)
)

data class SettlementSuggestion(
    val fromMember: GroupMember,
    val toMember: GroupMember,
    val amount: Long
)

@OptIn(ExperimentalCoroutinesApi::class)
class DongViewModel(
    application: Application,
    private val repository: DongRepository
) : AndroidViewModel(application) {

    // --- SELECTION STATE ---
    private val _selectedGroupId = MutableStateFlow<Int>(-1)
    val selectedGroupId: StateFlow<Int> = _selectedGroupId.asStateFlow()

    // --- ALL GROUPS ---
    val allGroups: StateFlow<List<DongGroup>> = repository.allGroups
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- RECTIVE DATA FLOW BASED ON SELECTED GROUP ---
    val activeGroup: StateFlow<DongGroup?> = _selectedGroupId
        .flatMapLatest { id ->
            if (id == -1) {
                flowOf<DongGroup?>(null)
            } else {
                flow {
                    emit(repository.getGroupById(id))
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val activeMembers: StateFlow<List<GroupMember>> = _selectedGroupId
        .flatMapLatest { id ->
            if (id == -1) flowOf(emptyList()) else repository.getMembersByGroup(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeExpenses: StateFlow<List<Expense>> = _selectedGroupId
        .flatMapLatest { id ->
            if (id == -1) flowOf(emptyList()) else repository.getExpensesByGroup(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeSettlements: StateFlow<List<Settlement>> = _selectedGroupId
        .flatMapLatest { id ->
            if (id == -1) flowOf(emptyList()) else repository.getSettlementsByGroup(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- MAIN RETURNING STATE CONTROLLER FOR SUMMARY CARD, BALANCES AND SUGGESTIONS ---
    val groupSummaryState: StateFlow<GroupSummaryState> = combine(
        activeMembers,
        activeExpenses,
        activeSettlements
    ) { members, expenses, settlements ->
        if (members.isEmpty()) return@combine GroupSummaryState()

        val netBalances = members.associate { it.id to 0L }.toMutableMap()

        // 1. Compute costs from active expenses
        for (expense in expenses) {
            val payer = expense.paidByMemberId
            val amount = expense.amount

            // Parse beneficiary IDs
            val beneficiaryIds = expense.beneficiaryMemberIds.split(",")
                .filter { it.isNotEmpty() }
                .mapNotNull { it.toIntOrNull() }
                .filter { id -> members.any { it.id == id } }

            val beneficiaries = if (beneficiaryIds.isEmpty()) {
                members.map { it.id }
            } else {
                beneficiaryIds
            }

            if (beneficiaries.isNotEmpty()) {
                val portion = amount / beneficiaries.size

                // Increase credit of payer
                netBalances[payer] = (netBalances[payer] ?: 0L) + amount

                // Decrease credit of beneficiaries (they owe)
                for (bId in beneficiaries) {
                    netBalances[bId] = (netBalances[bId] ?: 0L) - portion
                }
            }
        }

        // 2. Adjust with Recorded Settlements (Real payments)
        for (settlement in settlements) {
            if (settlement.isSettled) {
                val fromId = settlement.fromMemberId
                val toId = settlement.toMemberId
                val amount = settlement.amount

                // From member paid, so their absolute negative balance is reduced
                netBalances[fromId] = (netBalances[fromId] ?: 0L) + amount
                // To member received, so their credit reduces
                netBalances[toId] = (netBalances[toId] ?: 0L) - amount
            }
        }

        // Totals
        val totalExpSum = expenses.sumOf { it.amount }
        val sharePerPersonValue = if (members.isNotEmpty()) totalExpSum / members.size else 0L

        // Construct Balances
        val memberBalancesList = members.map { member ->
            MemberBalance(member = member, netBalance = netBalances[member.id] ?: 0L)
        }

        // Minimal Settlement Pairing Algorithm
        val debtorsList = memberBalancesList.filter { it.netBalance < 0 }
            .map { it.member to abs(it.netBalance) }
            .toMutableList()

        val creditorsList = memberBalancesList.filter { it.netBalance > 0 }
            .map { it.member to it.netBalance }
            .toMutableList()

        val suggestionsList = mutableListOf<SettlementSuggestion>()

        while (debtorsList.isNotEmpty() && creditorsList.isNotEmpty()) {
            val debtor = debtorsList.first()
            val creditor = creditorsList.first()

            val settleVal = min(debtor.second, creditor.second)
            if (settleVal > 0L) {
                suggestionsList.add(
                    SettlementSuggestion(
                        fromMember = debtor.first,
                        toMember = creditor.first,
                        amount = settleVal
                    )
                )
            }

            val remDebt = debtor.second - settleVal
            val remCredit = creditor.second - settleVal

            if (remDebt <= 0L) {
                debtorsList.removeAt(0)
            } else {
                debtorsList[0] = Pair(debtor.first, remDebt)
            }

            if (remCredit <= 0L) {
                creditorsList.removeAt(0)
            } else {
                creditorsList[0] = Pair(creditor.first, remCredit)
            }
        }

        GroupSummaryState(
            totalExpenses = totalExpSum,
            sharePerPerson = sharePerPersonValue,
            memberBalances = memberBalancesList,
            suggestions = suggestionsList
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GroupSummaryState()
    )

    // --- ACTIONS & OPERATIONS ---

    fun setSelectedGroup(groupId: Int) {
        _selectedGroupId.value = groupId
    }

    fun createGroup(
        name: String,
        description: String?,
        startDate: String?,
        endDate: String?,
        memberNames: List<String>
    ) {
        viewModelScope.launch {
            val groupId = repository.insertGroup(
                DongGroup(
                    name = name,
                    description = description,
                    startDate = startDate,
                    endDate = endDate
                )
            ).toInt()

            for (memberName in memberNames) {
                if (memberName.isNotBlank()) {
                    repository.insertMember(GroupMember(groupId = groupId, name = memberName.trim()))
                }
            }
            setSelectedGroup(groupId)
        }
    }

    fun updateGroup(group: DongGroup) {
        viewModelScope.launch {
            repository.updateGroup(group)
        }
    }

    fun deleteGroup(group: DongGroup) {
        viewModelScope.launch {
            repository.deleteGroup(group)
            if (_selectedGroupId.value == group.id) {
                _selectedGroupId.value = -1
            }
        }
    }

    fun addMember(name: String) {
        val gid = _selectedGroupId.value
        if (gid != -1 && name.isNotBlank()) {
            viewModelScope.launch {
                repository.insertMember(GroupMember(groupId = gid, name = name.trim()))
            }
        }
    }

    fun removeMember(member: GroupMember) {
        viewModelScope.launch {
            repository.deleteMember(member)
        }
    }

    fun saveExpense(
        id: Int = 0,
        amount: Long,
        description: String,
        paidById: Int,
        beneficiaryIds: List<Int>,
        category: String,
        date: Long,
        imageUri: Uri?,
        existingPhotoPath: String? = null
    ) {
        val gid = _selectedGroupId.value
        if (gid == -1) return

        viewModelScope.launch {
            var savedPath: String? = existingPhotoPath
            if (imageUri != null) {
                savedPath = copyUriToInternalStorage(imageUri)
            }

            val expense = Expense(
                id = id,
                groupId = gid,
                amount = amount,
                description = description.trim(),
                paidByMemberId = paidById,
                beneficiaryMemberIds = beneficiaryIds.joinToString(","),
                category = category,
                date = date,
                invoicePhotoPath = savedPath
            )
            repository.insertExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            // Optional: delete associated local file from system
            expense.invoicePhotoPath?.let { path ->
                try {
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun addSettlement(fromId: Int, toId: Int, amount: Long) {
        val gid = _selectedGroupId.value
        if (gid == -1) return
        viewModelScope.launch {
            repository.insertSettlement(
                Settlement(
                    groupId = gid,
                    fromMemberId = fromId,
                    toMemberId = toId,
                    amount = amount
                )
            )
        }
    }

    fun deleteSettlement(settlement: Settlement) {
        viewModelScope.launch {
            repository.deleteSettlement(settlement)
        }
    }

    // --- PHOTO IMPORT UTILITY ---
    private fun copyUriToInternalStorage(uri: Uri): String? {
        val context = getApplication<Application>().applicationContext
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val fileName = "rec_${System.currentTimeMillis()}.jpg"
                val file = File(context.filesDir, fileName)
                val outputStream = FileOutputStream(file)
                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                file.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

class DongViewModelFactory(
    private val application: Application,
    private val repository: DongRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DongViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DongViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
