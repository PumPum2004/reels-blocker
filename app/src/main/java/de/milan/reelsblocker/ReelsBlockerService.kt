package de.milan.reelsblocker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ReelsBlockerService : AccessibilityService() {

    private var lastAction = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != INSTAGRAM) return

        val root = rootInActiveWindow ?: return
        if (!isInReels(root)) return

        val now = System.currentTimeMillis()
        if (now - lastAction < DEBOUNCE_MS) return
        lastAction = now

        leaveReels(root)
    }

    override fun onInterrupt() = Unit

    private fun isInReels(root: AccessibilityNodeInfo): Boolean {
        for (id in VIEWER_IDS) {
            if (root.findAccessibilityNodeInfosByViewId("$INSTAGRAM:id/$id").isNotEmpty()) return true
        }
        for (id in REELS_TAB_IDS) {
            val tabs = root.findAccessibilityNodeInfosByViewId("$INSTAGRAM:id/$id")
            if (tabs.any { it.isSelected }) return true
        }
        return findSelectedByDescription(root, REELS_LABELS, depth = 0)
    }

    private fun findSelectedByDescription(
        node: AccessibilityNodeInfo,
        labels: Set<String>,
        depth: Int
    ): Boolean {
        if (depth > MAX_DEPTH) return false
        val desc = node.contentDescription?.toString()?.trim()?.lowercase()
        if (node.isSelected && desc != null && desc in labels) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findSelectedByDescription(child, labels, depth + 1)) return true
        }
        return false
    }

    private fun leaveReels(root: AccessibilityNodeInfo) {
        for (id in FEED_TAB_IDS) {
            val tab = root.findAccessibilityNodeInfosByViewId("$INSTAGRAM:id/$id").firstOrNull()
            if (tab != null && clickNodeOrParent(tab)) return
        }
        val byLabel = findClickableByDescription(root, FEED_LABELS, depth = 0)
        if (byLabel != null && clickNodeOrParent(byLabel)) return

        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun findClickableByDescription(
        node: AccessibilityNodeInfo,
        labels: Set<String>,
        depth: Int
    ): AccessibilityNodeInfo? {
        if (depth > MAX_DEPTH) return null
        val desc = node.contentDescription?.toString()?.trim()?.lowercase()
        if (desc != null && desc in labels) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findClickableByDescription(child, labels, depth + 1)?.let { return it }
        }
        return null
    }

    private fun clickNodeOrParent(start: AccessibilityNodeInfo): Boolean {
        var node: AccessibilityNodeInfo? = start
        var hops = 0
        while (node != null && hops < 4) {
            if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            node = node.parent
            hops++
        }
        return false
    }

    companion object {
        private const val INSTAGRAM = "com.instagram.android"
        private const val DEBOUNCE_MS = 700L
        private const val MAX_DEPTH = 25

        private val VIEWER_IDS = listOf(
            "clips_viewer_view_pager",
            "clips_viewer_container",
            "clips_swipe_refresh_container"
        )
        private val REELS_TAB_IDS = listOf("clips_tab")
        private val FEED_TAB_IDS = listOf("feed_tab")

        private val REELS_LABELS = setOf("reels")
        private val FEED_LABELS = setOf("home", "startseite", "feed")
    }
}
