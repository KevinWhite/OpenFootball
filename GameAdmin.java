import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class GameAdmin extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="Formations";

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
      if(Routines.loginCheck(true,request,response,database,context))
        {
        return;
        }
	  String action=request.getParameter("action");
	  String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
      if("uploadPlayers".equals(action))
        {
        for(int letter=0;letter<alphabet.length();letter++)
          {
          boolean firstTime=false;
          if(letter==0)
            {
            firstTime=true;	 	
            }
          Routines.getWebPage("http://www.nfl.com/players/playerindex/"+alphabet.charAt(letter),database,session,firstTime,context);
	      try{
	          Thread.sleep(30);
	          }
           catch(InterruptedException error)
	          {
	          Routines.writeToLog(servletName,"Unable to sleep : " + error,false,context);
	          }  
           }
        }
      Routines.WriteHTMLHead(null,//title
                             false,//showMenu
                             11,//menuHighLight
                             false,//seasonsMenu
		                     false,//weeksMenu
                             false,//scores
                             false,//standings
                             false,//gameCenter
                             false,//schedules
                             false,//previews
                             false,//teamCenter
		                     false,//draft
                             database,//database
                             request,//request
                             response,//response
                             webPageOutput,//webPageOutput
                             context);//context
      pool.returnConnection(database);
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/Admin.gif\"" +
                            " WIDTH='125' HEIGHT='115' ALT='Admin'>");
	  Routines.messageCheck(true,request,webPageOutput);
      webPageOutput.println("</CENTER>");
      Routines.tableStart(true,webPageOutput);
      Routines.tableHeader("Game Admin Menu",0,webPageOutput);
	  Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
	  Routines.WriteHTMLLink(request,
							 response,
							 webPageOutput,
							 "GameAdmin",
							 "action=uploadPlayers",
							 "Upload Players",
							 null,
							 true);
	  Routines.tableDataEnd(true,true,true,webPageOutput);
	  Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
	  Routines.WriteHTMLLink(request,
							 response,
							 webPageOutput,
							 "CreatePlayer",
							 null,
							 "Create Player",
							 null,
							 true);
	  Routines.tableDataEnd(true,true,true,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      Routines.WriteHTMLLink(request,
                             response,
                             webPageOutput,
                             "SkillTables",
                             null,
                             "Skill Tables",
                             null,
                             true);
      Routines.tableDataEnd(true,true,true,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      Routines.WriteHTMLLink(request,
                             response,
                             webPageOutput,
                             "Positions",
                             null,
                             "Positions",
                             null,
                             true);
      Routines.tableDataEnd(true,true,true,webPageOutput);
	  Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
	  Routines.WriteHTMLLink(request,
							 response,
							 webPageOutput,
							 "PositionDepthCharts",
							 null,
							 "PositionDepthCharts",
							 null,
							 true);
	  Routines.tableDataEnd(true,true,true,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      Routines.WriteHTMLLink(request,
                             response,
                             webPageOutput,
                             "Strategies",
                             null,
                             "Strategies",
                             null,
                             true);
      Routines.tableDataEnd(true,true,true,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      Routines.WriteHTMLLink(request,
                             response,
                             webPageOutput,
                             "Plays",
                             null,
                             "Plays",
                             null,
                             true);
      Routines.tableDataEnd(true,true,true,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      Routines.WriteHTMLLink(request,
                             response,
                             webPageOutput,
                             "DefaultSets",
                             null,
                             "Default Sets",
                             null,
                             true);
      Routines.tableDataEnd(true,true,true,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      Routines.WriteHTMLLink(request,
                             response,
                             webPageOutput,
                             "Formations",
                             null,
                             "Formations",
                             null,
                             true);
      Routines.tableDataEnd(true,true,true,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      Routines.WriteHTMLLink(request,
                             response,
                             webPageOutput,
                             "PlayOrders",
                             null,
                             "Play Orders",
                             null,
                             true);
      Routines.tableDataEnd(true,true,true,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      Routines.WriteHTMLLink(request,
                             response,
                             webPageOutput,
                             "SituationCalls",
                             null,
                             "Situation Calls",
                             null,
                             true);
      Routines.tableDataEnd(true,true,true,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      Routines.WriteHTMLLink(request,
                             response,
                             webPageOutput,
                             "DefaultSituations",
                             null,
                             "Default Situations",
                             null,
                             true);
      Routines.tableDataEnd(true,true,true,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      Routines.WriteHTMLLink(request,
                             response,
                             webPageOutput,
                             "DefaultPlayBook",
                             "type=0",
                             "Default Offensive PlayBook",
                             null,
                             true);
      Routines.tableDataEnd(true,true,true,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      Routines.WriteHTMLLink(request,
                             response,
                             webPageOutput,
                             "DefaultPlayBook",
                             "type=1",
                             "Default Defensive PlayBook",
                             null,
                             true);
      Routines.tableDataEnd(true,true,true,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      Routines.WriteHTMLLink(request,
                             response,
                             webPageOutput,
                             "DefaultPlayBook",
                             "type=2",
                             "Default Special Teams PlayBook",
                             null,
                             true);
      Routines.tableDataEnd(true,true,true,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      Routines.WriteHTMLLink(request,
                             response,
                             webPageOutput,
                             "Skills",
                             null,
                             "Skills",
                             null,
                             true);
      Routines.tableDataEnd(true,true,true,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      Routines.WriteHTMLLink(request,
                             response,
                             webPageOutput,
                             "PositionSkills",
                             null,
                             "Position Skills",
                             null,
                             true);
      Routines.tableDataEnd(true,true,true,webPageOutput);
	  Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
	  Routines.WriteHTMLLink(request,
							 response,
							 webPageOutput,
							 "Injuries",
							 null,
							 "Injuries",
							 null,
							 true);
	  Routines.tableDataEnd(true,true,true,webPageOutput);     
	  Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
	  Routines.WriteHTMLLink(request,
							 response,
							 webPageOutput,
							 "GameSettings",
							 null,
							 "GameSettings",
							 null,
							 true);
	  Routines.tableDataEnd(true,true,true,webPageOutput);    
	  Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
	  Routines.WriteHTMLLink(request,
							 response,
							 webPageOutput,
							 "ActionCards",
							 null,
							 "ActionCards",
							 null,
							 true);
	  Routines.tableDataEnd(true,true,true,webPageOutput);   	
	  Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
	  Routines.WriteHTMLLink(request,
							 response,
							 webPageOutput,
							 "PlayDescriptions",
							 null,
							 "PlayDescriptions",
							 null,
							 true);
	  Routines.tableDataEnd(true,true,true,webPageOutput);   	  
	  Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
	  Routines.WriteHTMLLink(request,
							 response,
							 webPageOutput,
							 "Colleges",
							 null,
							 "Colleges",
							 null,
							 true);
	  Routines.tableDataEnd(true,true,true,webPageOutput);   
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(2));
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }
   }