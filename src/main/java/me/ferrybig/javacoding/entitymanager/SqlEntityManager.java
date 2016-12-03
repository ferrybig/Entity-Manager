/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.entitymanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 *
 * @author admin
 */
public class SqlEntityManager extends AbstractEntityManager {
	
	private final SqlFormatter formatter;
	private final SQLConnector connector;

	public SqlEntityManager(SqlFormatter formatter, SQLConnector connector) {
		this.formatter = formatter;
		this.connector = connector;
	}

	@Override
	protected Connection getNewConnection() throws SQLException {
		return connector.getNewConnection();
	}

	@Override
	protected PreparedStatement generateInsert(Connection connection, String table, Map<String, Object> set) throws SQLException {
		return formatter.generateInsert(connection, table, set);
	}

	@Override
	protected PreparedStatement generateSelect(Connection connection, String table, List<String> fields, Map<String, Object> where, int from, int limit) throws SQLException {
		return formatter.generateSelect(connection, table, fields, where, from, limit);
	}

	@Override
	protected PreparedStatement generateUpdate(Connection connection, String table, Map<String, Object> set, Map<String, Object> where) throws SQLException {
		return formatter.generateSet(connection, table, set, where);
	}

	@Override
	protected PreparedStatement generateDelete(Connection connection, String table, Map<String, Object> where) throws SQLException {
		return formatter.generateDelete(connection, table, where);
	}
	
	
	
	
	public static class SqlFormatter {
		private final String columnFormat;
		private final String tableFormat;
		
		private final String whereFormat = "%s = %s";
		private final String whereSeperator = " AND ";
		private final String whereAll = "1";
		
		private final String selectSeperator = ", ";
		private final String selectFormat = "SELECT %s FROM %s WHERE %s";
		
		private final String updateFormat = "UPDATE %s SET %s WHERE %s";
		private final String setFormat = "%s = %s";
		private final String setSeperator = " ,";
		
		private final String deleteFormat = "DELETE FROM %s WHERE %s";
		
		private final String insertFormat = "INSERT INTO %s (%s) VALUES (%s)";
		private final String insertFieldListSeperator = ", ";
		private final String insertValuesListSeperator = ", ";

		public SqlFormatter() {
			this('`','`');
		}
		
		public SqlFormatter(char tableQuote, char columnQuote) {
			columnFormat = columnQuote + "%s" + columnQuote;
			tableFormat = tableQuote + "%s" + tableQuote;
		}
		
		
		
		protected String quoteString(Connection c, List<Object> p, String str) {
			return str.replaceAll("'","''");
		}
		
		protected String safeString(Connection c, List<Object> p, String str) {
			return quoteString(c, p, str);
		}
		
		protected String columnName(Connection c, List<Object> p, String column) {
			return String.format(columnFormat, quoteString(c, p, column));
		}
		
		protected String tableName(Connection c, List<Object> p, String column) {
			return String.format(tableFormat, quoteString(c, p, column));
		}
		
		protected String value(Connection c, List<Object> p, Object value) {
			if(value instanceof Number) {
				return value.toString();
			}
			p.add(value);
			return "?";
		}
		
		protected String generateWhere(Connection c, List<Object> p, Map<String, Object> where) {
			return where.isEmpty() ? whereAll : where.entrySet().stream()
					.map((e)->generateWhereArgumentPair(c, p, e.getKey(), e.getValue()))
					.collect(Collectors.joining(whereSeperator));
		}
		
		protected String generateWhereArgumentPair(Connection c, List<Object> p, String key, Object value) {
			return String.format(whereFormat, columnName(c, p, key), value(c, p, value));
		}
		
		protected String generateSelectField(Connection c, List<Object> p, Collection<String> fields) {
			return fields.stream().map(e->columnName(c, p, e)).collect(Collectors.joining(selectSeperator));
		}
		
