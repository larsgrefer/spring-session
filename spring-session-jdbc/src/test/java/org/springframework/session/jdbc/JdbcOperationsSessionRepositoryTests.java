/*
 * Copyright 2014-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.session.jdbc;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.FlushMode;
import org.springframework.session.MapSession;
import org.springframework.session.SaveMode;
import org.springframework.session.Session;
import org.springframework.session.jdbc.JdbcOperationsSessionRepository.JdbcSession;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link JdbcOperationsSessionRepository}.
 *
 * @author Vedran Pavic
 * @author Craig Andrews
 * @since 1.2.0
 */
class JdbcOperationsSessionRepositoryTests {

	private static final String SPRING_SECURITY_CONTEXT = "SPRING_SECURITY_CONTEXT";

	@Mock
	private JdbcOperations jdbcOperations;

	private JdbcOperationsSessionRepository repository;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.initMocks(this);
		this.repository = new JdbcOperationsSessionRepository(this.jdbcOperations,
				TransactionOperations.withoutTransaction());
	}

	@Test
	void constructorNullJdbcOperations() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new JdbcOperationsSessionRepository(null, TransactionOperations.withoutTransaction()))
				.withMessage("jdbcOperations must not be null");
	}

	@Test
	void constructorNullTransactionOperations() {
		assertThatIllegalArgumentException()
				.isThrownBy(
						() -> new JdbcOperationsSessionRepository(this.jdbcOperations, (TransactionOperations) null))
				.withMessage("transactionOperations must not be null");
	}

	@Test
	@SuppressWarnings("deprecation")
	void constructorNullTransactionManager() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new JdbcOperationsSessionRepository(this.jdbcOperations, (PlatformTransactionManager) null))
				.withMessage("transactionManager must not be null");
	}

	@Test
	void setTableNameNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setTableName(null))
				.withMessage("Table name must not be empty");
	}

	@Test
	void setTableNameEmpty() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setTableName(" "))
				.withMessage("Table name must not be empty");
	}

	@Test
	void setCreateSessionQueryNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setCreateSessionQuery(null))
				.withMessage("Query must not be empty");
	}

	@Test
	void setCreateSessionQueryEmpty() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setCreateSessionQuery(" "))
				.withMessage("Query must not be empty");
	}

	@Test
	void setCreateSessionAttributeQueryNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setCreateSessionAttributeQuery(null))
				.withMessage("Query must not be empty");
	}

	@Test
	void setCreateSessionAttributeQueryEmpty() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setCreateSessionAttributeQuery(" "))
				.withMessage("Query must not be empty");
	}

	@Test
	void setGetSessionQueryNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setGetSessionQuery(null))
				.withMessage("Query must not be empty");
	}

	@Test
	void setGetSessionQueryEmpty() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setGetSessionQuery(" "))
				.withMessage("Query must not be empty");
	}

	@Test
	void setUpdateSessionQueryNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setUpdateSessionQuery(null))
				.withMessage("Query must not be empty");
	}

	@Test
	void setUpdateSessionQueryEmpty() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setUpdateSessionQuery(" "))
				.withMessage("Query must not be empty");
	}

	@Test
	void setUpdateSessionAttributeQueryNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setUpdateSessionAttributeQuery(null))
				.withMessage("Query must not be empty");
	}

	@Test
	void setUpdateSessionAttributeQueryEmpty() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setUpdateSessionAttributeQuery(" "))
				.withMessage("Query must not be empty");
	}

	@Test
	void setDeleteSessionAttributeQueryNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setDeleteSessionAttributeQuery(null))
				.withMessage("Query must not be empty");
	}

	@Test
	void setDeleteSessionAttributeQueryEmpty() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setDeleteSessionAttributeQuery(" "))
				.withMessage("Query must not be empty");
	}

	@Test
	void setDeleteSessionQueryNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setDeleteSessionQuery(null))
				.withMessage("Query must not be empty");
	}

	@Test
	void setDeleteSessionQueryEmpty() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setDeleteSessionQuery(" "))
				.withMessage("Query must not be empty");
	}

	@Test
	void setListSessionsByPrincipalNameQueryNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setListSessionsByPrincipalNameQuery(null))
				.withMessage("Query must not be empty");
	}

	@Test
	void setListSessionsByPrincipalNameQueryEmpty() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setListSessionsByPrincipalNameQuery(" "))
				.withMessage("Query must not be empty");
	}

	@Test
	void setDeleteSessionsByLastAccessTimeQueryNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setDeleteSessionsByExpiryTimeQuery(null))
				.withMessage("Query must not be empty");
	}

	@Test
	void setDeleteSessionsByLastAccessTimeQueryEmpty() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setDeleteSessionsByExpiryTimeQuery(" "))
				.withMessage("Query must not be empty");
	}

	@Test
	void setLobHandlerNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setLobHandler(null))
				.withMessage("LobHandler must not be null");
	}

	@Test
	void setConversionServiceNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setConversionService(null))
				.withMessage("conversionService must not be null");
	}

	@Test
	void setFlushModeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setFlushMode(null))
				.withMessage("flushMode must not be null");
	}

	@Test
	void setSaveModeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setSaveMode(null))
				.withMessage("saveMode must not be null");
	}

	@Test
	void createSessionDefaultMaxInactiveInterval() {
		JdbcOperationsSessionRepository.JdbcSession session = this.repository.createSession();

		assertThat(session.isNew()).isTrue();
		assertThat(session.getMaxInactiveInterval()).isEqualTo(new MapSession().getMaxInactiveInterval());
		verifyNoMoreInteractions(this.jdbcOperations);
	}

	@Test
	void createSessionCustomMaxInactiveInterval() {
		int interval = 1;
		this.repository.setDefaultMaxInactiveInterval(interval);

		JdbcOperationsSessionRepository.JdbcSession session = this.repository.createSession();

		assertThat(session.isNew()).isTrue();
		assertThat(session.getMaxInactiveInterval()).isEqualTo(Duration.ofSeconds(interval));
		verifyNoMoreInteractions(this.jdbcOperations);
	}

	@Test
	void createSessionImmediateFlushMode() {
		this.repository.setFlushMode(FlushMode.IMMEDIATE);
		JdbcSession session = this.repository.createSession();
		assertThat(session.isNew()).isFalse();
		verify(this.jdbcOperations).update(startsWith("INSERT"), isA(PreparedStatementSetter.class));
		verifyNoMoreInteractions(this.jdbcOperations);
	}

	@Test
	void saveNewWithoutAttributes() {
		JdbcOperationsSessionRepository.JdbcSession session = this.repository.createSession();

		this.repository.save(session);

		assertThat(session.isNew()).isFalse();
		verify(this.jdbcOperations, times(1)).update(startsWith("INSERT"), isA(PreparedStatementSetter.class));
		verifyNoMoreInteractions(this.jdbcOperations);
	}

	@Test
	void saveNewWithSingleAttribute() {
		JdbcOperationsSessionRepository.JdbcSession session = this.repository.createSession();
		session.setAttribute("testName", "testValue");

		this.repository.save(session);

		assertThat(session.isNew()).isFalse();
		verify(this.jdbcOperations, times(1)).update(startsWith("INSERT INTO SPRING_SESSION("),
				isA(PreparedStatementSetter.class));
		verify(this.jdbcOperations, times(1)).update(startsWith("INSERT INTO SPRING_SESSION_ATTRIBUTES("),
				isA(PreparedStatementSetter.class));
		verifyNoMoreInteractions(this.jdbcOperations);
	}

	@Test
	void saveNewWithMultipleAttributes() {
		JdbcOperationsSessionRepository.JdbcSession session = this.repository.createSession();
		session.setAttribute("testName1", "testValue1");
		session.setAttribute("testName2", "testValue2");

		this.repository.save(session);

		assertThat(session.isNew()).isFalse();
		verify(this.jdbcOperations, times(1)).update(startsWith("INSERT INTO SPRING_SESSION("),
				isA(PreparedStatementSetter.class));
		verify(this.jdbcOperations, times(1)).batchUpdate(startsWith("INSERT INTO SPRING_SESSION_ATTRIBUTES("),
				isA(BatchPreparedStatementSetter.class));
		verifyNoMoreInteractions(this.jdbcOperations);
	}

	@Test
	void saveUpdatedAddSingleAttribute() {
		JdbcOperationsSessionRepository.JdbcSession session = this.repository.new JdbcSession(new MapSession(),
				"primaryKey", false);
		session.setAttribute("testName", "testValue");

		this.repository.save(session);

		assertThat(session.isNew()).isFalse();
		verify(this.jdbcOperations, times(1)).update(startsWith("INSERT INTO SPRING_SESSION_ATTRIBUTES("),
				isA(PreparedStatementSetter.class));
		verifyNoMoreInteractions(this.jdbcOperations);
	}

	@Test
	void saveUpdatedAddMultipleAttributes() {
		JdbcOperationsSessionRepository.JdbcSession session = this.repository.new JdbcSession(new MapSession(),
				"primaryKey", false);
		session.setAttribute("testName1", "testValue1");
		session.setAttribute("testName2", "testValue2");

		this.repository.save(session);

		assertThat(session.isNew()).isFalse();
		verify(this.jdbcOperations, times(1)).batchUpdate(startsWith("INSERT INTO SPRING_SESSION_ATTRIBUTES("),
				isA(BatchPreparedStatementSetter.class));
		verifyNoMoreInteractions(this.jdbcOperations);
	}

	@Test
	void saveUpdatedModifySingleAttribute() {
		JdbcOperationsSessionRepository.JdbcSession session = this.repository.new JdbcSession(new MapSession(),
				"primaryKey", false);
		session.setAttribute("testName", "testValue");
		session.clearChangeFlags();
		session.setAttribute("testName", "testValue");

		this.repository.save(session);

		assertThat(session.isNew()).isFalse();
		verify(this.jdbcOperations, times(1)).update(startsWith("UPDATE SPRING_SESSION_ATTRIBUTES SET"),
				isA(PreparedStatementSetter.class));
		verifyNoMoreInteractions(this.jdbcOperations);
	}

	@Test
	void saveUpdatedModifyMultipleAttributes() {
		JdbcOperationsSessionRepository.JdbcSession session = this.repository.new JdbcSession(new MapSession(),
				"primaryKey", false);
		session.setAttribute("testName1", "testValue1");
		session.setAttribute("testName2", "testValue2");
		session.clearChangeFlags();
		session.setAttribute("testName1", "testValue1");
		session.setAttribute("testName2", "testValue2");

		this.repository.save(session);

		assertThat(session.isNew()).isFalse();
		verify(this.jdbcOperations, times(1)).batchUpdate(startsWith("UPDATE SPRING_SESSION_ATTRIBUTES SET"),
				isA(BatchPreparedStatementSetter.class));
		verifyNoMoreInteractions(this.jdbcOperations);
	}

	@Test
	void saveUpdatedRemoveSingleAttribute() {
		JdbcOperationsSessionRepository.JdbcSession session = this.repository.new JdbcSession(new MapSession(),
				"primaryKey", false);
		session.setAttribute("testName", "testValue");
		session.clearChangeFlags();
		session.removeAttribute("testName");

		this.repository.save(session);

		assertThat(session.isNew()).isFalse();
		verify(this.jdbcOperations, times(1)).update(startsWith("DELETE FROM SPRING_SESSION_ATTRIBUTES WHERE"),
				isA(PreparedStatementSetter.class));
		verifyNoMoreInteractions(this.jdbcOperations);
	}

	@Test
	void saveUpdatedRemoveNonExistingAttribute() {
		JdbcOperationsSessionRepository.JdbcSession session = this.repository.new JdbcSession(new MapSession(),
				"primaryKey", false);
		session.removeAttribute("testName");

		this.repository.save(session);

		assertThat(session.isNew()).isFalse();
		verifyNoMoreInteractions(this.jdbcOperations);
	}

	@Test
	void saveUpdatedRemoveMultipleAttributes() {
		JdbcOperationsSessionRepository.JdbcSession session = this.repository.new JdbcSession(new MapSession(),
				"primaryKey", false);
		session.setAttribute("testName1", "testValue1");
		session.setAttribute("testName2", "testValue2");
		session.clearChangeFlags();
		session.removeAttribute("testName1");
		session.removeAttribute("testName2");

		this.repository.save(session);

		assertThat(session.isNew()).isFalse();
		verify(this.jdbcOperations, times(1)).batchUpdate(startsWith("DELETE FROM SPRING_SESSION_ATTRIBUTES WHERE"),
				isA(BatchPreparedStatementSetter.class));
		verifyNoMoreInteractions(this.jdbcOperations);
	}

	@Test // gh-1070
	void saveUpdatedAddAndModifyAttribute() {
		JdbcOperationsSessionRepository.JdbcSession session = this.repository.new JdbcSession(new MapSession(),
				"primaryKey", false);
		session.setAttribute("testName", "testValue1");
		session.setAttribute("testName", "testValue2");

		this.repository.save(session);

		assertThat(session.isNew()).isFalse();
		verify(this.jdbcOperations).update(startsWith("INSERT INTO SPRING_SESSION_ATTRIBUTES("),
				isA(PreparedStatementSetter.class));
		verifyNoMoreInteractions(this.jdbcOperations);
	}

	@Test // gh-1070
	void saveUpdatedAddAndRemoveAttribute() {
		JdbcOperationsSessionRepository.JdbcSession session = this.repository.new JdbcSession(new MapSession(),
				"primaryKey", false);
		session.setAttribute("testName", "testValue");
		session.removeAttribute("testName");

		this.repository.save(session);

		assertThat(session.isNew()).isFalse();
		verifyNoMoreInteractions(this.jdbcOperations);
	}

	@Test // gh-1070
	void saveUpdatedModifyAndRemoveAttribute() {
		JdbcOperationsSessionRepository.JdbcSession session = this.repository.new JdbcSession(new MapSession(),
				"primaryKey", false);
		session.setAttribute("testName", "testValue1");
		session.clearChangeFlags();
		session.setAttribute("testName", "testValue2");
		session.removeAttribute("testName");

		this.repository.save(session);

		assertThat(session.isNew()).isFalse();
		verify(this.jdbcOperations).update(startsWith("DELETE FROM SPRING_SESSION_ATTRIBUTES WHERE"),
				isA(PreparedStatementSetter.class));
		verifyNoMoreInteractions(this.jdbcOperations);
	}

	@Test // gh-1070
	void saveUpdatedRemoveAndAddAttribute() {
		JdbcOperationsSessionRepository.JdbcSession session = this.repository.new JdbcSession(new MapSession(),
				"primaryKey", false);
		session.setAttribute("testName", "testValue1");
		session.clearChangeFlags();
		session.removeAttribute("testName");
		session.setAttribute("testName", "testValue2");

		this.repository.save(session);

		assertThat(session.isNew()).isFalse();
		verify(this.jdbcOperations).update(startsWith("UPDATE SPRING_SESSION_ATTRIBUTES SET"),
				isA(PreparedStatementSetter.class));
		verifyNoMoreInteractions(this.jdbcOperations);
	}

	@Test
	void saveUpdatedLastAccessedTime() {
		JdbcOperationsSessionRepository.JdbcSession session = this.repository.new JdbcSession(new MapSession(),
				"primaryKey", false);
		session.setLastAccessedTime(Instant.now());

		this.repository.save(session);

		assertThat(session.isNew()).isFalse();
		verify(this.jdbcOperations, times(1)).update(startsWith("UPDATE SPRING_SESSION SET"),
				isA(PreparedStatementSetter.class));
		verifyNoMoreInteractions(this.jdbcOperations);
	}

	@Test
	void saveUnchanged() {
		JdbcOperationsSessionRepository.JdbcSession session = this.repository.new JdbcSession(new MapSession(),
				"primaryKey", false);

		this.repository.save(session);

		assertThat(session.isNew()).isFalse();
		verifyNoMoreInteractions(this.jdbcOperations);
	}

	@Test
	@SuppressWarnings("unchecked")
	void getSessionNotFound() {
		String sessionId = "testSessionId";
		given(this.jdbcOperations.query(isA(String.class), isA(PreparedStatementSetter.class),
				isA(ResultSetExtractor.class))).willReturn(Collections.emptyList());

		JdbcOperationsSessionRepository.JdbcSession session = this.repository.findById(sessionId);

		assertThat(session).isNull();
		verify(this.jdbcOperations, times(1)).query(isA(String.class), isA(PreparedStatementSetter.class),
				isA(ResultSetExtractor.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	void getSessionExpired() {
		Session expired = this.repository.createSession();
		expired.setLastAccessedTime(Instant.now().minusSeconds(MapSession.DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS + 1));
		given(this.jdbcOperations.query(isA(String.class), isA(PreparedStatementSetter.class),
				isA(ResultSetExtractor.class))).willReturn(Collections.singletonList(expired));

		JdbcOperationsSessionRepository.JdbcSession session = this.repository.findById(expired.getId());

		assertThat(session).isNull();
		verify(this.jdbcOperations, times(1)).query(isA(String.class), isA(PreparedStatementSetter.class),
				isA(ResultSetExtractor.class));
		verify(this.jdbcOperations, times(1)).update(startsWith("DELETE"), eq(expired.getId()));
	}

	@Test
	@SuppressWarnings("unchecked")
	void getSessionFound() {
		Session saved = this.repository.new JdbcSession(new MapSession(), "primaryKey", false);
		saved.setAttribute("savedName", "savedValue");
		given(this.jdbcOperations.query(isA(String.class), isA(PreparedStatementSetter.class),
				isA(ResultSetExtractor.class))).willReturn(Collections.singletonList(saved));

		JdbcOperationsSessionRepository.JdbcSession session = this.repository.findById(saved.getId());

		assertThat(session.getId()).isEqualTo(saved.getId());
		assertThat(session.isNew()).isFalse();
		assertThat(session.<String>getAttribute("savedName")).isEqualTo("savedValue");
		verify(this.jdbcOperations, times(1)).query(isA(String.class), isA(PreparedStatementSetter.class),
				isA(ResultSetExtractor.class));
	}

	@Test
	void delete() {
		String sessionId = "testSessionId";

		this.repository.deleteById(sessionId);

		verify(this.jdbcOperations, times(1)).update(startsWith("DELETE"), eq(sessionId));
	}

	@Test
	void findByIndexNameAndIndexValueUnknownIndexName() {
		String indexValue = "testIndexValue";

		Map<String, JdbcOperationsSessionRepository.JdbcSession> sessions = this.repository
				.findByIndexNameAndIndexValue("testIndexName", indexValue);

		assertThat(sessions).isEmpty();
		verifyNoMoreInteractions(this.jdbcOperations);
	}

	@Test
	@SuppressWarnings("unchecked")
	void findByIndexNameAndIndexValuePrincipalIndexNameNotFound() {
		String principal = "username";
		given(this.jdbcOperations.query(isA(String.class), isA(PreparedStatementSetter.class),
				isA(ResultSetExtractor.class))).willReturn(Collections.emptyList());

		Map<String, JdbcOperationsSessionRepository.JdbcSession> sessions = this.repository
				.findByIndexNameAndIndexValue(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, principal);

		assertThat(sessions).isEmpty();
		verify(this.jdbcOperations, times(1)).query(isA(String.class), isA(PreparedStatementSetter.class),
				isA(ResultSetExtractor.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	void findByIndexNameAndIndexValuePrincipalIndexNameFound() {
		String principal = "username";
		Authentication authentication = new UsernamePasswordAuthenticationToken(principal, "notused",
				AuthorityUtils.createAuthorityList("ROLE_USER"));
		List<Session> saved = new ArrayList<>(2);
		Session saved1 = this.repository.createSession();
		saved1.setAttribute(SPRING_SECURITY_CONTEXT, authentication);
		saved.add(saved1);
		Session saved2 = this.repository.createSession();
		saved2.setAttribute(SPRING_SECURITY_CONTEXT, authentication);
		saved.add(saved2);
		given(this.jdbcOperations.query(isA(String.class), isA(PreparedStatementSetter.class),
				isA(ResultSetExtractor.class))).willReturn(saved);

		Map<String, JdbcOperationsSessionRepository.JdbcSession> sessions = this.repository
				.findByIndexNameAndIndexValue(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, principal);

		assertThat(sessions).hasSize(2);
		verify(this.jdbcOperations, times(1)).query(isA(String.class), isA(PreparedStatementSetter.class),
				isA(ResultSetExtractor.class));
	}

	@Test
	void cleanupExpiredSessions() {
		this.repository.cleanUpExpiredSessions();

		verify(this.jdbcOperations, times(1)).update(startsWith("DELETE"), anyLong());
	}

	@Test // gh-1120
	void getAttributeNamesAndRemove() {
		JdbcOperationsSessionRepository.JdbcSession session = this.repository.createSession();
		session.setAttribute("attribute1", "value1");
		session.setAttribute("attribute2", "value2");

		for (String attributeName : session.getAttributeNames()) {
			session.removeAttribute(attributeName);
		}

		assertThat(session.getAttributeNames()).isEmpty();
	}

	@Test
	void saveWithSaveModeOnSetAttribute() {
		this.repository.setSaveMode(SaveMode.ON_SET_ATTRIBUTE);
		MapSession delegate = new MapSession();
		delegate.setAttribute("attribute1", (Supplier<String>) () -> "value1");
		delegate.setAttribute("attribute2", (Supplier<String>) () -> "value2");
		delegate.setAttribute("attribute3", (Supplier<String>) () -> "value3");
		JdbcOperationsSessionRepository.JdbcSession session = this.repository.new JdbcSession(delegate,
				UUID.randomUUID().toString(), false);
		session.getAttribute("attribute2");
		session.setAttribute("attribute3", "value4");
		this.repository.save(session);
		verify(this.jdbcOperations).update(startsWith("UPDATE SPRING_SESSION_ATTRIBUTES SET"),
				isA(PreparedStatementSetter.class));
		verifyNoMoreInteractions(this.jdbcOperations);
	}

	@Test
	void saveWithSaveModeOnGetAttribute() {
		this.repository.setSaveMode(SaveMode.ON_GET_ATTRIBUTE);
		MapSession delegate = new MapSession();
		delegate.setAttribute("attribute1", (Supplier<String>) () -> "value1");
		delegate.setAttribute("attribute2", (Supplier<String>) () -> "value2");
		delegate.setAttribute("attribute3", (Supplier<String>) () -> "value3");
		JdbcOperationsSessionRepository.JdbcSession session = this.repository.new JdbcSession(delegate,
				UUID.randomUUID().toString(), false);
		session.getAttribute("attribute2");
		session.setAttribute("attribute3", "value4");
		this.repository.save(session);
		ArgumentCaptor<BatchPreparedStatementSetter> captor = ArgumentCaptor
				.forClass(BatchPreparedStatementSetter.class);
		verify(this.jdbcOperations).batchUpdate(startsWith("UPDATE SPRING_SESSION_ATTRIBUTES SET"), captor.capture());
		assertThat(captor.getValue().getBatchSize()).isEqualTo(2);
		verifyNoMoreInteractions(this.jdbcOperations);
	}

	@Test
	void saveWithSaveModeAlways() {
		this.repository.setSaveMode(SaveMode.ALWAYS);
		MapSession delegate = new MapSession();
		delegate.setAttribute("attribute1", (Supplier<String>) () -> "value1");
		delegate.setAttribute("attribute2", (Supplier<String>) () -> "value2");
		delegate.setAttribute("attribute3", (Supplier<String>) () -> "value3");
		JdbcOperationsSessionRepository.JdbcSession session = this.repository.new JdbcSession(delegate,
				UUID.randomUUID().toString(), false);
		session.getAttribute("attribute2");
		session.setAttribute("attribute3", "value4");
		this.repository.save(session);
		ArgumentCaptor<BatchPreparedStatementSetter> captor = ArgumentCaptor
				.forClass(BatchPreparedStatementSetter.class);
		verify(this.jdbcOperations).batchUpdate(startsWith("UPDATE SPRING_SESSION_ATTRIBUTES SET"), captor.capture());
		assertThat(captor.getValue().getBatchSize()).isEqualTo(3);
		verifyNoMoreInteractions(this.jdbcOperations);
	}

	@Test
	void flushModeImmediateSetAttribute() {
		this.repository.setFlushMode(FlushMode.IMMEDIATE);
		JdbcSession session = this.repository.new JdbcSession(new MapSession(), "primaryKey", false);
		String attrName = "someAttribute";
		session.setAttribute(attrName, "someValue");
		verify(this.jdbcOperations).update(startsWith("INSERT INTO SPRING_SESSION_ATTRIBUTES("),
				isA(PreparedStatementSetter.class));
		verifyNoMoreInteractions(this.jdbcOperations);
	}

	@Test
	void flushModeImmediateRemoveAttribute() {
		this.repository.setFlushMode(FlushMode.IMMEDIATE);
		MapSession cached = new MapSession();
		cached.setAttribute("attribute1", "value1");
		JdbcSession session = this.repository.new JdbcSession(cached, "primaryKey", false);
		session.removeAttribute("attribute1");
		verify(this.jdbcOperations).update(startsWith("DELETE FROM SPRING_SESSION_ATTRIBUTES WHERE"),
				isA(PreparedStatementSetter.class));
		verifyNoMoreInteractions(this.jdbcOperations);
	}

	@Test
	void flushModeSetMaxInactiveIntervalInSeconds() {
		this.repository.setFlushMode(FlushMode.IMMEDIATE);
		JdbcSession session = this.repository.new JdbcSession(new MapSession(), "primaryKey", false);
		session.setMaxInactiveInterval(Duration.ofSeconds(1));
		verify(this.jdbcOperations).update(startsWith("UPDATE SPRING_SESSION SET"), isA(PreparedStatementSetter.class));
		verifyNoMoreInteractions(this.jdbcOperations);
	}

	@Test
	void flushModeSetLastAccessedTime() {
		this.repository.setFlushMode(FlushMode.IMMEDIATE);
		JdbcSession session = this.repository.new JdbcSession(new MapSession(), "primaryKey", false);
		session.setLastAccessedTime(Instant.now());
		verify(this.jdbcOperations).update(startsWith("UPDATE SPRING_SESSION SET"), isA(PreparedStatementSetter.class));
		verifyNoMoreInteractions(this.jdbcOperations);
	}

}
