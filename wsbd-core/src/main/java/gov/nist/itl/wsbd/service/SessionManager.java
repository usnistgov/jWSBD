/*----------------------------------------------------------------------------------------------------+
|                             National Institute of Standards and Technology                          |
|                                        Biometric Clients Lab                                        |
+-----------------------------------------------------------------------------------------------------+
 File author(s):
      Kevin Mangold (kevin.mangold@nist.gov)
      Jacob Glueck (jacob.glueck@nist.gov)

+-----------------------------------------------------------------------------------------------------+
| NOTICE & DISCLAIMER                                                                                 |
|                                                                                                     |
| The research software provided on this web site ("software") is provided by NIST as a public        |
| service. You may use, copy and distribute copies of the software in any medium, provided that you   |
| keep intact this entire notice. You may improve, modify and create derivative works of the software |
| or any portion of the software, and you may copy and distribute such modifications or works.        |
| Modified works should carry a notice stating that you changed the software and should note the date |
| and nature of any such change.  Please explicitly acknowledge the National Institute of Standards   |
| and Technology as the source of the software.                                                       |
|                                                                                                     |
| The software is expressly provided "AS IS."  NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED,  |
| IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF      |
| MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY.  NIST        |
| NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR         |
| ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED.  NIST DOES NOT WARRANT OR MAKE ANY               |
| REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED |
| TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.                           |
|                                                                                                     |
| You are solely responsible for determining the appropriateness of using and distributing the        |
| software and you assume all risks associated with its use, including but not limited to the risks   |
| and costs of program errors, compliance with applicable laws, damage to or loss of data, programs   |
| or equipment, and the unavailability or interruption of operation.  This software is not intended   |
| to be used in any situation where a failure could cause risk of injury or damage to property.  The  |
| software was developed by NIST employees.  NIST employee contributions are not subject to copyright |
| protection within the United States.                                                                |
|                                                                                                     |
| Specific hardware and software products identified in this open source project were used in order   |
| to perform technology transfer and collaboration. In no case does such identification imply         |
| recommendation or endorsement by the National Institute of Standards and Technology, nor            |
| does it imply that the products and equipment identified are necessarily the best available for the |
| purpose.                                                                                            |
+----------------------------------------------------------------------------------------------------*/

package gov.nist.itl.wsbd.service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.oasis_open.docs.bioserv.ns.wsbd_1.Result;
import org.oasis_open.docs.bioserv.ns.wsbd_1.Status;

import gov.nist.itl.wsbd.configuration.ServerConfiguration;

/**
 * Represents: a manager for WS-BD sessions. Handles registration, locking, and
 * sensor usage.
 *
 * This class is not thread safe and so external synchronization is required.
 *
 * @author Kevin Mangold
 * @author Jacob Glueck
 *
 */
public class SessionManager {
	
	/**
	 * Represents: a map of the currently registered sessions to the time the
	 * last were active
	 */
	private final Map<UUID, Instant> sessions;
	
	/**
	 * The current lock holder, or <code>null</code> if there is no current lock
	 * holder.
	 */
	private UUID lockHolder;

	/**
	 * The current sensor user, or <code>null</code> if there is no sensor user.
	 * Invariant: If <code>sensorUser != null</code>, then
	 * <code>lockHolder.equals(sensorUser)</code> and
	 * <code>sessions.containsKey(sensorUser)</code>
	 */
	private UUID sensorUser;
	
	/**
	 * The configuration of this session manager
	 */
	private final SessionManagerConfiguration config;
	
	/**
	 * Creates: a new session manager which has no users and which uses the
	 * default configuration.
	 */
	public SessionManager() {
		this(new SessionManagerConfiguration());
	}

	/**
	 * Creates: a new session manager which has no users.
	 *
	 * @param config
	 *            the configuration of this session manager.
	 */
	public SessionManager(SessionManagerConfiguration config) {
		sessions = new HashMap<>();
		this.config = config;
	}
	
	/**
	 * @return the session manager configuration
	 */
	public SessionManagerConfiguration configuration() {

		return config;
	}
	
