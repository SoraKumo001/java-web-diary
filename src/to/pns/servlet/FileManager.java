package to.pns.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import to.pns.database.DBFile;
import to.pns.database.DBFile.DBStream;
import to.pns.database.DBUser;
import to.pns.database.DBUser.SessionData;
import to.pns.lib.SQLite;
import to.pns.lib.WebService;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Servlet implementation class FileManager
 */
@WebServlet("/FileManager")
public class FileManager extends HttpServlet {
	private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
	DBFile mFileDB;

	private SQLite mSqlite;
	private DBUser mUserDB;
    public FileManager() {
        super();

    }
    /**
     * @see 初期化DB接続
     */
	@Override
	public void init() throws ServletException
	{
		super.init();
		mSqlite = WebService.openDB(this);
        mFileDB = new DBFile(mSqlite);
        mUserDB = new DBUser(mSqlite);

	}
    /**
     * @see 終了DB切断
     */
    @Override
	public void destroy()
	{
    	WebService.closeDB();
		super.destroy();
	}

	public FileManager(SQLite sqlite)
	{
		mFileDB = new DBFile(sqlite);
	}
	void dirList(Map<?,?> param, HttpServletResponse response, SessionData sessionData)
	{
		try
		{
			Map<String,Object> map = new HashMap<String,Object>();
			if(sessionData != null)
			{
				int pid = (Integer)param.get("id");
				ArrayList<DBFile.FileData> dir = mFileDB.getDir(pid);
				map.put("dir",dir);
			}
			WebService.outJson(response,map);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	void fileList(Map<?,?> param,HttpServletResponse response, SessionData sessionData)
	{
		try
		{
			Map<String,Object> map = new HashMap<String,Object>();
			if(sessionData != null)
			{
				int pid = (Integer)param.get("id");
				ArrayList<DBFile.FileData> files = mFileDB.getFiles(pid);
				map.put("files",files);
			}
			WebService.outJson(response,map);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	void dirNew(Map<?,?> param, HttpServletResponse response, SessionData sessionData)
	{
		try
		{
			Map<String,Object> map = new HashMap<String,Object>();
			if(sessionData != null)
			{
				String name = (String)param.get("name");
				int pid = (Integer)param.get("pid");
				int id = mFileDB.createDir(pid, name);
				map.put("id",id);
			}
			WebService.outJson(response,map);

		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	void download(int id, HttpServletResponse response)
	{
		try
		{
			DBStream stream = mFileDB.download(id);
			String fileName = stream.getName();
			String fileType = null;
			int point = fileName.lastIndexOf(".");
		    if (point != -1) {
		        fileType = fileName.substring(point + 1).toLowerCase();
		    }
		    String httpType = "application/octet-stream";
		    String httpDisposition= "inline;";
		    switch(fileType)
		    {
		    case "png":
		    	httpType = "image/png";
		    	break;
		    case "svg":
		    	httpType = "image/svg+xml";
		    	break;
		    case "jpeg":
		    case "jpg":
		    	httpType = "image/jpeg";
		    	break;
		    case "gif":
		    	httpType = "image/gif";
		    	break;
		    default:
		    	httpDisposition= "attachment;";
		    	break;
		    }

			response.setContentType(httpType);
			response.setHeader("Content-Disposition", httpDisposition+" filename*=utf-8'jp'"+
					URLEncoder.encode( stream.getName(),"UTF8" ).replaceAll("\\+"," ")+"");
			InputStream in = stream.getStream();
			ServletOutputStream out = response.getOutputStream();
			byte[] buff = new byte[2048];
			int size;
			while((size = in.read(buff))!=-1)
			{
				out.write(buff,0,size);
			}
			stream.close();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	void fileRename(Map<?,?> param, HttpServletResponse response, SessionData sessionData)
	{
		try
		{
			Map<String,Object> map = new HashMap<String,Object>();
			if(sessionData != null)
			{
				String name = (String)param.get("name");
				int id = (Integer)param.get("id");
				boolean ret = mFileDB.renameDir(id, name);
				map.put("return",ret);
			}
			WebService.outJson(response,map);

		} catch (Exception e)
		{
			e.printStackTrace();
		}

	}
	void fileId(Map<?,?> param, HttpServletResponse response, SessionData sessionData)
	{
		try
		{
			Map<String,Object> map = new HashMap<String,Object>();
			if(sessionData != null)
			{
				String name = (String)param.get("name");
				int pid = (Integer)param.get("pid");
				int id = mFileDB.getFileId(pid, name);
				map.put("id",id);
			}
			WebService.outJson(response,map);

		} catch (Exception e)
		{
			e.printStackTrace();
		}

	}
	void fileMove(Map<?,?> param, HttpServletResponse response, SessionData sessionData)
	{
		try
		{
			boolean ret = false;
			Map<String,Object> map = new HashMap<String,Object>();
			if(sessionData != null)
			{
				int sid = (Integer)param.get("src_id");
				int did = (Integer)param.get("dest_id");

				ret = mFileDB.moveFile(sid,did);
			}

			map.put("return",ret);

			WebService.outJson(response,map);

		} catch (Exception e)
		{
			e.printStackTrace();
		}

	}
	void fileDel(Map<?,?> param, HttpServletResponse response, SessionData sessionData)
	{
		try
		{
			boolean ret = false;
			if(sessionData != null)
			{
				int id = (Integer)param.get("id");
				ret = mFileDB.delFile(id);
			}

			Map<String,Object> map = new HashMap<String,Object>();
			map.put("return",ret);
			WebService.outJson(response,map);

		} catch (Exception e)
		{
			e.printStackTrace();
		}

	}

	private void upload(HashMap<String, String> params, HttpServletRequest request,HttpServletResponse response, SessionData sessionData) throws IOException
	{
		response.setHeader("Content-type","text/event-stream; charset=utf-8");
		response.setHeader("Transfer-encoding","chunked");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Connection", "no-cache");
		PrintWriter out = response.getWriter();

		if(sessionData != null)
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			InputStream is = request.getInputStream();
			try
			{
				byte[] content = new byte[ 1024 ];
				int bytesRead = -1;
				int size = 0;
				while( ( bytesRead = is.read( content ) ) != -1 ) {
				    size+=bytesRead;

					//10MBを超えたら転送を打ち切る
					if(size > 10*1024*1024)
					{
						out.println(-1);
						out.flush();
						size = -1;
						break;
					}
				    baos.write( content, 0, bytesRead );
					out.println(size);
					out.flush();

				}
				if(size > -1)
					mFileDB.upload(Integer.parseInt((String)params.get("pid")), (String)params.get("filename"), Calendar.getInstance().getTimeInMillis(),baos.toByteArray());

			} catch (Exception e)
			{
				e.printStackTrace();
				out.println(-1);
			}
			baos.close();
		}
		else
		{
			out.println(-1);

		}
		out.close();
	}


	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if(!command(request,response))
		{
			// 要求文字コードのセット(Javaプログラムからはき出す文字コード)
		    response.setCharacterEncoding("UTF-8");
		    // 応答文字コードのセット(クライアントに通知する文字コードとファイルの種類)
		    response.setContentType("text/plain; charset=UTF-8");
		    // 出力ストリームの取得
		    PrintWriter out = response.getWriter();

		    out.println("FileManager");
		}
	}
	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		super.doOptions(request, response);
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "Content-Type,X-Session");
	}
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		command(request,response);
	}
	public boolean command(HttpServletRequest request, HttpServletResponse response)
	{
		//Ajaxのドメイン越えを許可
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "Content-Type,X-Session");
		//URLパラメータの取得
		HashMap<String,String> params = WebService.getParameter(request);
		SessionData sessionData = UserManager.getSession(mUserDB, request);

	    boolean flag = true;
		try
		{

		    String cmd = params.get("cmd");
		    if(cmd == null)
		    	return false;
		    System.out.println(cmd);

			if(cmd.equals("file_upload"))
			{
				upload(params,request,response,sessionData);

			}
			else if(cmd.equals("file_download"))
			{
				download(Integer.parseInt((String)params.get("id")),response);
			}
			else
			{
				ObjectMapper mapper = new ObjectMapper();
				Map<?,?> param = null;
				try
				{
					param = mapper.readValue(request.getInputStream(),Map.class);
				} catch (Exception e)
				{
					e.printStackTrace();
				}
				switch(cmd)
				{
				case "dir":
					dirList(param,response,sessionData);
					break;
				case "files":
					fileList(param,response,sessionData);
					break;
				case "dir_new":
					dirNew(param,response,sessionData);
					break;
				case "file_id":
					fileId(param,response,sessionData);
					break;
				case "file_rename":
					fileRename(param,response,sessionData);
					break;
				case "file_del":
					fileDel(param,response,sessionData);
					break;
				case "file_move":
					fileMove(param,response,sessionData);
					break;
				default:
					flag = false;
					break;
				}
			}

		} catch (Exception e)
		{
			e.printStackTrace();
		}
	    return flag;
	}

}
