/*
  Copyright 2018 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.audience;

import static com.adobe.marketing.mobile.audience.AudienceConstants.LOG_TAG;

import androidx.annotation.VisibleForTesting;
import com.adobe.marketing.mobile.MobilePrivacyStatus;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.StringUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * AudienceState class is responsible for the following:
 * <ol>
 *     <li>Keeping the current state of all Audience-related variables.</li>
 *     <li>Persisting variables via {@link NamedCollection}.</li>
 *     <li>Providing getters and setters for all maintained variables.</li>
 * </ol>
 */
class AudienceState {

	private static final String LOG_SOURCE = "AudienceState";

	private final NamedCollection localStorage;

	// configuration settings
	private String uuid = null;
	private Map<String, String> visitorProfile = null;
	private MobilePrivacyStatus privacyStatus = AudienceConstants.DEFAULT_PRIVACY_STATUS;

	private long lastResetTimestampMillis; // Stores the timestamp for most recent resetIdentities API call

	/**
	 * Constructor.
	 */
	AudienceState() {
		this(
			ServiceProvider
				.getInstance()
				.getDataStoreService()
				.getNamedCollection(AudienceConstants.AUDIENCE_MANAGER_SHARED_PREFS_DATA_STORE)
		);
	}

	@VisibleForTesting
	AudienceState(final NamedCollection namedCollection) {
		this.localStorage = namedCollection;
	}

	// ========================================================
	// package-protected methods
	// ========================================================
	/**
	 * Sets the value of this {@link #uuid} property.
	 * <p>
	 * Persists the new value to the data store returned by {@link ServiceProvider#getDataStoreService()}.
	 * <p>
	 * Setting the identifier is ignored if the global privacy is set to {@link MobilePrivacyStatus#OPT_OUT}.
	 *
	 * @param uuid {@link String} containing the new value for {@code uuid}
	 */
	void setUuid(final String uuid) {
		// update uuid locally
		if (StringUtils.isNullOrEmpty(uuid) || privacyStatus != MobilePrivacyStatus.OPT_OUT) {
			this.uuid = uuid;
		}

		// update uuid in data store
		if (localStorage == null) {
			Log.warning(
				LOG_TAG,
				LOG_SOURCE,
				"Unable to update uuid in persistence - persistence collection could not be retrieved."
			);
			return;
		}

		if (StringUtils.isNullOrEmpty(uuid)) {
			localStorage.remove(AudienceConstants.AUDIENCE_MANAGER_SHARED_PREFS_USER_ID_KEY);
		} else if (privacyStatus != MobilePrivacyStatus.OPT_OUT) {
			localStorage.setString(AudienceConstants.AUDIENCE_MANAGER_SHARED_PREFS_USER_ID_KEY, uuid);
		}
	}

	/**
	 * Sets the value of this {@link #visitorProfile} property.
	 * <p>
	 * Persists the new value to the {@link NamedCollection} for the Audience extension.
	 * <p>
	 * Setting the identifier is ignored if the global privacy is set to {@link MobilePrivacyStatus#OPT_OUT}.
	 *
	 * @param visitorProfile {@code Map<String, String>} containing the new {@code visitorProfile}
	 */
	void setVisitorProfile(final Map<String, String> visitorProfile) {
		// update visitorProfile locally
		if (visitorProfile == null || visitorProfile.isEmpty() || privacyStatus != MobilePrivacyStatus.OPT_OUT) {
			this.visitorProfile = visitorProfile;
		}

		// update the visitor profile in the data store
		if (localStorage == null) {
			Log.warning(
				LOG_TAG,
				LOG_SOURCE,
				"Unable to update visitor profile in persistence - persistence collection could not be retrieved."
			);
			return;
		}

		if (visitorProfile == null || visitorProfile.isEmpty()) {
			localStorage.remove(AudienceConstants.AUDIENCE_MANAGER_SHARED_PREFS_PROFILE_KEY);
		} else if (privacyStatus != MobilePrivacyStatus.OPT_OUT) {
			localStorage.setMap(AudienceConstants.AUDIENCE_MANAGER_SHARED_PREFS_PROFILE_KEY, visitorProfile);
		}
	}

