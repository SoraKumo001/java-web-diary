package to.pns.lib;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;

public class SQLite
{
	private Connection mConnection;


	public class SQRes
	{
		private Statement mStatement;
		private ResultSet mResult;
		public SQRes(Statement s,ResultSet r)
		{
			mStatement = s;
			mResult = r;
		}
		public ResultSetMetaData getMetaData() throws SQLException
		{
			return mResult.getMetaData();
		}
		public void close()
		{
			try
			{
				mResult.close();
				if(mStatement != null)
					mStatement.close();
			} catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
		public boolean next()
		{
			try
			{
				return mResult.next();
			} catch (SQLException e)
			{
			}
			return false;
		}
		public String getString(int index)
		{
			try
			{
				return mResult.getString(index);
			} catch (SQLException e)
			{
				e.printStackTrace();
			}
			return null;
		}
		public InputStream getBinaryStream(int index)
		{
			try
			{
				return mResult.getBinaryStream(index);
			} catch (SQLException e)
			{
				e.printStackTrace();
			}
			return null;
		}
		public int getInt(int index) throws SQLException
		{
			return mResult.getInt(index);
		}
		public long getLong(int index) throws SQLException
		{
			return mResult.getLong(index);
		}
		public boolean getBoolean(int index) throws SQLException
		{
			return mResult.getBoolean(index);
		}

		public Object getObject(int index)
		{
			try
			{
				return mResult.getObject(index);
			} catch (SQLException e)
			{
				e.printStackTrace();
			}
			return null;
		}
	}

	public SQLite()
	{
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}

	public Connection getConnection()
	{
		return mConnection;
	}
	public boolean open(String name)
	{
		// init table
	    try {
	    	mConnection = DriverManager.getConnection("jdbc:sqlite:"+name);
	    	//mConnection.setAutoCommit(false);
	    } catch (SQLException e) {
	        System.out.println(e);
	        return false;
	    }
	    return true;
	}
	@Override
	protected void finalize() throws Throwable
	{
		close();
		Enumeration<Driver> drivers = DriverManager.getDrivers();
		while( drivers.hasMoreElements() ){
		    Driver driver = drivers.nextElement();
		    DriverManager.deregisterDriver(driver);
		}
		super.finalize();
	}
	public boolean close()
	{
		try {
			if(mConnection != null)
			{
				mConnection.close();
				mConnection = null;
				return true;
			}
		} catch (SQLException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		return false;
	}
	public boolean exec(String sql)
	{
		try {
			Statement statement = mConnection.createStatement();
			if(statement != null)
			{
				statement.setQueryTimeout(5);
				statement.executeUpdate(sql);
				statement.close();
			    return true;
			}
		} catch (SQLException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		return false;
	}
	public SQRes query(String sql)
	{
		try {
			Statement statement = mConnection.createStatement();
			statement.setQueryTimeout(5);
			ResultSet rs = statement.executeQuery(sql);
			SQRes res = new SQRes(statement,rs);
			return res;
		} catch (SQLException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		return null;
	}

	public boolean isTable(String tableName)
	{
        //SQL文の作成
		String sql;
		sql = String.format("select name from sqlite_master where name='%s'",tableName);
		//SQLの実行
		SQRes res = query(sql);
		//nextの結果を返す
		try
		{
			return res.next();
		} finally
		{
			if(res != null)
				res.close();
		}
	}
	public boolean insert(String sql,Object[] list)
	{
		try
		{
			PreparedStatement pstmt = mConnection.prepareStatement(sql);
			for(int i=0;i<list.length;i++)
			{
				Object data = list[i];
				if(data instanceof  byte[])
					pstmt.setBytes(i+1, (byte[])list[i]);
				else if(data instanceof  InputStream)
					pstmt.setBinaryStream(i+1, (InputStream) list[i]);
				else
					pstmt.setString(i+1, list[i].toString());
			}
			pstmt.execute();
			pstmt.close();
		} catch (SQLException e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static String STR(String str)
	{
		//シングルクオートをシングルクオート二つにエスケーブ
		return str.replaceAll("'", "''");
	}
}