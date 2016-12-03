/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.entitymanager;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.ferrybig.javacoding.entitymanager.SqlEntityManager.SqlFormatter;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author admin
 */
public class SqlEntityManagerIT {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

//	@BeforeClass
//	public static void enableLogging() {
//		Logger.getLogger("").getHandlers()[0].setLevel(Level.ALL);
//		Logger.getLogger("").setLevel(Level.INFO);
//		//Logger.getLogger(DefaultEntityType.class.getName()).setLevel(Level.ALL);
//		Logger.getLogger(AbstractEntityManager.class.getName()).setLevel(Level.ALL);
//	}
//	
//	@AfterClass
//	public static void disableLogging() {
//		Logger.getLogger("").getHandlers()[0].setLevel(Level.INFO);
//		Logger.getLogger("").setLevel(Level.INFO);
//		Logger.getLogger(DefaultEntityType.class.getName()).setLevel(Level.INFO);
//		Logger.getLogger(AbstractEntityManager.class.getName()).setLevel(Level.INFO);
//	}
	@Test
	public void testBasicInteraction() throws Exception {
		File db = folder.newFile("sqlite.db");
		Class.forName("org.sqlite.JDBC");
		SQLConnector sql = () -> DriverManager.getConnection("jdbc:sqlite:" + db.getAbsolutePath());
		SqlFormatter formatter = new SqlFormatter();
		IntegerEntityType<UserEntity> test = UserEntity.TYPE;

		SqlEntityManager instance = new SqlEntityManager(formatter, sql);
		instance.register(test);

		try (Connection c = sql.getNewConnection()) {
			try (Statement s = c.createStatement()) {
				s.executeUpdate("CREATE TABLE test ( "
						+ "id INTEGER PRIMARY KEY AUTOINCREMENT, "
						+ "firstname TEXT NOT NULL, "
						+ "lastname TEXT NOT NULL);");
			}
		}

		assertFalse(instance.select(test, 0).isPresent());
		assertFalse(instance.select(test, 1).isPresent());
		assertFalse(instance.select(test, 2).isPresent());
		assertFalse(instance.select(test, 3).isPresent());
		assertFalse(instance.select(test, 4).isPresent());
		assertEquals(0, instance.select(test).size());

		UserEntity person1 = instance.insert(new UserEntity(0, "firstname", "lastname"), test);

		assertFalse(instance.select(test, 0).isPresent());
		assertTrue(instance.select(test, 1).isPresent());
		assertFalse(instance.select(test, 2).isPresent());
		assertFalse(instance.select(test, 3).isPresent());
		assertFalse(instance.select(test, 4).isPresent());
		assertEquals(person1, instance.select(test, 1).get());
		assertEquals("firstname", instance.select(test, 1).get().getFirstname());
		assertEquals("lastname", instance.select(test, 1).get().getLastname());
		assertEquals(1, instance.select(test).size());

		UserEntity person2 = instance.insert(new UserEntity(0, "firstname2", "lastname2"), test);
		UserEntity person3 = instance.insert(new UserEntity(0, "firstname3", "lastname3"), test);

		assertFalse(instance.select(test, 0).isPresent());
		assertTrue(instance.select(test, 1).isPresent());
		assertTrue(instance.select(test, 2).isPresent());
		assertTrue(instance.select(test, 3).isPresent());
		assertFalse(instance.select(test, 4).isPresent());
		assertEquals(person1, instance.select(test, 1).get());
		assertEquals("firstname", instance.select(test, 1).get().getFirstname());
		assertEquals("lastname", instance.select(test, 1).get().getLastname());
		assertEquals(person2, instance.select(test, 2).get());
		assertEquals("firstname2", instance.select(test, 2).get().getFirstname());
		assertEquals("lastname2", instance.select(test, 2).get().getLastname());
		assertEquals(person3, instance.select(test, 3).get());
		assertEquals("firstname3", instance.select(test, 3).get().getFirstname());
		assertEquals("lastname3", instance.select(test, 3).get().getLastname());
		assertEquals(3, instance.select(test).size());

		instance.delete(person2, test);

		assertFalse(instance.select(test, 0).isPresent());
		assertTrue(instance.select(test, 1).isPresent());
		assertFalse(instance.select(test, 2).isPresent());
		assertTrue(instance.select(test, 3).isPresent());
		assertFalse(instance.select(test, 4).isPresent());
		assertEquals(person1, instance.select(test, 1).get());
		assertEquals("firstname", instance.select(test, 1).get().getFirstname());
		assertEquals("lastname", instance.select(test, 1).get().getLastname());
		assertEquals(person3, instance.select(test, 3).get());
		assertEquals("firstname3", instance.select(test, 3).get().getFirstname());
		assertEquals("lastname3", instance.select(test, 3).get().getLastname());
		assertEquals(2, instance.select(test).size());

		person1.firstname = "Ferrybig";

		person1 = instance.update(person1, test);

		assertFalse(instance.select(test, 0).isPresent());
		assertTrue(instance.select(test, 1).isPresent());
		assertFalse(instance.select(test, 2).isPresent());
		assertTrue(instance.select(test, 3).isPresent());
		assertFalse(instance.select(test, 4).isPresent());
		assertEquals(person1, instance.select(test, 1).get());
		assertEquals("Ferrybig", instance.select(test, 1).get().getFirstname());
		assertEquals("lastname", instance.select(test, 1).get().getLastname());
		assertEquals(person3, instance.select(test, 3).get());
		assertEquals("firstname3", instance.select(test, 3).get().getFirstname());
		assertEquals("lastname3", instance.select(test, 3).get().getLastname());
		assertEquals(2, instance.select(test).size());

	}