	/**
	 * Effect: logs the specified session ID as having last been active now.
	 * Requires: the session must be registered (assert)
	 *
	 * @param id
	 *            the ID to mark as used now
	 */
	private void logSessionActivity(UUID id) {

		assert sessions.containsKey(id);
		sessions.put(id, Instant.now());
	}

	/**
	 * @return true if and only if the number of sessions is the maximum number
	 *         of sessions.
	 */
	private boolean atCapacity() {
		
		return sessions.size() >= config.maximumConcurrentSessions();
	}
	
	/**
	 * Effect: unregisters all sessions that have been inactive for longer than
	 * the inactivity timeout. If a session has been inactive for longer than
	 * the timeout and holds the lock or is using the sensor, its lock is
	 * released and its sensor use is released.
	 *
	 * @return the UUIDs of the unregistered sessions
	 */
	public Set<UUID> pruneInactiveSessions() {
		
		Set<UUID> removed = new HashSet<>();
		for (Map.Entry<UUID, Instant> session : sessions.entrySet()) {
			if (Duration.between(session.getValue(), Instant.now()).compareTo(config.inactivitytimeout()) > 0) {
				removed.add(session.getKey());
				if (sensorUser != null && sensorUser.equals(session.getKey())) {
					sensorUser = null;
				}
				if (lockHolder != null && lockHolder.equals(session.getKey())) {
					lockHolder = null;
				}
				assert invariant();
			}
		}

		for (UUID toRemove : removed) {
			sessions.remove(toRemove);
		}
		return removed;
	}

	/**
	 * Effect: creates a new session and registers it
	 *
	 * @return the UUID of the new session
	 */
	public Result registerSession() {
		// Make sure we do not go over capacity
		if (atCapacity() && config.autoDropLRUSessions()) {
			pruneInactiveSessions();
		}

		// If we are still at capacity, return a failure
		if (atCapacity()) {
			return Utility.result(Status.FAILURE, "Service at maximum capacity of " + config.maximumConcurrentSessions() + " and is unable to remove any inactive sessions");
		}

		UUID id = UUID.randomUUID();
		while (sessions.containsKey(id)) {
			id = UUID.randomUUID();
		}
		registerSession(id);
		Result result = Utility.result(Status.SUCCESS);
		Utility.setResultSessionID(result, id);
		return result;
	}
	
	/**
	 * Effect: adds the specified ID to the session map with the current time
	 * Requires: the ID must not be in the map (assert)
	 *
	 * @param id
	 *            the ID
	 */
	private void registerSession(UUID id) {
		assert !sessions.containsKey(id);
		sessions.put(id, Instant.now());
	}
	
	/**
	 * Determines if the specified UUID is registered
	 *
	 * @param id
	 *            the ID to check
	 * @return true if and only if the ID is registered
	 */
	public boolean isRegistered(UUID id) {
		return sessions.containsKey(id);
	}
	
	/**
	 * Effect: attempts to unregister the session. If order to succeed, the
	 * specified client must not currently be using the sensor. If he is, this
	 * method will return a result with the status {@link Status#SENSOR_BUSY}.
	 * If the specified session currently holds the lock, this method will
	 * release the lock and then unregister the session. If the session is not
	 * currently registered, this method returns with the status
	 * {@link Status#SUCCESS} because it is idempotent.
	 *
	 * @param id
	 *            the ID of the sensor to unregister
	 * @return the result
	 */
	public Result unregisterSession(UUID id) {
		assert invariant();

		// If not registered, it must work because this is idempotent
		if (!isRegistered(id)) {
			return Utility.result(Status.SUCCESS);
		}
		
		if (sensorUser != null && sensorUser.equals(id)) {
			logSessionActivity(id);
			return Utility.result(Status.SENSOR_BUSY);
		} else {
			// Safe because unlock is idempotent so if we do not have the lock,
			// it must succeed.
			Result unlockStatus = unlock(id);
			assert unlockStatus.getStatus() == Status.SUCCESS;
			sessions.remove(id);
			return Utility.result(Status.SUCCESS);
		}
	}
	
