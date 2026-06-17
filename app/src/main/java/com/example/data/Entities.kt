package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "groups")
data class DongGroup(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val startDate: String? = null,
    val endDate: String? = null,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "members",
    foreignKeys = [
        ForeignKey(
            entity = DongGroup::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["groupId"])]
)
data class GroupMember(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val name: String
)

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = DongGroup::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["groupId"])]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val amount: Long, // in Toman
    val description: String,
    val paidByMemberId: Int,
    val beneficiaryMemberIds: String, // Comma separated, e.g. "1,2,3"
    val category: String, // e.g. غذا، حمل‌ونقل، اقامت، متفرقه
    val date: Long = System.currentTimeMillis(),
    val invoicePhotoPath: String? = null
)

@Entity(
    tableName = "settlements",
    foreignKeys = [
        ForeignKey(
            entity = DongGroup::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["groupId"])]
)
data class Settlement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val fromMemberId: Int, // borrower
    val toMemberId: Int, // lender
    val amount: Long,
    val date: Long = System.currentTimeMillis(),
    val isSettled: Boolean = true
)