	/**
	 * Sets the {@code MobilePrivacyStatus} for this {@code AudienceState}.
	 * @param privacyStatus the {@link MobilePrivacyStatus} to set for this {@link AudienceState}
	 */
	void setMobilePrivacyStatus(final MobilePrivacyStatus privacyStatus) {
		this.privacyStatus = privacyStatus;
		if (privacyStatus == MobilePrivacyStatus.OPT_OUT) {
			clearIdentifiers();
		}
	}

	/**
	 * Returns this {@link #uuid}.
	 * <p>
	 * If there is no {@code uuid} value in memory, this method attempts to find one from the {@link NamedCollection}.
	 *
	 * @return {@link String} containing {@code uuid} value
	 */
	String getUuid() {
		if (StringUtils.isNullOrEmpty(uuid)) {
			// load uuid from data store if we have one
			if (localStorage == null) {
				Log.warning(
					LOG_TAG,
					LOG_SOURCE,
					"Unable to retrieve uuid from persistence - persistence could not be accessed."
				);
				return uuid;
			}

			uuid = localStorage.getString(AudienceConstants.AUDIENCE_MANAGER_SHARED_PREFS_USER_ID_KEY, uuid);
		}

		return uuid;
	}

	/**
	 * Returns this {@link #visitorProfile}.
	 * <p>
	 * If there is no {@code visitorProfile} value in memory, this method attempts to find one from the {@link NamedCollection}.
	 *
	 * @return {@code Map<String, String>} containing visitor profile
	 */
	Map<String, String> getVisitorProfile() {
		if (visitorProfile == null || visitorProfile.isEmpty()) {
			// load visitor profile from data store if we have one
			if (localStorage == null) {
				Log.warning(
					LOG_TAG,
					LOG_SOURCE,
					"Unable to retrieve visitor profile from persistence - persistence could not be accessed."
				);
				return visitorProfile;
			}

			if (localStorage.contains(AudienceConstants.AUDIENCE_MANAGER_SHARED_PREFS_PROFILE_KEY)) {
				visitorProfile = localStorage.getMap(AudienceConstants.AUDIENCE_MANAGER_SHARED_PREFS_PROFILE_KEY);
			}
		}

		return visitorProfile;
	}

	/**
	 * Gets the {@code MobilePrivacyStatus} for this {@code AudienceState}.
	 * @return the {@link MobilePrivacyStatus} for this {@link AudienceState}
	 */
	MobilePrivacyStatus getMobilePrivacyStatus() {
		return privacyStatus;
	}

	/**
	 * Updates the last reset timestamp in memory
	 * @param timestampMillis the timestamp of the reset event, in milliseconds
	 */
	void setLastResetTimestamp(final long timestampMillis) {
		if (timestampMillis >= 0) {
			this.lastResetTimestampMillis = timestampMillis;
		}
	}

	/**
	 * @return last reset timestamp, in milliseconds
	 */
	long getLastResetTimestampMillis() {
		return this.lastResetTimestampMillis;
	}

	/**
	 * Get the data for this {@code AudienceState} instance to share with other modules.
	 * The state data is only populated if the set privacy status is not {@link MobilePrivacyStatus#OPT_OUT}.
	 *
	 * @return {@link Map<String, Object>} map of this {@link AudienceState}
	 */
	Map<String, Object> getStateData() {
		final Map<String, Object> stateData = new HashMap<>();

		if (getMobilePrivacyStatus() == MobilePrivacyStatus.OPT_OUT) {
			// do not share state if privacy is Opt-Out
			return stateData;
		}

		String uuid = getUuid();

		if (!StringUtils.isNullOrEmpty(uuid)) {
			stateData.put(AudienceConstants.EventDataKeys.Audience.UUID, uuid);
		}

		Map<String, String> profile = getVisitorProfile();

		if (profile != null) {
			stateData.put(AudienceConstants.EventDataKeys.Audience.VISITOR_PROFILE, profile);
		}

		return stateData;
	}

	/**
	 * Clear the identifiers for this {@code AudienceState}.
	 * The cleared identifiers are:
	 * <ul>
	 *     <li>UUID</li>
	 *     <li>Visitor Profiles</li>
	 * </ul>
	 */
	void clearIdentifiers() {
		setUuid(null);
		setVisitorProfile(null);
	}
}