	/**
	 * Effect: attempts to obtain the lock. If the ID is not registered, returns
	 * a status of {@link Status#INVALID_ID}. If the lock is held by another
	 * client, returns a status of {@link Status#LOCK_HELD_BY_ANOTHER}. If the
	 * lock is held by no one or is held by the specified client, this method
	 * returns a status of {@link Status#SUCCESS}.
	 *
	 * @param id
	 *            the client who wants to obtain the lock
	 * @return the result
	 */
	public Result lock(UUID id) {
		
		assert invariant();
		if (!isRegistered(id)) {
			return Utility.result(Status.INVALID_ID);
		} else {
			logSessionActivity(id);
			if (lockHolder == null || lockHolder.equals(id)) {
				lockHolder = id;
				return Utility.result(Status.SUCCESS);
			} else {
				return Utility.result(Status.LOCK_HELD_BY_ANOTHER);
			}
		}
	}
	
	/**
	 * Checks to see if the specified client holds the lock
	 *
	 * @param id
	 *            the client's ID
	 * @return true if and only if the client holds the lock
	 */
	public boolean hasLock(UUID id) {

		assert invariant();
		return lockHolder != null && lockHolder.equals(id);
	}
	
	/**
	 * Effect: attempts to release the lock. If the ID is not registered,
	 * returns a status of {@link Status#INVALID_ID}. If the client is currently
	 * using the sensor, this method returns a status of
	 * {@link Status#SENSOR_BUSY}. Otherwise, it releases the lock if the client
	 * holds it and returns a status of {@link Status#SUCCESS}. Note that this
	 * method succeeds even if no client holds the lock or another client holds
	 * the lock because it is idempotent.
	 *
	 * @param id
	 *            the client's ID
	 * @return the result
	 */
	public Result unlock(UUID id) {
		
		if (!isRegistered(id)) {
			return Utility.result(Status.INVALID_ID);
		} else {
			logSessionActivity(id);
			if (sensorUser != null && sensorUser.equals(id)) {
				return Utility.result(Status.SENSOR_BUSY);
			} else {
				// If no one has the lock or we have the lock, we can unlock.
				// Otherwise, someone else holds the lock.
				if (lockHolder == null || hasLock(id)) {
					lockHolder = null;
					return Utility.result(Status.SUCCESS);
				} else {
					return Utility.result(Status.LOCK_HELD_BY_ANOTHER);
				}
			}
		}
	}
	
	/**
	 * Effect: attempts to steal the lock from the current holder. If the ID is
	 * not registered, returns a status of {@link Status#INVALID_ID}. If the
	 * client currently holds the lock or the lock is not held, returns a status
	 * of {@link Status#SUCCESS}. If a different client holds the lock, and if
	 * the Lock Stealing Prevention Period (LSPP) has not elapsed, returns
	 * {@link Status#FAILURE}. If the LSPP has elapsed, this method will obtain
	 * the lock and return {@link Status#SUCCESS}. However, if a sensor
	 * operation is currently running, it will remain running, and may need to
	 * be canceled.
	 *
	 * @param id
	 *            the client's ID
	 * @return the result
	 */
	public Result stealLock(UUID id) {

		if (!isRegistered(id)) {
			return Utility.result(Status.INVALID_ID);
		} else {
			logSessionActivity(id);
			if (lockHolder == null || lockHolder.equals(id)) {
				return Utility.result(Status.SUCCESS);
			} else {
				Duration elapsedTime = Duration.between(sessions.get(lockHolder), Instant.now());
				if (elapsedTime.compareTo(config.lockStealingPreventionPeriod()) > 0) {
					// Steal the lock
					// After the lock is stolen, no user is using the sensor
					// even if it is still completing an operation
					sensorUser = null;
					lockHolder = id;
					return Utility.result(Status.SUCCESS);
				} else {
					return Utility.result(Status.FAILURE,
							"Only " + elapsedTime.toString() + "has elapsed since the current lock holder's last operation. The current lock stealing prevention period is "
									+ config.lockStealingPreventionPeriod().toString());
				}
			}
		}
	}

