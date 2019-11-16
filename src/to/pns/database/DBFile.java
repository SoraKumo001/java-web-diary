package to.pns.database;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;

import to.pns.lib.SQLite;
import to.pns.lib.SQLite.SQRes;



public class DBFile
{
	public class FileData
	{
		public FileData(int id,int pid,String name,ArrayList<FileData> childs)
		{
			mId = id;
			mParentId = pid;
			mName = name;
			mChilds = childs;
		}
		public FileData(int id,int pid,int kind,String name,int size,long date)
		{
			mId = id;
			mParentId = pid;
			mKind = kind;
			mName = name;
			mDate = date;
			mChilds = null;
			mSize = size;
		}
		
		public int getId()
		{
			return mId;
		}
		public int getParent()
		{
			return mParentId;
		}
		public ArrayList<FileData> getChilds()
		{
			return mChilds;
		}
		public String getName()
		{
			return mName;
		}
		public long getDate()
		{
			return mDate;
		}
		public int getSize()
		{
			return mSize;
		}
		public int getKind()
		{
			return mKind;
		}
		int mId;
		int mParentId;
		String mName;
		int mSize;
		long mDate;
		int mKind;
		boolean mChild;
		ArrayList<FileData> mChilds;
	}
	
	
	
	final static String TABLE_NAME = "sys_file";
	SQLite mSqlite;
	public DBFile(SQLite db)
	{
		if(!db.isTable(TABLE_NAME))
		{
			String sql;
			sql = String.format("create table %s(file_id integer primary key,file_parent int,file_kind int,file_name text,file_size int,file_date int,file_data blob);",TABLE_NAME);
			db.exec(sql);
			sql = String.format("insert into %s values(null,0,0,'',0,%d,null)",TABLE_NAME,Calendar.getInstance().getTimeInMillis());
			db.exec(sql);
		}
		mSqlite = db;
	}
	public boolean upload(int parent,String name,long date,byte[] data)
	{
		String sql;
		Object[] values = {data};
		int id = getFileId(parent,name);
		if(id > 0)
		{
			sql = String.format("update %s set file_size=%d,file_date=%d,file_data=? where file_id=%d",
					TABLE_NAME,data.length,date,id);
		}
		else
		{
			sql = String.format("insert into %s values(null,%d,1,'%s',%d,%d,?)",
					TABLE_NAME,parent,SQLite.STR(name),data.length,date);
		}
		mSqlite.insert(sql, values);
		return true;
	}
	public boolean upload(int parent,String name,long date,InputStream is)
	{
		String sql;
		sql = String.format("insert into %s values(null,%d,1,'%s',%d,%d,?)",
				TABLE_NAME,parent,SQLite.STR(name),0,date);
		Object[] values = {is};
		mSqlite.insert(sql, values);
		return true;
	}
	public class DBStream
	{
		SQRes mRes;
		String mName;
		InputStream mInputStream;
		public DBStream(SQRes res,String name,InputStream is)
		{
			mRes = res;
			mName = name;
			mInputStream = is;
		}
		public void close()
		{
			try
			{
				mInputStream.close();
				mRes.close();
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		public InputStream getStream()
		{
			return mInputStream;
		}
		public String getName()
		{
			return mName;
		}
	}
	public DBStream download(int id)
	{
		SQRes res = null;
		try
		{
			String sql = String.format("select file_name,file_data from %s where file_id=%d"
					,TABLE_NAME,id);
			res = mSqlite.query(sql);
			if(res.next())
			{
				return new DBStream(res,res.getString(1),res.getBinaryStream(2));
			}
		}finally
		{
			if(res != null)
				res.close();
		}
		return null;
	}
	public ArrayList<FileData> getFiles(int pid)
	{
		ArrayList<FileData> list = new ArrayList<FileData>();
		
		String sql;
		sql = String.format("select file_id,file_kind,file_name,file_size,file_date from %s where file_parent=%d",TABLE_NAME,pid);
		SQRes res = mSqlite.query(sql);
		try
		{
			while(res.next())
			{
				FileData file = new FileData(res.getInt(1),pid,res.getInt(2),res.getString(3),res.getInt(4),res.getLong(5));
				list.add(file);
			}
		} catch (SQLException e)
		{
			e.printStackTrace();
		}finally
		{
			res.close();
			
		}
		return list;
	}
	public ArrayList<FileData> getDir(int pid)
	{
		ArrayList<FileData> list = new ArrayList<FileData>();
		
		String sql;
		sql = String.format("select file_id,file_name from %s where file_kind=0 and file_parent=%d order by file_name",TABLE_NAME,pid);
		SQRes res = mSqlite.query(sql);
		try
		{
			while(res.next())
			{
				int id = res.getInt(1);
				ArrayList<FileData> childs = getDir(id);
				FileData dir = new FileData(id,pid,res.getString(2),childs);
				list.add(dir);
			}
		} catch (SQLException e)
		{
			e.printStackTrace();
		}finally
		{
			close(res);
			
		}
		return list;
	}
	public boolean close()
	{
		if(mSqlite == null)
			return false;
		mSqlite.close();
		mSqlite = null;
		return true;
	}
	public boolean close(SQRes res)
	{
		if(res != null)
		{
			res.close();
		}
		return true;
	}
	public int getFileId(int pid,String name)
	{
		String[] splitDir = name.split("/");
		if(splitDir.length == 0)
			return -1;
		int index = 0;
		if(splitDir[0].length() == 0)
			pid = 0;
		
		SQRes res = null;
		try
		{
			for(;index < splitDir.length;index++)
			{
				String sql = String.format("select file_id from %s where file_parent=%d and file_name='%s'"
						,TABLE_NAME,pid,SQLite.STR(splitDir[index]));
				res = mSqlite.query(sql);
				pid = -1;
				if(res.next())
				{
					pid = res.getInt(1);
				}
				res.close();
				if(pid == -1)
					break;
			}
			if(index == splitDir.length)
				return pid;
		} catch (SQLException e)
		{
			e.printStackTrace();
		}
		return -1;
	}
	public boolean isFile(int pid,String name)
	{
		String[] splitDir = name.split("/");
		if(splitDir.length == 0)
			return false;
		int index = 0;
		if(splitDir[0].length() == 0)
			pid = 0;
		
		SQRes res = null;
		try
		{
			for(;index < splitDir.length;index++)
			{
				String sql = String.format("select file_id from %s where file_parent=%d and file_name='%s'"
						,TABLE_NAME,pid,SQLite.STR(splitDir[index]));
				res = mSqlite.query(sql);
				pid = -1;
				if(res.next())
				{
					pid = res.getInt(1);
				}
				res.close();
				if(pid == -1)
					break;
			}
			return index == splitDir.length;
		} catch (SQLException e)
		{
			e.printStackTrace();
		}
		return false;
	}
	public boolean isFile(int id)
	{
		SQRes res = null;
		try
		{
			String sql = String.format("select file_id from %s where file_id=%d"
					,TABLE_NAME,id);
			res = mSqlite.query(sql);
			if(res.next())
				return true;
		}finally
		{
			if(res != null)
				res.close();
		}
		return false;
	}
	public boolean isDir(int id)
	{
		SQRes res = null;
		try
		{
			String sql = String.format("select file_id from %s where file_id=%d and file_kind=0"
					,TABLE_NAME,id);
			res = mSqlite.query(sql);
			if(res.next())
				return true;
		}finally
		{
			if(res != null)
				res.close();
		}
		return false;
	}
	public int createDir(int pid,String name)
	{
		String[] splitDir = name.split("/");
		if(splitDir.length == 0)
			return -1;
		if(splitDir[0].length() == 0)
			pid = 0;
		
		int i;
		for(i=0;i<splitDir.length;i++)
		{
			
			//既に存在しているか？
			int id = getFileId(pid,splitDir[i]);
			if(id == -1)
			{
				//ディレクトリの作成
				String sql = String.format("insert into %s values(null,%d,0,'%s',0,%d,null);",
						TABLE_NAME,pid,SQLite.STR(splitDir[i]),Calendar.getInstance().getTimeInMillis());
				mSqlite.exec(sql);
				pid = getFileId(pid,splitDir[i]);
			}
			else
				pid = id;
		}
		if(i == splitDir.length)
			return pid;
		return -1;
	}
	public int getParentId(int id)
	{
		String sql = String.format("select file_parent from %s where file_id=%d;",
				TABLE_NAME,id);		
		SQRes res = mSqlite.query(sql);
		try
		{
			if(res.next())
			{
				return res.getInt(1);
			}
		} catch (SQLException e)
		{
			e.printStackTrace();
		}finally
		{
			if(res != null)
				res.close();
		}
		return -1;
	}
	public boolean moveFile(int srcId,int destId)
	{
		//既に存在しているか？
		if(!isFile(srcId) || !isDir(destId) || srcId==destId)
			return false;
		//移動先が配下ではないか確認
		int id = destId;
		while((id = getParentId(id)) != 0)
		{
			if(id == srcId)
				return false;
		}
		//ディレクトリの移動
		String sql = String.format("update %s set file_parent=%d where file_id=%d;",
				TABLE_NAME,destId,srcId);
		mSqlite.exec(sql);
		return true;
	}
	public boolean delFile(int id)
	{
		//既に存在しているか？
		if(id <= 1 || !isFile(id))
			return false;
		ArrayList<FileData> dir = getDir(id);
		for(FileData d : dir)
		{
			delFile(d.getId());
		}
		
		//ディレクトリの作成
		String sql = String.format("delete from %s where file_id='%d';",
				TABLE_NAME,id);
		mSqlite.exec(sql);
		return true;
	}
	public boolean renameDir(int id,String name)
	{
		//既に存在しているか？
		if(!isFile(id))
			return false;
		//ディレクトリの作成
		String sql = String.format("update %s set file_name='%s' where file_id='%d';",
				TABLE_NAME,SQLite.STR(name),id);
		mSqlite.exec(sql);
		return true;
	}

}