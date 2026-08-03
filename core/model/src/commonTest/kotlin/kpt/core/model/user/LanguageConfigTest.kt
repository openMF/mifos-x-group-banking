/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.model.user

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Coverage for the `SWAHILI` entry added to `LanguageConfig` for the settings feature
 * (`idea-layer/screens/settings/{api,ui}.yaml#dtos.AppLanguage` + `i18n.lang_swahili`
 * "Kiswahili"), plus a sanity check that English/French/Hindi (the settings screen's other 3
 * declared `AppLanguage` values) are still present unmodified.
 */
class LanguageConfigTest {

    @Test
    fun languageConfig_swahili_hasCorrectLocaleNameAndText() {
        assertEquals("sw", LanguageConfig.SWAHILI.localeName)
        assertEquals("Kiswahili", LanguageConfig.SWAHILI.text)
    }

    @Test
    fun languageConfig_swahili_isPresentInEntries() {
        assertTrue(LanguageConfig.entries.contains(LanguageConfig.SWAHILI))
    }

    @Test
    fun languageConfig_settingsScreenLanguageValues_allPresent() {
        // idea-layer/screens/settings/api.yaml#dtos.AppLanguage.values: [ENGLISH, SWAHILI, FRENCH, HINDI]
        assertEquals("en", LanguageConfig.ENGLISH.localeName)
        assertEquals("sw", LanguageConfig.SWAHILI.localeName)
        assertEquals("fr", LanguageConfig.FRENCH.localeName)
        assertEquals("hi", LanguageConfig.HINDI.localeName)
    }
}