	/**
	 * Effect: tries to mark the sensor as being used by the specified client.
	 * This prevents the client from unlocking or unregistering while using the
	 * sensor. In order to succeed, the current sensor user must either be the
	 * no one or the client attempting to acquire it. If the requestor is not
	 * registered, returns {@link Status#INVALID_ID}. If the requestor does not
	 * hold the lock, returns {@link Status#LOCK_NOT_HELD} if no one holds the
	 * lock or {@link Status#LOCK_HELD_BY_ANOTHER} if another client holds the
	 * lock.
	 *
	 * Requires: the ID is registered and locked (assert).
	 *
	 * @param id
	 *            the client's ID
	 * @return true if and only if the sensor was acquired
	 */
	public Result acquireSensor(UUID id) {
		
		if (!isRegistered(id)) {
			return Utility.result(Status.INVALID_ID);
		} else {
			return setSensorUserIfAuthorized(id, id);
		}
	}

	/**
	 * Effect: tries to mark the sensor as no longer being used by the specified
	 * client. This allows the client using the sensor to release the lock or
	 * unregister. This can only fail if the current sensor user is not the
	 * specified user. Otherwise, it will succeed. Note that this succeeds if no
	 * one is using the sensor and if the specified user is using the sensor. If
	 * the requestor is not registered, returns {@link Status#INVALID_ID}. If
	 * the requestor does not hold the lock, returns
	 * {@link Status#LOCK_NOT_HELD} if no one holds the lock or
	 * {@link Status#LOCK_HELD_BY_ANOTHER} if another client holds the lock.
	 *
	 * @param id
	 *            the client ID
	 * @return true if and only if no user is now using the sensor
	 */
	public Result releaseSensor(UUID id) {

		assert invariant();
		if (!isRegistered(id)) {
			return Utility.result(Status.INVALID_ID);
		} else {
			logSessionActivity(id);
			if (lockHolder == null) {
				return Utility.result(Status.LOCK_NOT_HELD);
			} else if (!lockHolder.equals(id)) {
				return Utility.result(Status.LOCK_HELD_BY_ANOTHER);
			} else {
				// Since the client holds the lock and the invariant holds, the
				// client must be the sensor user or there must be no sensor
				// user.
				sensorUser = null;
				return Utility.result(Status.SUCCESS);
			}
		}
	}

	/**
	 * Effect: sets the sensor user to <code>newUser</code> if there is no
	 * current sensor user or the current sensor user is the requestor. If the
	 * requestor is not registered, returns {@link Status#INVALID_ID}. If the
	 * requestor does not hold the lock, returns {@link Status#LOCK_NOT_HELD} if
	 * no one holds the lock or {@link Status#LOCK_HELD_BY_ANOTHER} if another
	 * client holds the lock. <br>
	 * Requires: that the new user be registered (assert)
	 *
	 * @param requestor
	 *            the UUID of the user requesting the change
	 * @param newUser
	 *            the new sensor user
	 * @return true if and only if the requestor is authorized to set the state
	 */
	private Result setSensorUserIfAuthorized(UUID requestor, UUID newUser) {

		assert invariant();
		assert newUser != null && isRegistered(newUser);
		if (!isRegistered(requestor)) {
			return Utility.result(Status.INVALID_ID);
		} else {
			logSessionActivity(newUser);
			if (lockHolder == null) {
				return Utility.result(Status.LOCK_NOT_HELD);
			} else if (!lockHolder.equals(requestor)) {
				return Utility.result(Status.LOCK_HELD_BY_ANOTHER);
			} else {
				// Since the client holds the lock and the invariant holds, the
				// client must be the sensor user or there must be no sensor
				// user.
				assert sensorUser == null || sensorUser.equals(requestor);
				sensorUser = newUser;
				return Utility.result(Status.SUCCESS);
			}
		}
	}

	/**
	 * Checks the class invariant.
	 *
	 * @return true if the invariant holds, false otherwise.
	 */
	private boolean invariant() {

		if (sensorUser != null) {
			return lockHolder.equals(sensorUser) && sessions.containsKey(sensorUser);
		} else {
			return true;
		}
	}

	/**
	 * Represents: the configuration of a session manager
	 *
	 * @author Jacob Glueck
	 *
	 */
	public static class SessionManagerConfiguration {

		/**
		 * The default inactivity timeout
		 */
		public static final Duration defaultInactivityTimeout = Duration.of(1, ChronoUnit.HOURS);
		/**
		 * The minimum time that must elapse since a client's last operation
		 * before they may be dropped for any reason
		 */
		private Duration inactivityTimeout = SessionManagerConfiguration.defaultInactivityTimeout;

