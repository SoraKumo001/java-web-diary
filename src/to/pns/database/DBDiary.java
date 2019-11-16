package to.pns.database;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import to.pns.lib.SQLite;
import to.pns.lib.SQLite.SQRes;


public class DBDiary
{
	//日記データ用テーブル名
	final static String TABLE_NAME = "sys_diary";
	//日記データクラス
	public class DiaryData
	{
		public DiaryData(int id,String title,String message,long date,boolean visible,int priority)
		{
			mDiaryId = id;
			mDiaryTitle = title;
			mDiaryVisible = visible;
			mDiaryDate = date;
			mDiaryMessage = message;
			mDiaryVisible = visible;
			mDiaryPriority = priority;
		}

		public int getDiaryId()
		{
			return mDiaryId;
		}
		public String getDiaryTitle()
		{
			return mDiaryTitle;
		}
		public String getDiaryMessage()
		{
			return mDiaryMessage;
		}
		public String getDiaryDateString()
		{
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(mDiaryDate);
			return String.format("%4d/%02d/%02d",cal.get(Calendar.YEAR),cal.get(Calendar.MONTH)+1,cal.get(Calendar.DATE));
		}
		public Calendar getDiaryDate()
		{
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(mDiaryDate);
			return cal;
		}
		public boolean isDiaryVisible()
		{
			return mDiaryVisible;
		}
		public int getDiaryPriority()
		{
			return mDiaryPriority;
		}

		private int mDiaryId;
		private String mDiaryTitle;
		private String mDiaryMessage;
		private long mDiaryDate;
		private boolean mDiaryVisible;
		private int mDiaryPriority;
	}


