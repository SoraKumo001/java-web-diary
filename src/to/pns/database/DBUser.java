package to.pns.database;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import to.pns.lib.SQLite;
import to.pns.lib.SQLite.SQRes;

public class DBUser
{
	final static String TABLE_NAME = "sys_user";
	final static String TABLE_NAME2 = "sys_user_session";

	public class UserData
	{
		public UserData(int id,String name,boolean enable)
		{
			mUserId = id;
			mUserName = name;
			mUserEnable = enable;
		}
		public int getUserId()
		{
			return mUserId;
		}
		public String getUserName()
		{
			return mUserName;
		}
		public boolean getUserEnable()
		{
			return mUserEnable;
		}
		private int mUserId;
		private String mUserName;
		private boolean mUserEnable;
	}


	SQLite mSqlite;
	public DBUser(SQLite db)
	{
		if(!db.isTable(TABLE_NAME))
		{
			String sql;
			//テーブル作成
			sql = String.format("create table %s(user_id integer primary key,user_name text unique,user_pass text,user_enable integer);",TABLE_NAME);
			db.exec(sql);
			sql = String.format("create table %s(session_id integer primary key,session_hash text,session_date integer,user_id integer);",TABLE_NAME2);
			db.exec(sql);
			//管理用初期ユーザ作成
			sql = String.format("insert into %s values(null,'root','',1)",TABLE_NAME);
			db.exec(sql);


		}
		mSqlite = db;
	}
	public boolean close()
	{
		if(mSqlite == null)
			return false;
		mSqlite.close();
		mSqlite = null;
		return true;
	}
	public int addUser(String name,String pass,boolean enable)
	{
		synchronized(this)
		{
			String sql = String.format("insert into %s values(null,'%s','%s',%d)",
			TABLE_NAME,SQLite.STR(name),SQLite.STR(pass),enable?1:0);
			int id;
			try
			{
				mSqlite.exec(sql);
				SQRes res = mSqlite.query("select last_insert_rowid();");
				id = res.getInt(1);
				res.close();
				return id;
			} catch (SQLException e)
			{
				e.printStackTrace();
			}
			return -1;
		}
	}
	public boolean setUser(int userId,String name,String pass,boolean enable)
	{
		String sql;
		//パスワードの変更をするかどうか
		if(pass.equals("*"))
			sql = String.format("update %s set user_name='%s',user_enable=%d where user_id=%d",
					TABLE_NAME,SQLite.STR(name),enable?1:0,userId);
		else
			sql = String.format("update %s set user_name='%s',user_pass='%s',user_enable=%d where user_id=%d",
				TABLE_NAME,SQLite.STR(name),SQLite.STR(pass),enable?1:0,userId);
		return mSqlite.exec(sql);
	}
	public boolean delUser(int id)
	{
		String sql = String.format("delete from %s where user_id='%d'",
				TABLE_NAME,id);
		return mSqlite.exec(sql);
	}
	public List<UserData> getUserList()
	{
		ArrayList<UserData> list = new ArrayList<UserData>();
		String sql = String.format("select user_id,user_name,user_enable from %s order by user_id;",
				TABLE_NAME);
		SQRes res = mSqlite.query(sql);
		try
		{
			while(res.next())
			{
				UserData userData = new UserData(res.getInt(1),res.getString(2),res.getBoolean(3));
				list.add(userData);
			}
		} catch (SQLException e)
		{
			e.printStackTrace();
		}
		return list;
	}
	public class SessionData
	{
		private String mSession;
		private int mUserId;
		private String mUserName;
		public SessionData(String session,int id,String name)
		{
			mSession = session;
			mUserId = id;
			mUserName = name;
		}
		public String getSession()
		{
			return mSession;
		}
		public int getUserId()
		{
			return mUserId;
		}
		public String getUserName()
		{
			return mUserName;
		}
	}
	public SessionData getSession(String session)
	{
		synchronized(this)
		{
			String sql;
			Calendar cal = Calendar.getInstance();
			long t = cal.getTimeInMillis()-6*24*60*60*1000; //6日

			//期限切れのセッションを削除
			sql = String.format("delete from %s where session_date < %d;",TABLE_NAME2,t);
			mSqlite.exec(sql);
			//セッションの確認
			sql = String.format("select user_id,user_name,session_date from %s natural join %s where user_enable=1 and session_hash='%s';",
					TABLE_NAME,TABLE_NAME2,SQLite.STR(session));
			SQRes res = null;
			try
			{
				res = mSqlite.query(sql);
				if(!res.next())
					return null;
				int userId = res.getInt(1);
				String userName = res.getString(2);
				long sessionDate = res.getLong(3);
				//30分経過していたらセッションを再度作成
				if(t-sessionDate > 30*60*1000)
					session = createSession(userId);
				return new SessionData(session,userId,userName);
			} catch (SQLException e)
			{
				e.printStackTrace();
			}finally
			{
				if(res != null)
					res.close();
			}
			return null;
		}
	}
	public String createSession(int id)
	{
		try
		{
			//セッション用ハッシュの作成
			Calendar cal = Calendar.getInstance();
			String s = ""+cal.getTimeInMillis()+id;
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			byte[] result = md5.digest(s.getBytes());
			StringBuffer dec = new StringBuffer("");
			for (int i = 0; i < result.length; i++) {
				dec.append(String.format("%02X",result[i]));
			}
			String hash = dec.toString();
			//セッション情報を保存
			String sql = String.format("insert into %s values(null,'%s',%d,%d)",
					TABLE_NAME2,hash,cal.getTimeInMillis(),id);
			mSqlite.exec(sql);
			return hash;
		} catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	public String login(String user,String pass)
	{
		String sql;
		try
		{
			//ユーザIDの照合
			sql = String.format("select user_id from %s where user_name='%s' and user_pass='%s' and user_enable=1;",
					TABLE_NAME,SQLite.STR(user),SQLite.STR(pass));
			SQRes res = mSqlite.query(sql);
			if(!res.next())
				return null;
			int id = res.getInt(1);
			res.close();
			//セッションの作成
			return createSession(id);
		}catch (SQLException e)
		{
			e.printStackTrace();
		}
		return null;
	}

}
