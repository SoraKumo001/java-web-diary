package to.pns.diary;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import to.pns.database.DBDiary;
import to.pns.database.DBDiary.DiaryData;
import to.pns.lib.SQLite;
import to.pns.lib.WebService;

/**
 * Servlet implementation class TopIndex
 */
@WebServlet("/index.html")
public class TopIndex extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private SQLite mSqlite;
	private DBDiary mDiaryDB;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public TopIndex() {
        super();
        // TODO Auto-generated constructor stub
    }
	@Override
	public void init() throws ServletException
	{
		super.init();
		mSqlite = WebService.openDB(this);
        mDiaryDB = new DBDiary(mSqlite);
 	}
    /**
     * @see 終了DB切断
     */
    @Override
	public void destroy()
	{
    	mSqlite = null;
    	mDiaryDB = null;
    	WebService.closeDB();
		super.destroy();
	}
    void outFile(HttpServletResponse response,String fileName) throws IOException
    {
       	// 要求文字コードのセット(Javaプログラムからはき出す文字コード)
	    response.setCharacterEncoding("UTF-8");
	    // 応答文字コードのセット(クライアントに通知する文字コードとファイルの種類)
	    response.setContentType("text/html");
		ServletOutputStream out = response.getOutputStream();
		FileInputStream in = new FileInputStream(getServletContext().getRealPath(fileName));
        byte[] buff = new byte[2048];
        int len;
        while ((len = in.read(buff, 0, buff.length)) != -1) {
            out.write(buff, 0, len);
        }
        in.close();
        out.close();
    }
    void outDiary(HttpServletRequest request,HttpServletResponse response) throws IOException
    {
    	// 要求文字コードのセット(Javaプログラムからはき出す文字コード)
	    response.setCharacterEncoding("UTF-8");
	    // 応答文字コードのセット(クライアントに通知する文字コードとファイルの種類)
	    response.setContentType("text/html; charset=UTF-8");
	    // 出力ストリームの取得
	    PrintWriter out = response.getWriter();

	    out.println(
	    	"<html>\n"+
	    	"<head>\n"+
	    	"    <meta charset=\"UTF-8\"/>\n"+
	        "    <link rel=\"alternate\" type=\"application/rss+xml\" title=\"RSS\" href=\"DiaryManager?cmd=rss\"/>\n"+
	    	"    <title>ふぉ日記</title>\n"+
	    	"</head>\n"+
	    	"<body>\n");

		//URLパラメータの取得
		Calendar cal = null;
		HashMap<String,String> params = WebService.getParameter(request);
		String cmd = params.get("p");
		if(cmd != null)
		{
			Pattern p = Pattern.compile("(\\d.+?)-(\\d*)");
	        Matcher m = p.matcher(cmd);
	        if(m.find())
	        {
	        	cal = Calendar.getInstance();
	        	cal.clear();
	        	String s1 = m.group(1);
	        	String s2 = m.group(2);
	        	cal.set(Integer.parseInt(s1), Integer.parseInt(s2)-1, 1);
	        }
		}
	    if(cal == null)
	    {
	    	List<String> months = mDiaryDB.getDiaryMonthList();
	    	for(String s : months)
	    	{
	    		out.format("\t<DIV><a href=\"?p=%s\">%s</A></DIV>\n", s,s);
	    	}
	    }
	    else
	    {
	    	List<DiaryData> list = mDiaryDB.getDiary(0,cal.getTimeInMillis(),0,false);
	    	for(DiaryData diary : list)
	    	{
	    		out.format("<DIV>%s</DIV>\n<DIV>%s</DIV>\n<DIV>%s</DIV>\n\n",
	    			diary.getDiaryDateString(),diary.getDiaryTitle(),diary.getDiaryMessage());

	    	}
	    }
	    out.println(
		    	"</body>\n"+
		    	"</html>\n");
	}
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String agent = request.getHeader("user-agent");
		if(agent != null)
			agent = agent.toLowerCase();
		else
			agent = "";
		if(agent.indexOf("bot") >= 0 || agent.indexOf("slurp") >= 0)
		{
			//ボット用HTMLの出力
			outDiary(request,response);
		}
		else
			outFile(response,"index.html");
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String agent = request.getHeader("user-agent");
		if(agent != null && agent.toLowerCase().indexOf("bot") >= 0)
		{
			//ボット用HTMLの出力
			outDiary(request,response);
		}
		else
			outFile(response,"index.html");
	}

}
