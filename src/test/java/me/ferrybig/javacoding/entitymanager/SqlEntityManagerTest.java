/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.entitymanager;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import me.ferrybig.javacoding.entitymanager.SQLConnector;
import me.ferrybig.javacoding.entitymanager.SqlEntityManager.SqlFormatter;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author admin
 */
public class SqlEntityManagerTest {
	
	public SqlEntityManagerTest() {
	}

	/**
	 * Test of getNewConnection method, of class SqlEntityManager.
	 */
	@Test
	public void testGetNewConnection() throws Exception {
		System.out.println("getNewConnection");
		Connection connection = mock(Connection.class);
		Connection connection2 = mock(Connection.class);
		Connection connection3 = mock(Connection.class);
		SQLConnector con = mock(SQLConnector.class);
		when(con.getNewConnection()).thenReturn(connection, connection2, connection3);
		SqlFormatter formatter = new SqlFormatter();
		
		SqlEntityManager instance = new SqlEntityManager(formatter, con);
		
		assertEquals(connection, instance.getNewConnection());
		assertEquals(connection2, instance.getNewConnection());
		assertEquals(connection3, instance.getNewConnection());
	}

	/**
	 * Test of generateInsert method, of class SqlEntityManager.
	 */
	@Test
	public void testInsert() throws Exception {
		System.out.println("generateInsert");
		Connection connection = mock(Connection.class);
		ResultSet res = mock(ResultSet.class);
		PreparedStatement prep = mock(PreparedStatement.class);
		ResultSet res2 = mock(ResultSet.class);
		ResultSetMetaData meta = mock(ResultSetMetaData.class);
		PreparedStatement prep2 = mock(PreparedStatement.class);
		SQLConnector con = mock(SQLConnector.class);
		EntityType<UserEntity> test = UserEntity.TYPE;
		String expectedInsertSql = "INSERT INTO `test` (`firstname`, `lastname`) VALUES (?, ?)";
		String expectedSelectSql = "SELECT `firstname`, `id`, `lastname` FROM `test` WHERE `id` = 1000";
		
		when(connection.prepareStatement(any())).thenReturn(prep2).thenThrow(new AssertionError());
		when(connection.prepareStatement(any(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(prep).thenThrow(new AssertionError());
		when(prep.getGeneratedKeys()).thenReturn(res);
		when(prep.executeUpdate()).thenReturn(1);
		when(prep2.executeQuery()).thenReturn(res2);
		when(res.getObject(1)).thenReturn(1000);
		when(res.getMetaData()).thenReturn(meta);
		when(meta.getColumnCount()).thenReturn(1);
		when(meta.getCatalogName(1)).thenReturn("id");
		when(res.next()).thenReturn(true);
		when(res2.next()).thenReturn(true, false);
		when(res2.getObject("id")).thenReturn(1000);
		when(res2.getObject("firstname")).thenReturn("test");
		when(res2.getObject("lastname")).thenReturn("test2");
		when(con.getNewConnection()).thenReturn(connection);
		SqlFormatter formatter = new SqlFormatter();
		
		UserEntity expected = new UserEntity();
		expected.id = 1000;
		expected.firstname = "test";
		{
			Field f = expected.getClass().getDeclaredField("lastname");
			f.setAccessible(true);
			f.set(expected, "test2");
			f.setAccessible(false);
		}
		
		UserEntity insert = new UserEntity();
		insert.firstname = "test";
		
		SqlEntityManager instance = new SqlEntityManager(formatter, con);
		instance.register(test);
		
		UserEntity db = instance.insert(insert, test);
		
		assertEquals(expected, db);
		verify(connection).prepareStatement(expectedSelectSql);
		verify(connection).prepareStatement(expectedInsertSql, Statement.RETURN_GENERATED_KEYS);
	}

	/**
	 * Test of generateSelect method, of class SqlEntityManager.
	 */
	@Test
	public void testGetEntity() throws Exception {
		System.out.println("generateSelect");
		Connection connection = mock(Connection.class);
		ResultSet res = mock(ResultSet.class);
		PreparedStatement prep = mock(PreparedStatement.class);
		SQLConnector con = mock(SQLConnector.class);
		IntegerEntityType<UserEntity> test = UserEntity.TYPE;
		String expectedSelectSql = "SELECT `firstname`, `id`, `lastname` FROM `test` WHERE `id` = 1000";
		
		when(connection.prepareStatement(any())).thenReturn(prep).thenThrow(new AssertionError());
		when(prep.executeQuery()).thenReturn(res);
		when(res.getObject(1)).thenReturn(1000);
		when(res.next()).thenReturn(true, false);
		when(res.getObject("id")).thenReturn(1000);
		when(res.getObject("firstname")).thenReturn("test");
		when(res.getObject("lastname")).thenReturn("test2");
		when(con.getNewConnection()).thenReturn(connection);
		SqlFormatter formatter = new SqlFormatter();
		
		UserEntity expected = new UserEntity();
		expected.id = 1000;
		expected.firstname = "test";
		{
			Field f = expected.getClass().getDeclaredField("lastname");
			f.setAccessible(true);
			f.set(expected, "test2");
			f.setAccessible(false);
		}
		
		SqlEntityManager instance = new SqlEntityManager(formatter, con);
		instance.register(test);
		
		Optional<UserEntity> db = instance.select(test, 1000);
		
		assertEquals(expected, db.get());
		verify(connection).prepareStatement(expectedSelectSql);
	}
	
}
