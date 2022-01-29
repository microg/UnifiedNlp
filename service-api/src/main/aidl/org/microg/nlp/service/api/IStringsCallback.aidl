/*
 * SPDX-FileCopyrightText: 2022, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service.api;

interface IStringsCallback {
    oneway void onStrings(int statusCode, in List<String> strings);
}
