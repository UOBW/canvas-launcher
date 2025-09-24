package io.github.canvas.data.contacts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import io.github.canvas.data.HideableSearchResult
import io.github.canvas.data.RenameableSearchResult
import io.github.canvas.data.SearchResult
import io.github.canvas.data.StringLabelSearchResult
import io.github.canvas.data.Uid
import io.github.canvas.data.application
import io.github.canvas.data.icons.AdaptiveIcon
import io.github.canvas.data.log
import java.lang.String.CASE_INSENSITIVE_ORDER

data class Contact(
    /** The contact id currently used by the system */
    val id: Long,
    /** A more stable identifier of the contact */
    val lookupKey: String,
    override val label: String,
    override val icon: AdaptiveIcon,
    override val badgeIcon: AdaptiveIcon?,
    /** Whether the user has starred the contacts in the contacts app, starred contacts will be sorted first */
    val isStarred: Boolean,
    /** The priority of the contact set in the contacts app, higher values mean higher priority */
    val priority: Int,
    override val originalLabelOrNull: String?,
) : SearchResult, StringLabelSearchResult,
    RenameableSearchResult, HideableSearchResult, Comparable<Contact> {
    override val uid: Uid = Uid("contact/$lookupKey/$id")

    override val searchTokens: List<String> = label.split(' ')

    override fun open(context: Context, options: Bundle) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = ContactsContract.Contacts.CONTENT_LOOKUP_URI.buildUpon()
                .appendEncodedPath(lookupKey).appendEncodedPath("$id").build()
        }
        context.startActivity(intent, options)
        log.d("Opened contact details for contact $this")
    }

    override fun compareTo(other: Contact): Int = when {
        this.isStarred != other.isStarred -> this.isStarred compareTo other.isStarred
        this.priority != other.priority -> this.priority compareTo other.priority
        else -> CASE_INSENSITIVE_ORDER.compare(this.label, other.label)
    }

    override fun renameAsync(newName: String, context: Context): Unit =
        context.application.contactsRepository.renameAsync(this, newName)

    override fun setHiddenAsync(value: Boolean, context: Context): Unit =
        context.application.contactsRepository.setHiddenAsync(this, value)

    override fun toString(): String = "contact/$lookupKey/$id ($label)"
}
