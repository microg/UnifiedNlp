/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.ui.binding

import android.content.Context
import android.util.TypedValue
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter

@ColorInt
private fun Context.resolveColor(@AttrRes resid: Int): Int? {
    val typedValue = TypedValue()
    if (!theme.resolveAttribute(resid, typedValue, true)) return null
    val colorRes = if (typedValue.resourceId != 0) typedValue.resourceId else typedValue.data
    return ContextCompat.getColor(this, colorRes)
}

@BindingAdapter("app:backgroundColorAttr")
fun View.setBackgroundColorAttribute(@AttrRes resId: Int) = context.resolveColor(resId)?.let { setBackgroundColor(it) }