		/**
		 * The default maximum number of concurrent sessions permitted
		 */
		public static final int defaultMaximumConcurrentSessions = Integer.MAX_VALUE;
		/**
		 * The number of maximum concurrent sessions permitted
		 */
		private int maximumConcurrentSessions = SessionManagerConfiguration.defaultMaximumConcurrentSessions;

		/**
		 * The default value for autoDropLRUSessions.
		 */
		public static final boolean defaultAutoDropLRUSessions = true;
		/**
		 * True if this session manager should automatically drop the least
		 * recently used sessions to make room for a new sessions. The sessions
		 * manager will not drop any session unless they have been inactive for
		 * longer than the inactivity timeout.
		 */
		private boolean autoDropLRUSessions = SessionManagerConfiguration.defaultAutoDropLRUSessions;
		
		/**
		 * The default amount of time that must elapse after a client's most
		 * recent action for another client to steal the lock.
		 */
		public static final Duration defaultLockStealingPreventionPeriod = Duration.of(5, ChronoUnit.MINUTES);
		/**
		 * The lock stealing prevention period
		 */
		private Duration lockStealingPreventionPeriod = SessionManagerConfiguration.defaultLockStealingPreventionPeriod;

		/**
		 * Creates: a new configuration with all the default values
		 */
		public SessionManagerConfiguration() {
		}
		
		/**
		 * Creates: a new configuration based on an XML server configuration.
		 *
		 * @param conf
		 *            the server configuration
		 * @throws ArithmeticException
		 *             if the numbers are too big
		 */
		public SessionManagerConfiguration(ServerConfiguration conf) {
			inactivityTimeout = conf.inactivityTimeout();
			maximumConcurrentSessions = conf.maximumConcurrentSessions().intValueExact();
			autoDropLRUSessions = conf.autoDropLRUSessions();
			lockStealingPreventionPeriod = conf.lockStealingPreventionPeriod();
		}
		
		/**
		 * @return the inactivitytimeout
		 */
		public Duration inactivitytimeout() {
			
			return inactivityTimeout;
		}
		
		/**
		 * @param inactivitytimeout
		 *            the inactivitytimeout to set
		 */
		public void setInactivitytimeout(Duration inactivitytimeout) {
			
			inactivityTimeout = inactivitytimeout;
		}
		
		/**
		 * @return the maximumConcurrentSessions
		 */
		public int maximumConcurrentSessions() {
			
			return maximumConcurrentSessions;
		}
		
		/**
		 * @param maximumConcurrentSessions
		 *            the maximumConcurrentSessions to set
		 */
		public void setMaximumConcurrentSessions(int maximumConcurrentSessions) {
			
			this.maximumConcurrentSessions = maximumConcurrentSessions;
		}
		
		/**
		 * @return the autoDropLRUSessions
		 */
		public boolean autoDropLRUSessions() {
			
			return autoDropLRUSessions;
		}
		
		/**
		 * @param autoDropLRUSessions
		 *            the autoDropLRUSessions to set
		 */
		public void setAutoDropLRUSessions(boolean autoDropLRUSessions) {
			
			this.autoDropLRUSessions = autoDropLRUSessions;
		}
		
		/**
		 * @return the lockStealingPreventionPeriod
		 */
		public Duration lockStealingPreventionPeriod() {
			
			return lockStealingPreventionPeriod;
		}
		
		/**
		 * @param lockStealingPreventionPeriod
		 *            the lockStealingPreventionPeriod to set
		 */
		public void setLockStealingPreventionPeriod(Duration lockStealingPreventionPeriod) {
			
			this.lockStealingPreventionPeriod = lockStealingPreventionPeriod;
		}
		
		@Override
		public String toString() {

			return "SessionManagerConfiguration [inactivityTimeout=" + inactivityTimeout + ", maximumConcurrentSessions=" + maximumConcurrentSessions + ", autoDropLRUSessions=" + autoDropLRUSessions
					+ ", lockStealingPreventionPeriod=" + lockStealingPreventionPeriod + "]";
		}

	}
}