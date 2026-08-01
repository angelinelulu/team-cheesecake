package com.teamcheesecake.doomscrollpet.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class FriendLink(
    val id: String,
    val codeA: String,
    val codeB: String,
    val nameA: String,
    val nameB: String,
    val status: String, // "pending" | "accepted"
    val requestedBy: String,
) {
    fun otherCode(myCode: String): String = if (codeA == myCode) codeB else codeA
    fun otherName(myCode: String): String = if (codeA == myCode) nameB else nameA
    fun isIncomingFor(myCode: String): Boolean = status == "pending" && requestedBy != myCode
    fun isOutgoingFor(myCode: String): Boolean = status == "pending" && requestedBy == myCode
    fun isAccepted(): Boolean = status == "accepted"
}

/**
 * Manages friend requests as "links" — one Firestore document per pair of codes, keyed by
 * the two codes sorted alphabetically (so both sides always resolve to the same document id,
 * regardless of who's looking it up). A link starts "pending" when one side sends a request,
 * and becomes "accepted" once the other side approves it.
 */
class FriendRepository {
    private val linksCollection = FirebaseFirestore.getInstance().collection("friend_links")

    private fun linkId(codeA: String, codeB: String): String =
        listOf(codeA, codeB).sorted().joinToString("_")

    /** Sends a friend request. No-ops if a link (pending or accepted) already exists. */
    suspend fun sendRequest(myCode: String, myName: String, targetCode: String) {
        val normalizedTarget = targetCode.trim().uppercase()
        val normalizedMe = myCode.trim().uppercase()
        if (normalizedTarget.isBlank() || normalizedTarget == normalizedMe) return

        val id = linkId(normalizedMe, normalizedTarget)
        val doc = linksCollection.document(id)
        val existing = doc.get().await()
        if (existing.exists()) return // already pending or already friends

        val sorted = listOf(normalizedMe, normalizedTarget).sorted()
        doc.set(
            mapOf(
                "codeA" to sorted[0],
                "codeB" to sorted[1],
                "nameA" to if (sorted[0] == normalizedMe) myName else "",
                "nameB" to if (sorted[1] == normalizedMe) myName else "",
                "status" to "pending",
                "requestedBy" to normalizedMe,
                "createdAt" to System.currentTimeMillis(),
            )
        ).await()
    }

    /** Accepts a pending incoming request from [otherCode]. */
    suspend fun acceptRequest(myCode: String, otherCode: String, myName: String) {
        val normalizedMe = myCode.trim().uppercase()
        val normalizedOther = otherCode.trim().uppercase()
        val doc = linksCollection.document(linkId(normalizedMe, normalizedOther))
        val snapshot = doc.get().await()
        if (!snapshot.exists()) return
        val codeA = snapshot.getString("codeA") ?: return
        val nameField = if (codeA == normalizedMe) "nameA" else "nameB"
        doc.set(
            mapOf("status" to "accepted", nameField to myName),
            SetOptions.merge(),
        ).await()
    }

    /** Declines a pending incoming request, or removes an existing friendship. */
    suspend fun removeLink(myCode: String, otherCode: String) {
        val normalizedMe = myCode.trim().uppercase()
        val normalizedOther = otherCode.trim().uppercase()
        linksCollection.document(linkId(normalizedMe, normalizedOther)).delete().await()
    }

    /**
     * Listens for every link involving [myCode] — pending (incoming/outgoing) and accepted.
     * Firestore can't OR-query across two different fields in one listener reliably across
     * SDK versions, so this runs two listeners (codeA == myCode, codeB == myCode) and merges
     * their results into one combined list on every update.
     */
    fun listenToLinks(myCode: String, onUpdate: (List<FriendLink>) -> Unit): ListenerRegistration {
        val normalizedMe = myCode.trim().uppercase()
        var lastA: List<FriendLink> = emptyList()
        var lastB: List<FriendLink> = emptyList()

        fun emit() {
            val merged = (lastA + lastB).associateBy { it.id }.values.toList()
            onUpdate(merged)
        }

        fun parse(snapshot: QuerySnapshot?): List<FriendLink> =
            snapshot?.documents?.mapNotNull { doc ->
                val codeA = doc.getString("codeA") ?: return@mapNotNull null
                val codeB = doc.getString("codeB") ?: return@mapNotNull null
                FriendLink(
                    id = doc.id,
                    codeA = codeA,
                    codeB = codeB,
                    nameA = doc.getString("nameA") ?: "",
                    nameB = doc.getString("nameB") ?: "",
                    status = doc.getString("status") ?: "pending",
                    requestedBy = doc.getString("requestedBy") ?: "",
                )
            } ?: emptyList()

        val regA = linksCollection.whereEqualTo("codeA", normalizedMe)
            .addSnapshotListener { snap, _ -> lastA = parse(snap); emit() }
        val regB = linksCollection.whereEqualTo("codeB", normalizedMe)
            .addSnapshotListener { snap, _ -> lastB = parse(snap); emit() }

        return object : ListenerRegistration {
            override fun remove() {
                regA.remove()
                regB.remove()
            }
        }
    }
}