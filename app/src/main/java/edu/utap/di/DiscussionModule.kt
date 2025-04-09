package edu.utap.di

import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.repository.DiscussionRepositoryInterface
import edu.utap.repository.FirestoreDiscussionRepository

/**
 * Simple singleton dependency provider for the discussion repository.
 *
 * This avoids tight coupling and allows for easier testing or replacement.
 * In a production app, this would typically be replaced by a DI framework like Hilt or Dagger.
 */
object DiscussionModule {

    private var discussionRepository: DiscussionRepositoryInterface? = null

    /**
     * Provides a singleton instance of [DiscussionRepositoryInterface].
     *
     * Lazily initializes the repository with Firestore.
     * Thread-safe via `synchronized`.
     *
     * @return The singleton [DiscussionRepositoryInterface] implementation.
     */
    fun provideDiscussionRepository(): DiscussionRepositoryInterface =
        discussionRepository ?: synchronized(this) {
            val firestore = FirebaseFirestore.getInstance()
            FirestoreDiscussionRepository(firestore).also {
                discussionRepository = it
            }
        }
}
