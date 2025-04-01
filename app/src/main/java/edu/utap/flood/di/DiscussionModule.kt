package edu.utap.flood.di

import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.flood.repository.DiscussionRepositoryInterface
import edu.utap.flood.repository.FirestoreDiscussionRepository

/**
 * A simple dependency provider for the DiscussionRepository
 */
object DiscussionModule {

    private var discussionRepository: DiscussionRepositoryInterface? = null

    fun provideDiscussionRepository(): DiscussionRepositoryInterface =
        discussionRepository ?: synchronized(this) {
            val firestore = FirebaseFirestore.getInstance()
            FirestoreDiscussionRepository(firestore).also {
                discussionRepository = it
            }
        }
}
