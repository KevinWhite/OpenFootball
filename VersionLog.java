import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class VersionLog extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="VersionLog";

   public void init()
      {
      context = getServletContext();
      synchronized(context)
        {
        pool=(ConnectionPool)context.getAttribute("pool");
        if(pool==null)
          {
          String driverClassName = context.getInitParameter("driverClassName");
          String url = context.getInitParameter("url");
          String userName = context.getInitParameter("username");
          String password = context.getInitParameter("password");
          try
            {
            pool=new ConnectionPool(driverClassName,url,userName,password);
            }
          catch(Exception error)
            {
			Routines.writeToLog(servletName,"Unable to create connection pool : " + error,false,context);
            }
          context.setAttribute("pool",pool);
          }
        }
      }

   public void doGet(HttpServletRequest request,
                     HttpServletResponse response)
      {
      response.setContentType("text/html");
	  PrintWriter webPageOutput=null;
	  try
		{
		webPageOutput=response.getWriter();
		}
	  catch(IOException error)
		{
		Routines.writeToLog(servletName,"Error getting writer : " + error,false,context);
		}
      HttpSession session=request.getSession();
      session.setAttribute("redirect",request.getRequestURL() + "?" + request.getQueryString());
      Connection database=null;
      try
        {
        database=pool.getConnection(servletName);
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to connect to database : " + error,false,context);
        }
      Routines.WriteHTMLHead("Version Log",//title
                             true,//showMenu
                             0,//menuHighLight
                             false,//seasonsMenu
		                     false,//weeksMenu
                             false,//scores
                             false,//standings
                             false,//gameCenter
                             false,//Schedules
                             false,//previews
                             false,//teamCenter
		                     false,//draft
                             database,//database
                             request,//request
                             response,//response
                             webPageOutput,//webPageOutput
                             context);//context
     webPageOutput.println("<CENTER>");
     webPageOutput.println("<IMG SRC=\"../Images/Log.gif\"" +
                           " WIDTH='57' HEIGHT='100' ALT='Version Log'>");
     webPageOutput.println("</CENTER>");
     Routines.tableStart(false,webPageOutput);
     int majorVersion=0;
     int minorVersion=0;
     if("View".equals((String)session.getAttribute("action")))
       {
       String version=(String)session.getAttribute("version");
       int dot=version.indexOf(".");
       majorVersion=Routines.safeParseInt(version.substring(0,dot));
       minorVersion=Routines.safeParseInt(version.substring(dot+1,version.length()));
       }
     else
       {
       majorVersion=Routines.safeParseInt((String)session.getAttribute("majorVersion"));
       minorVersion=Routines.safeParseInt((String)session.getAttribute("minorVersion"));
       }
     try
       {
       Statement sql=database.createStatement();
       ResultSet queryResults=sql.executeQuery("SELECT MajorVersion,MinorVersion,ChangeDate " +
                                               "FROM changelog " +
                                               "ORDER BY MajorVersion DESC,MinorVersion DESC, ChangeLog DESC");
       int currentMajorVersion=0;
       int currentMinorVersion=0;
       Date currentDate=new Date(0);
       String selected="";
       String minorVersionText="";
       String selectText="<FORM ACTION=\"http://" +
                          request.getServerName() +
                          ":" +
                          request.getServerPort() +
                          request.getContextPath() +
                          "/servlet/VersionLog\" METHOD=\"GET\">";
       selectText=selectText + "<SELECT NAME=\"version\">";
       while(queryResults.next())
         {
         int tempMajorVersion=queryResults.getInt(1);
         int tempMinorVersion=queryResults.getInt(2);
         currentDate=queryResults.getDate(3);
         if(tempMajorVersion!=currentMajorVersion||
            tempMinorVersion!=currentMinorVersion)
            {
            currentMajorVersion=tempMajorVersion;
            currentMinorVersion=tempMinorVersion;
            if(tempMajorVersion==majorVersion&&
               tempMinorVersion==minorVersion)
               {
               selected="SELECTED ";
               }
            else
               {
               selected="";
               }
            minorVersionText=Routines.minorVersionText(tempMinorVersion);
            selectText=selectText +
                       " " +
                       "<OPTION " +
                       selected +
                       "VALUE=\"" +
                       tempMajorVersion +
                       "." +
                       tempMinorVersion +
                       "\">" +
                       "Version " +
                       tempMajorVersion +
                       "." +
                       minorVersionText +
                       " loaded " +
                       Routines.reformatDate(currentDate);
            }
          }
       selectText=selectText + " " + "</SELECT>";
       selectText=selectText + " " + "<INPUT TYPE=\"SUBMIT\" NAME=\"action\" VALUE=\"View\">";
       selectText=selectText + " " + "<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">";
       selectText=selectText + " " + "</FORM>";
       Routines.tableHeader(selectText,
                            2,
                            webPageOutput);

       queryResults=sql.executeQuery("SELECT ChangeLog,MajorVersion,MinorVersion,ChangeDate,ChangeType,ChangeText " +
                                     "FROM changelog " +
                                     "WHERE MajorVersion = " + majorVersion + " " +
                                     "AND MinorVersion = " + minorVersion + " " +
                                     "ORDER BY ChangeType,ChangeLog ASC");
       int changeLog=0;
       int changeType=0;
       int currentType=0;
       String changeText="";
       String typeText="";
       Date changeDate;
       while(queryResults.next())
	     {
         changeLog=queryResults.getInt(1);
         majorVersion=queryResults.getInt(2);
         minorVersion=queryResults.getInt(3);
         changeDate=queryResults.getDate(4);
         changeType=queryResults.getInt(5);
         changeText=queryResults.getString(6);
         minorVersionText=Routines.minorVersionText(minorVersion);
         if(changeType!=currentType)
           {
           currentType=changeType;
           String titleText="";
           if(changeType==1)
             {
             titleText="Bug Fixes";
             typeText="B";
             }
           if(changeType==2)
             {
             titleText="New Features";
             typeText="N";
             }
           Routines.tableHeader(titleText,2,webPageOutput);
           }
         Routines.tableDataStart(true,false,false,true,false,15,0,"scoresrow",webPageOutput);
         webPageOutput.println(majorVersion + "." + minorVersionText + "-" + changeLog + "-" + typeText);
         Routines.tableDataEnd(false,false,false,webPageOutput);
         Routines.tableDataStart(true,false,false,false,false,85,0,"scoresrow",webPageOutput);
         webPageOutput.println(changeText);
         Routines.tableDataEnd(false,false,true,webPageOutput);
         }
       }
     catch(SQLException error)
       {
	   Routines.writeToLog(servletName,"Unable to access ChangeLog : " + error,false,context);	
       }
      Routines.tableEnd(webPageOutput);
      pool.returnConnection(database);
      webPageOutput.println(Routines.spaceLines(15));
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }
   }