package to.pns.lib;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class WebService
{
    public static SQLite gSqlite = null;
    public static int gSqliteCount = 0;
    public static String gDBFileName = "test.db";
    public static SQLite openDB(HttpServlet servlet)
    {
    	gSqliteCount++;
    	if(gSqlite != null)
    		return gSqlite;
        String path=null;
		try {
			path = servlet.getServletContext().getRealPath("/WEB-INF/"+gDBFileName);
			SQLite sqlite = new SQLite();
			sqlite.open(path);
			gSqlite = sqlite;
			return sqlite;
 		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		return null;
    }
    public static void closeDB()
    {
    	--gSqliteCount;
    	if(gSqliteCount == 0)
    	{
        	if(gSqlite != null)
        	{
        		gSqlite.close();
        		gSqlite = null;
        	}
    	}
    }
    public static HashMap<String,String> getParameter(HttpServletRequest request)
	{
		HashMap<String,String> params = new HashMap<String,String>();
		String str = request.getQueryString();
		if(str != null)
		{
			String[] datas = str.split("&");
			for(String p : datas)
			{
				String[] data = p.split("=");
				try
				{
					String name = URLDecoder.decode(data[0],"utf-8");
					if(data.length > 1)
					{
						String value = URLDecoder.decode(data[1],"utf-8");

						params.put(name,value);
					}
					else
						params.put(name,"");
				} catch (UnsupportedEncodingException e)
				{
					e.printStackTrace();
				}
			}
		}
		return params;
	}
	public static void outJson(HttpServletResponse response,Map<String,?> map)
	{
		try
		{
			ObjectMapper mapper = new ObjectMapper();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			response.setCharacterEncoding("UTF-8");
			response.setContentType("text/json; charset=UTF-8");
			PrintWriter out = response.getWriter();
			out.println(mapper.writeValueAsString(map));
			out.close();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