		protected String generateSelect(Connection c, List<Object> p, String table, List<String> fields, Map<String, Object> where, int start, int length) {
			return String.format(selectFormat, generateSelectField(c, p, fields), 
					tableName(c, p, table), generateWhere(c, p, where));
		}
		
		public PreparedStatement generateSelect(Connection c, String table, List<String> fields, Map<String, Object> where, int start, int length) throws SQLException {
			
			List<Object> arguments = new ArrayList<>();
			String sql = generateSelect(c, arguments, table, fields, where, start, length);
			LOG.log(Level.FINER, "Executing `{0}` with arguments {1}", new Object[]{sql, arguments});
			PreparedStatement stat = c.prepareStatement(sql);
			for(int i = 0; i < arguments.size(); i++) {
				stat.setObject(i + 1, arguments.get(i));
			}
			return stat;
		}
		
		protected String generateSetList(Connection c, List<Object> p, Map<String, Object> set) {
			return set.entrySet().stream()
					.map((e)->generateSetFieldArgumentPair(c, p, e.getKey(), e.getValue()))
					.collect(Collectors.joining(setSeperator));
		}
		
		protected String generateSetFieldArgumentPair(Connection c, List<Object> p, String key, Object value) {
			return String.format(setFormat, columnName(c, p, key), value(c, p, value));
		}
		
		protected String generateSet(Connection c, List<Object> p, String table, Map<String, Object> set, Map<String, Object> where) {
			return String.format(updateFormat, tableName(c, p, table),
					generateSetList(c, p, set), generateWhere(c, p, where));
		}
		
		public PreparedStatement generateSet(Connection c, String table, Map<String, Object> set, Map<String, Object> where) throws SQLException {
			
			List<Object> arguments = new ArrayList<>();
			String sql = generateSet(c, arguments, table, set, where);
			LOG.log(Level.FINER, "Executing `{0}` with arguments {1}", new Object[]{sql, arguments});
			PreparedStatement stat = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			for(int i = 0; i < arguments.size(); i++) {
				stat.setObject(i + 1, arguments.get(i));
			}
			return stat;
		}
		
		protected String generateDelete(Connection c, List<Object> p, String table, Map<String, Object> where) {
			return String.format(deleteFormat, tableName(c, p, table), generateWhere(c, p, where));
		}
		
		public PreparedStatement generateDelete(Connection c, String table, Map<String, Object> where) throws SQLException {
			
			List<Object> arguments = new ArrayList<>();
			String sql = generateDelete(c, arguments, table, where);
			LOG.log(Level.FINER, "Executing `{0}` with arguments {1}", new Object[]{sql, arguments});
			PreparedStatement stat = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			for(int i = 0; i < arguments.size(); i++) {
				stat.setObject(i + 1, arguments.get(i));
			}
			return stat;
		}
		
		protected String generateInsertFieldList(Connection c, List<Object> p, Set<String> fields) {
			return fields.stream().map(f -> columnName(c, p, f))
					.collect(Collectors.joining(insertFieldListSeperator));
		}
		
		protected String generateInsertValueList(Connection c, List<Object> p, Collection<Object> value) {
			return value.stream().map(f -> value(c, p, f))
					.collect(Collectors.joining(insertValuesListSeperator));
		}
		
		protected String generateInsert(Connection c, List<Object> p, String table, Map<String, Object> data) {
			return String.format(insertFormat, tableName(c, p, table), 
					generateInsertFieldList(c, p, data.keySet()),
					generateInsertValueList(c, p, data.values()));
		}
		
		public PreparedStatement generateInsert(Connection c, String table, Map<String, Object> data) throws SQLException {
			
			List<Object> arguments = new ArrayList<>();
			String sql = generateInsert(c, arguments, table, data);
			LOG.log(Level.FINER, "Executing `{0}` with arguments {1}", new Object[]{sql, arguments});
			PreparedStatement stat = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			for(int i = 0; i < arguments.size(); i++) {
				stat.setObject(i + 1, arguments.get(i));
			}
			return stat;
		}
		
	}

	
}
