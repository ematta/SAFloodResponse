/**
 * This file previously contained mapping functions between local database entities and Firebase models.
 *
 * Since the app now exclusively uses FirebaseUser and FirestoreUser, the old mappings to UserEntity
 * have been removed. This file is kept as a placeholder for any future mapping utilities.
 */
package edu.utap.auth.repository

import com.google.firebase.auth.FirebaseUser
// Removed import edu.utap.db.UserEntity
import edu.utap.utils.RoleUtils

// Removed mapping functions as UserEntity is being replaced by FirebaseUser
