package dev.ahmeddawood.elghassala.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/** Direction arrow that points the way the text actually reads: in RTL "A ← B" means from A to B. */
@Composable
fun arrow(): String = if (LocalLayoutDirection.current == LayoutDirection.Rtl) "←" else "→"

/** Same glyph choice outside composition, from the current default locale. */
fun arrowForLocale(): String =
    if (androidx.core.text.TextUtilsCompat.getLayoutDirectionFromLocale(java.util.Locale.getDefault()) ==
        androidx.core.view.ViewCompat.LAYOUT_DIRECTION_RTL) "←" else "→"
