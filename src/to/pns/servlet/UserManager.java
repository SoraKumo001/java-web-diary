package to.pns.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import to.pns.database.DBUser;
import to.pns.database.DBUser.SessionData;
import to.pns.database.DBUser.UserData;
import to.pns.lib.WebService;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Servlet implementation class UserManager
 */
@WebServlet("/UserManager")
public class UserManager extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private DBUser mUserDB;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public UserManager() {
        super();
     }
    /**
     * @see 初期化DB接続
     */
	@Override
	public void init() throws ServletException
	{
		super.init();
        mUserDB = new DBUser(WebService.openDB(this));
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

		    out.println("UserManager");
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		command(request,response);
	}
	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		super.doOptions(request, response);
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "Content-Type,X-Session,Connection");
		response.setHeader("Connection", "close");
	}
	public boolean command(HttpServletRequest request, HttpServletResponse response)
	{
		//Ajaxのドメイン越えを許可
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "Content-Type,X-Session,Connection");
		response.setHeader("Connection", "close");
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
			case "user_list":
				getUser(param,response,sessionData);
				break;
			case "user_del":
				delUser(param,response,sessionData);
				break;
			case "user_set":
				setUser(param,response,sessionData);
				break;
			case "user_login":
				loginUser(param,response);
				break;
			case "user_session":
				sessionUser(param,response);
				break;
			default:
				flag = false;
				break;
			}

		} catch (Exception e)
		{
			e.printStackTrace();
		}
	    return flag;
	}
	private void sessionUser(Map<?, ?> param, HttpServletResponse response)
	{
		String session = (String) param.get("session");
		SessionData sessionData = mUserDB.getSession(session);
		HashMap<String,Object> map = new HashMap<String,Object>();
		if(sessionData == null)
		{
			map.put("user_session",null);
			map.put("user_name",null);
		}
		else
		{
			map.put("user_session",sessionData.getSession());
			map.put("user_name",sessionData.getUserName());
		}
		WebService.outJson(response,map);
	}
	private void loginUser(Map<?, ?> param, HttpServletResponse response)
	{
		String user = (String) param.get("user");
		String pass = (String) param.get("pass");

		HashMap<String,Object> map = new HashMap<String,Object>();

		String session = mUserDB.login(user, pass);
		if(session != null)
		{
			map.put("user_session",session);
			map.put("user_name",user);
		}
		else
		{
			map.put("user_session",null);
			map.put("user_name",null);
		}
		WebService.outJson(response,map);
	}
	private void setUser(Map<?, ?> param, HttpServletResponse response, SessionData sessionData)
	{
		boolean flag = false;
		if(sessionData != null)
		{
			int userId = (Integer)param.get("user_id");
			boolean userEnable = (Boolean)param.get("user_enable");
			String userName = (String) param.get("user_name");
			String userPass = (String) param.get("user_pass");
			if(userId > 0)
				flag = mUserDB.setUser(userId, userName, userPass, userEnable);
			else
				flag = mUserDB.addUser(userName, userPass, userEnable)>0;

		}

		HashMap<String,Object> map = new HashMap<String,Object>();
		map.put("return",flag);
		WebService.outJson(response,map);
	}
	private void delUser(Map<?, ?> param, HttpServletResponse response, SessionData sessionData)
	{
		boolean flag = false;
		if(sessionData != null)
		{
			try
			{
				int userId = (Integer)param.get("user_id");
				if(userId > 1)
					flag = mUserDB.delUser(userId);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		HashMap<String,Object> map = new HashMap<String,Object>();
		map.put("return",flag);
		WebService.outJson(response,map);
	}
	private void getUser(Map<?, ?> param, HttpServletResponse response, SessionData sessionData)
	{
		HashMap<String,Object> map = new HashMap<String,Object>();
		if(sessionData != null)
		{
			List<UserData> list = mUserDB.getUserList();
			map.put("user_list", list);
		}
		WebService.outJson(response,map);
	}
	public static SessionData getSession(DBUser dbUser,HttpServletRequest request)
	{
		String session = request.getHeader("X-Session");
		if(session != null)
			return dbUser.getSession(session);
		return null;
	}
}