	SQLite mSqlite;
	public DBDiary(SQLite db)
	{
		mSqlite = db;
		if(!db.isTable(TABLE_NAME))
		{
			String sql;
			//テーブル作成
			sql = String.format("create table %s(diary_id integer primary key,diary_title text,diary_message text,diary_date integer,diary_visible integer,diary_priority integer);",TABLE_NAME);
			db.exec(sql);
			//インデックスの作成
			sql = String.format("create index nameindex on %s(diary_date);",TABLE_NAME);
			db.exec(sql);
			//インフォメーション表示用
			setDiary(1,"インフォメーション","",0,true,0);

		}
	}
	boolean isDiary(int id)
	{
		String sql = String.format("select diary_id from %s where diary_id='%d'",
				TABLE_NAME,id);
		boolean flag = false;

		SQRes res = mSqlite.query(sql);
		if(res.next())
			flag = true;
		res.close();

		return flag;
	}
	public int setDiary(int id,String title,String message,long date,boolean visible,int priority)
	{
		String sql;
		if(id == 0 || !isDiary(id))
		{
			//新規作成
			try
			{
				sql = String.format("insert into %s values(%s,'%s','%s','%d','%d','%d')",
						TABLE_NAME,id!=0?id:"null",SQLite.STR(title),SQLite.STR(message),date,visible?1:0,priority);
				//mSqlite.exec("begin;");
				mSqlite.exec(sql);
				id = -1;
				SQRes res = mSqlite.query("select last_insert_rowid();");
				id = res.getInt(1);
				//mSqlite.exec("commit;");
				res.close();
			} catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			//内容変更
			sql = String.format("update %s set diary_title='%s',diary_message='%s',diary_date='%d',diary_visible='%d',diary_priority='%d' where diary_id='%d'",
					TABLE_NAME,SQLite.STR(title),SQLite.STR(message),date,visible?1:0,priority,id);
			mSqlite.exec(sql);
		}
		updatePriority(date);
		return id;
	}
	public boolean delDiary(int id)
	{
		String sql;
		sql = String.format("delete from %s where diary_id=%d",
			TABLE_NAME,id);
		return mSqlite.exec(sql);
	}
	public boolean updatePriority(long date)
	{
		//表示順序制御用数値の正規化
		String sql = String.format("select diary_id from %s where diary_date='%d' order by diary_priority;",
				TABLE_NAME,date);

		try
		{
			//mSqlite.exec("begin;");

			SQRes res = mSqlite.query(sql);
			ArrayList<Integer> list = new ArrayList<Integer>();
			while(res.next())
				list.add(res.getInt(1));
			int priority = 100;
			for(Integer id : list)
			{
				sql = String.format("update %s set diary_priority='%d' where diary_id='%d'",
						TABLE_NAME,priority,id);
				mSqlite.exec(sql);
				priority += 100;
			}
			res.close();
			//mSqlite.exec("commit;");
			return true;
		} catch (SQLException e)
		{
			e.printStackTrace();
		}
		return false;

	}
	public List<DiaryData> getDiary(int id,long dateStart,long dateEnd,boolean admin)
	{
		ArrayList<DiaryData> list = new ArrayList<DiaryData>();
		String sql;

		if(id > 0)
		{
			if(admin)
				sql = String.format("select * from %s where diary_id='%d';",TABLE_NAME,id);
			else
				sql = String.format("select * from %s where diary_id='%d' and diary_visible=1;",TABLE_NAME,id);
		}
		else
		{
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(dateStart);

			Calendar calStart = Calendar.getInstance();
			calStart.clear();
			calStart.set(cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DATE));
			Calendar calEnd = Calendar.getInstance();
			calEnd.clear();
			if(dateEnd == 0)
				calEnd.set(cal.get(Calendar.YEAR),cal.get(Calendar.MONTH)+1,1);
			else
			{
				cal.setTimeInMillis(dateEnd);
				calEnd.set(cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DATE)+1);
			}
			//月指定
			if(admin)
				sql = String.format("select * from %s where diary_id > 1 and diary_date >= '%d' and diary_date < '%d' order by diary_date desc,diary_priority;",
					TABLE_NAME,calStart.getTimeInMillis(),calEnd.getTimeInMillis());
			else
				sql = String.format("select * from %s where diary_id > 1 and diary_visible=1 and diary_date >= '%d' and diary_date < '%d' order by diary_date desc,diary_priority;",
					TABLE_NAME,calStart.getTimeInMillis(),calEnd.getTimeInMillis());
		}
		try
		{
			SQRes res = mSqlite.query(sql);
			while(res.next())
			{
				DiaryData diaryData =
					new DiaryData(res.getInt(1),res.getString(2),res.getString(3),
							res.getLong(4),res.getBoolean(5),res.getInt(6));
				list.add(diaryData);
			}
			res.close();
		} catch (SQLException e)
		{
			e.printStackTrace();
		}
		return list;
	}
	public List<DiaryData> getDiaryRSS()
	{
		ArrayList<DiaryData> list = new ArrayList<DiaryData>();
		String sql;

		sql = String.format("select * from %s where diary_id > 1 and diary_visible=1 order by diary_date desc,diary_priority;",
				TABLE_NAME);

		try
		{
			SQRes res = mSqlite.query(sql);
			while(res.next())
			{
				DiaryData diaryData =
						new DiaryData(res.getInt(1),res.getString(2),res.getString(3),
								res.getLong(4),res.getBoolean(5),res.getInt(6));
				list.add(diaryData);
			}
			res.close();
		} catch (SQLException e)
		{
			e.printStackTrace();
		}
		return list;
	}
	public List<String> getDiaryMonthList()
	{
		ArrayList<String> list = new ArrayList<String>();
		String sql;

		sql = String.format("select distinct strftime('%%Y-%%m',diary_date/1000, 'unixepoch','localtime') as date from %s where diary_id>1 and diary_visible=1 order by date desc;",
				TABLE_NAME);

		SQRes res = mSqlite.query(sql);
		while(res.next())
		{
			list.add(res.getString(1));
		}
		res.close();
		return list;
	}

	public long getDiaryDate(int id,boolean admin)
	{
		if(id > 0)
		{
			SQRes res = null;
			try
			{
				String sql;
				sql = String.format("select diary_date from %s where diary_id='%d';",TABLE_NAME,id);
				res = mSqlite.query(sql);
				if(res.next())
					return res.getLong(1);
			} catch (SQLException e)
			{
				e.printStackTrace();
			}finally
			{
				if(res != null)
					res.close();
			}
		}
		return 0;
	}
	public long getDiaryDate(boolean admin)
	{
		SQRes res = null;
		try
		{
			String sql;
			sql = String.format("select max(diary_date) from %s;",TABLE_NAME);
			res = mSqlite.query(sql);
			if(res.next())
				return res.getLong(1);
		} catch (SQLException e)
		{
			e.printStackTrace();
		}finally
		{
			if(res != null)
				res.close();
		}
		return 0;
	}

}