	@Test
	public void testAdvancedInteraction() throws Exception {
		File db = folder.newFile("sqlite.db");
		Class.forName("org.sqlite.JDBC");
		// TODO replace with proper caching db driver
		SQLConnector sql = () -> DriverManager.getConnection("jdbc:sqlite:" + db.getAbsolutePath());
		SqlFormatter formatter = new SqlFormatter();

		SqlEntityManager instance = new SqlEntityManager(formatter, sql);
		instance.register(UserEntity.TYPE);
		instance.register(MessageEntity.TYPE);
		instance.register(ChatRoomEntity.TYPE);

		try (Connection c = sql.getNewConnection()) {
			try (Statement s = c.createStatement()) {
				s.executeUpdate("CREATE TABLE test ( "
						+ "id INTEGER PRIMARY KEY AUTOINCREMENT, "
						+ "firstname TEXT NOT NULL, "
						+ "lastname TEXT NOT NULL);");
				s.executeUpdate("CREATE TABLE room ( "
						+ "id INTEGER PRIMARY KEY AUTOINCREMENT, "
						+ "name TEXT NOT NULL, "
						+ "creator_id INT NOT NULL,"
						+ "description TEXT NOT NULL, "
						+ "created INTEGER NOT NULL"
						+ ");");
				s.executeUpdate("CREATE TABLE message ( "
						+ "id INTEGER PRIMARY KEY AUTOINCREMENT, "
						+ "owner INT NOT NULL, "
						+ "message TEXT NOT NULL,"
						+ "room INT NULL);");
				s.executeUpdate("INSERT INTO test VALUES (1, 'first', 'second');");
				s.executeUpdate("INSERT INTO message VALUES (1, 1, 'third', NULL);");
			}
		}

		MessageEntity message = instance.select(MessageEntity.TYPE, 1).get();

		assertEquals("third", message.getMessage());
		assertEquals(1, message.getId());
		assertNotNull(message.getOwner());
		assertEquals(1, message.getOwner().getId());
		assertEquals("first", message.getOwner().getFirstname());
		assertEquals("second", message.getOwner().getLastname());

		UserEntity person2 = instance.insert(new UserEntity("test", "blah"));
		message.setOwner(person2);
		message = instance.update(message);
		assertEquals(1, person2.getMessages().size());

		MessageEntity databaseMessage = instance.select(MessageEntity.TYPE, message.getId()).get();

		assertEquals(message, databaseMessage);
		assertEquals("third", databaseMessage.getMessage());
		assertEquals(1, databaseMessage.getId());
		assertNotNull(databaseMessage.getOwner());
		assertEquals(person2.getId(), databaseMessage.getOwner().getId());
		assertEquals("test", databaseMessage.getOwner().getFirstname());
		assertEquals("blah", databaseMessage.getOwner().getLastname());

		instance.delete(message);

		assertFalse(instance.select(MessageEntity.TYPE, 1).isPresent());

	}

	
	@Test
	public void testMasterInteraction() throws Exception {
		File db = folder.newFile("sqlite.db");
		Class.forName("org.sqlite.JDBC");
		// TODO replace with proper caching db driver
		SQLConnector sql = () -> DriverManager.getConnection("jdbc:sqlite:" + db.getAbsolutePath());
		SqlFormatter formatter = new SqlFormatter();

		SqlEntityManager instance = new SqlEntityManager(formatter, sql);
		instance.register(UserEntity.TYPE);
		instance.register(MessageEntity.TYPE);
		instance.register(ChatRoomEntity.TYPE);
		instance.register(ChatRoomPermissionEntity.TYPE);

		try (Connection c = sql.getNewConnection()) {
			try (Statement s = c.createStatement()) {
				s.executeUpdate("CREATE TABLE test ( "
						+ "id INTEGER PRIMARY KEY AUTOINCREMENT, "
						+ "firstname TEXT NOT NULL, "
						+ "lastname TEXT NOT NULL);");
				s.executeUpdate("CREATE TABLE room ( "
						+ "id INTEGER PRIMARY KEY AUTOINCREMENT, "
						+ "name TEXT NOT NULL, "
						+ "creator_id INT NOT NULL,"
						+ "description TEXT NOT NULL, "
						+ "created INTEGER NOT NULL"
						+ ");");
				s.executeUpdate("CREATE TABLE message ( "
						+ "id INTEGER PRIMARY KEY AUTOINCREMENT, "
						+ "owner INT NOT NULL, "
						+ "message TEXT NOT NULL,"
						+ "room INT NULL);");
				s.executeUpdate("CREATE TABLE room_permission ( "
						+ "id INTEGER PRIMARY KEY AUTOINCREMENT, "
						+ "owner INT NOT NULL, "
						+ "message TEXT NOT NULL,"
						+ "room INT NULL);");
				s.executeUpdate("INSERT INTO test VALUES (1, 'first', 'second');");
				s.executeUpdate("INSERT INTO message VALUES (1, 1, 'third', NULL);");
			}
		}


	}
}
