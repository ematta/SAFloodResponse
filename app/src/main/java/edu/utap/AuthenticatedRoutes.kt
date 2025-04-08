package edu.utap

/**
 * Object containing navigation route constants for authenticated parts of the app.
 *
 * These routes are used with the Navigation component to navigate between screens.
 */
object AuthenticatedRoutes {
    /** Route for the dashboard screen. */
    const val DASHBOARD = "dashboard"

    /** Route for the discussions list screen. */
    const val DISCUSSIONS = "discussions"

    /** Route pattern for a specific discussion thread screen. */
    const val DISCUSSIONS_THREAD = "discussions/{threadId}"

    /** Route for the user profile screen. */
    const val PROFILE = "profile"

    /** Route for the flood report submission screen. */
    const val FLOOD_REPORT = "flood_report"
}
