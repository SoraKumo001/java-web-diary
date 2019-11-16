package to.pns.diary;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import to.pns.database.DBDiary;
import to.pns.database.DBDiary.DiaryData;
import to.pns.database.DBUser;
import to.pns.database.DBUser.SessionData;
import to.pns.lib.SQLite;
import to.pns.lib.WebService;
import to.pns.servlet.UserManager;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Servlet implementation class Diary
 */
@WebServlet("/DiaryManager")
public class DiaryManager extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private SQLite mSqlite;
	private DBDiary mDiaryDB;
	private DBUser mUserDB;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public DiaryManager() {
        super();
    }

	@Override
	public void init() throws ServletException
	{
		super.init();
		mSqlite = WebService.openDB(this);
        mDiaryDB = new DBDiary(mSqlite);
        mUserDB = new DBUser(mSqlite);
	}
    /**
     * @see 終了DB切断
     */
    @Override
	public void destroy()
	{
    	mSqlite = null;
    	mDiaryDB = null;
    	mUserDB = null;
    	WebService.closeDB();
		super.destroy();
	}
	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		super.doOptions(request, response);
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "Content-Type,X-Session");
	}
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if(!command(request,response))
		{
			// 要求文字コードのセット(Javaプログラムからはき出す文字コード)
		    response.setCharacterEncoding("UTF-8");
		    // 応答文字コードのセット(クライアントに通知する文字コードとファイルの種類)
		    response.setContentType("text/plain; charset=UTF-8");
		    // 出力ストリームの取得
		    PrintWriter out = response.getWriter();

		    out.println("Diary");
		}
	}
	@Override
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
		    if(cmd.equals("rss"))
		    {
		    	getDiaryRss(response);
		    	return true;
		    }

			ObjectMapper mapper = new ObjectMapper();
			Map<?,?> param = null;
			try
			{
				param = mapper.readValue(request.getInputStream(),Map.class);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
			Map<String,Object> sendObjects = new HashMap<String,Object>();
			switch(cmd)
			{
			case "diary_write":
				writeDiary(param,sendObjects,sessionData);
				break;
			case "diary_get":
				getDiary(param,sendObjects,sessionData);
				break;
			case "diary_search":
				searchDiary(param,sendObjects,sessionData);
				break;
			case "diary_del":
				delDiary(param,sendObjects,sessionData);
				break;
			case "diary_date":
				getDiaryDate(param,sendObjects,sessionData);
				break;
			case "diary_date_last":
				getDiaryDateLast(param,sendObjects,sessionData);
				break;
			default:
				flag = false;
				break;
			}
			if(flag == true)
				WebService.outJson(response,sendObjects);


		}catch(Exception e){}

	    return flag;
	}

	private void searchDiary(Map<?, ?> param, Map<String, Object> sendObjects, SessionData sessionData)
	{
		String[] searchList = ((String)param.get("keyword")).toLowerCase().split(" ");

		List<DiaryData> list = mDiaryDB.getDiaryRSS();
		List<DiaryData> list2 = new ArrayList<DiaryData>();
		for(DiaryData diaryData : list)
		{
			String[] replaceStr = {
					"<.+?>", "",
					"&quot;","\"",
					"&nbsp;", " ",
					"&lt","<",
					"&gt",">",
					"&copy;","@",
					"&amp;","&"
			};
			//タグ、特殊文字の除去
			String message = diaryData.getDiaryMessage();
			for(int i=0;i<replaceStr.length;i+=2)
				message = message.replaceAll(replaceStr[i],replaceStr[+1]);
			message = message.toLowerCase();
			String title = diaryData.getDiaryTitle().toLowerCase();

			boolean flag = true;
			for(String s : searchList)
			{
				if(title.indexOf(s) == -1 && message.indexOf(s) == -1)
				{
					flag = false;
					break;
				}
			}
			if(flag)
			{
				list2.add(diaryData);
			}

		}
		sendObjects.put("diary",list2);
	}
	private void getDiaryRss(HttpServletResponse response)
	{
	    try
		{
			// 要求文字コードのセット(Javaプログラムからはき出す文字コード)
		    response.setCharacterEncoding("UTF-8");
		    // 応答文字
			response.setContentType("application/rss+xml");

		    PrintWriter out = response.getWriter();
			out.println(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<rss version=\"2.0\"\n" +
				"	xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
				"	xmlns:sy=\"http://purl.org/rss/1.0/modules/syndication/\"\n" +
				"	xmlns:admin=\"http://webns.net/mvcb/\"\n" +
				"	xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n"+
				"   <channel>");

			DateFormat output = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

			List<DiaryData> list = mDiaryDB.getDiaryRSS();
			for(DiaryData diaryData : list)
			{
				Calendar now = Calendar.getInstance();
				Calendar cal = diaryData.getDiaryDate();
				cal.set(Calendar.HOUR_OF_DAY,now.get(Calendar.HOUR_OF_DAY));
				cal.set(Calendar.MINUTE,now.get(Calendar.MINUTE));

				String item =
			    	String.format(
			    		"\t\t<item>\n"+
			    		"\t\t\t<link>%s?p=%d-%d-%d</link>\n"+
			    		"\t\t\t<title>%s</title>\n"+
			    		"\t\t\t<pubDate>%s</pubDate>\n"+
			    		"\t\t\t<category>%s</category>\n"+
			    		"\t\t\t<description>%s</description>\n"+
			    		"\t\t</item>",

			    		"http://diary.pns.mydns.jp/",cal.get(Calendar.YEAR),cal.get(Calendar.MONTH)+1,cal.get(Calendar.DATE),
			    		diaryData.getDiaryTitle(),
			    		output.format(cal.getTime()),
			    		"日記",
			    		diaryData.getDiaryMessage().replaceAll("<.+?>", "").replaceAll("&nbsp;", " "));
				out.println(item);
			}

			out.println("    </channel>\n</rss>");
			out.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	private void getDiaryDate(Map<?, ?> param, Map<String, Object> sendObjects, SessionData sessionData)
	{
		String date = null;
		int id;
		if(param.get("id") != null)
		{
			id = (Integer)param.get("id");
			long d = mDiaryDB.getDiaryDate(id,sessionData!=null);
			if(d > 0)
			{
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(d);
				date = String.format("%d/%d/%d",
						cal.get(Calendar.YEAR),cal.get(Calendar.MONTH)+1,cal.get(Calendar.DAY_OF_MONTH));
			}
		}
		sendObjects.put("date",date);
	}
	private void getDiaryDateLast(Map<?, ?> param, Map<String, Object> sendObjects, SessionData sessionData)
	{
		String date = null;
		long d = mDiaryDB.getDiaryDate(sessionData!=null);
		Calendar cal = Calendar.getInstance();
		if(d > 0)
		{
			cal.setTimeInMillis(d);
		}
		date = String.format("%d/%d/%d",
				cal.get(Calendar.YEAR),cal.get(Calendar.MONTH)+1,cal.get(Calendar.DAY_OF_MONTH));
		sendObjects.put("date",date);
	}
	private void delDiary(Map<?, ?> param, Map<String,Object> sendObjects, SessionData sessionData)
	{
		boolean flag = false;
		if(sessionData != null)
		{
			int id = 0;
			if(param.get("id") != null)
			{
				id = (Integer)param.get("id");
				flag = mDiaryDB.delDiary(id);
			}
		}
		sendObjects.put("return",flag);
	}

	private void getDiary(Map<?, ?> param, Map<String,Object> sendObjects, SessionData sessionData)
	{
		try
		{
			List<DiaryData> list;

			int id = 0;
			long dateStart = 0;
			long dateEnd = 0;
			if(param.get("id") != null)
				id = (Integer)param.get("id");
			if(param.get("date") != null)
				dateStart = DateFormat.getDateInstance().parse((String)param.get("date")).getTime();
			if(param.get("dateEnd") != null)
				dateEnd = DateFormat.getDateInstance().parse((String)param.get("dateEnd")).getTime();


			list = mDiaryDB.getDiary(id,dateStart,dateEnd,sessionData!=null);
			sendObjects.put("diary",list);

		} catch (ParseException e)
		{
			e.printStackTrace();
		}
	}

	private void writeDiary(Map<?, ?> param, Map<String,Object> sendObjects, SessionData sessionData)
	{
		//メッセージの書き込み
		try
		{
			if(sessionData!=null)
			{
				int id = (Integer)param.get("id");
				String title = (String)param.get("title");
				String message = (String)param.get("message");
				long date = DateFormat.getDateInstance().parse((String)param.get("date")).getTime();
				boolean visible = (Boolean)param.get("visible");
				int rid = mDiaryDB.setDiary(id, title, message, date, visible,0x7fffffff);
				sendObjects.put("id",rid);
			}
			else
				sendObjects.put("id",-1);

		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}



}
