/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.grouplist.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.feature.grouplist.GroupListTestTags
import org.mifos.groupbanking.feature.grouplist.generated.resources.Res
import org.mifos.groupbanking.feature.grouplist.generated.resources.screens_group_list_search_clear_cd
import org.mifos.groupbanking.feature.grouplist.generated.resources.screens_group_list_search_hint
import org.mifos.groupbanking.feature.grouplist.generated.resources.screens_group_list_search_icon_cd

/**
 * Search field — `ui.yaml#components.search_bar` (mirrors `SPEC.md`'s 48dp-height,
 * 28dp-corner-radius search bar). Dispatches `OnSearch(query)` on every keystroke (client-side
 * filter, no debounce needed — filtering runs against the already-loaded in-memory page set) and
 * `OnClearSearch` from the trailing clear icon, which only renders once [query] is non-blank. See
 * API.md#screen.
 */
@Composable
fun GroupListSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchIconCd = stringResource(Res.string.screens_group_list_search_icon_cd)
    val clearIconCd = stringResource(Res.string.screens_group_list_search_clear_cd)
    val hint = stringResource(Res.string.screens_group_list_search_hint)
    val sp = MaterialTheme.spacing

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = sp.touchTargetMin)
            .testTag(GroupListTestTags.SEARCH_FIELD),
        placeholder = { Text(text = hint) },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = searchIconCd) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier
                        .heightIn(min = sp.touchTargetMin)
                        .testTag(GroupListTestTags.SEARCH_CLEAR_ICON),
                ) {
                    Icon(imageVector = Icons.Filled.Clear, contentDescription = clearIconCd)
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(),
    )
}
