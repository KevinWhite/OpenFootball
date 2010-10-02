import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class MyTeam extends HttpServlet
   {
   private ConnectionPool pool;
   private static ServletContext context;
   private static String servletName="MyTeam";

   public void init()
      {
      context=getServletContext();
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
      if(Routines.loginCheck(false,request,response,database,context))
        {
        return;
        }
      Routines.WriteHTMLHead("My Team Menu",//title
                             true,//showMenu
                             9,//menuHighLight
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
      int league=Routines.safeParseInt((String)session.getAttribute("league"));
      int team=Routines.safeParseInt((String)session.getAttribute("team"));
      boolean staffDraft=false;
      boolean playerDraft=false;
      boolean lockDown=false;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResults;
        queryResults=sql.executeQuery("SELECT Season,Week,LockDown,Status " +
                                      "FROM leagues " +
                                      "WHERE LeagueNumber=" + league);
        queryResults.first();
        int season=queryResults.getInt(1);
        int week=queryResults.getInt(2);
        int lockDownValue=queryResults.getInt(3);
        int leagueStatus=queryResults.getInt(4);
        int status=0;
        if(season==1&&week==0&&leagueStatus!=2)
          {
          queryResults=sql.executeQuery("SELECT Status " +
                                        "FROM teams " +
                                        "WHERE TeamNumber=" + team);
          if(queryResults.first())
            {
            status=queryResults.getInt(1);
            if(status==0)
              {
              staffDraft=true;
              }
            if(status==1)
              {
              playerDraft=true;
              }
            }
          }
        if(lockDownValue==1)
          {
          lockDown=true;
          staffDraft=false;
          playerDraft=false;
          }
        }
      catch(SQLException error)
        {
        session.setAttribute("message",error.getMessage());
        }
      pool.returnConnection(database);
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/FrontOfficeText.jpg\"" +
                            " WIDTH='486' HEIGHT='79' ALT='Front Office Text'>");
      webPageOutput.println(Routines.spaceLines(1));
      webPageOutput.println("<IMG SRC=\"../Images/FrontOffice.gif\"" +
                            " WIDTH='120' HEIGHT='96' ALT='Front Office'>");
      webPageOutput.println("</CENTER>");
      Routines.messageCheck(true,request,webPageOutput);
      if(lockDown)
        {
        response.setHeader("Refresh","60");
        Routines.myTableStart(false,webPageOutput);
        Routines.myTableHeader((String)session.getAttribute("teamName"),1,webPageOutput);
        Routines.tableEnd(webPageOutput);
        Routines.tableStart(false,webPageOutput);
        Routines.tableHeader("Please wait...",0,webPageOutput);
        Routines.tableDataStart(true,false,false,true,false,0,0,"scoresrow",webPageOutput);
        webPageOutput.println("Your league is currently being updated, ");
        webPageOutput.println("whilst this is happening all coaches are prevented from updating the system. ");
        webPageOutput.println("Updates normally take a few minutes, after which this message will be removed ");
        webPageOutput.println("and you will be able to access your team again.");
        Routines.tableDataEnd(false,true,true,webPageOutput);
        Routines.tableEnd(webPageOutput);
        }
      if(staffDraft)
        {
        Routines.myTableStart(false,webPageOutput);
        Routines.myTableHeader((String)session.getAttribute("teamName"),1,webPageOutput);
        Routines.tableEnd(webPageOutput);
        Routines.tableStart(false,webPageOutput);
        Routines.turnCountDown(league,true,false,database,webPageOutput,context,session);
        Routines.tableDataStart(true,false,false,true,false,0,0,"scoresrow",webPageOutput);
        webPageOutput.println("First up, get some staff...");
        webPageOutput.println(Routines.spaceLines(2));
        webPageOutput.println("All teams initially sign the same number of staff members. ");
        webPageOutput.println("Your staff will have a spread of talent, some great, some average, some poor - all of which counts against the salary cap.");
        webPageOutput.println("From the \"Sign Staff\" link below you can rate each staff position according to your own preferences. ");
        webPageOutput.println("Staff at the top of the list will be given the highest skill ratings. ");
        webPageOutput.println("Staff at the bottom of the list will get the lowest skill ratings. ");
        webPageOutput.println(Routines.spaceLines(2));
        webPageOutput.println("A brief description of the impact each staff member has on your squad is also ");
        webPageOutput.println("given. To simplify matters at the start of your first season all staff ");
        webPageOutput.println("members are signed to a fixed one year contract. Whilst staff contracts do count ");
        webPageOutput.println("against the salary cap, after the first season you are free to spend as much, or as little of ");
        webPageOutput.println("your money on staff as you wish. ");
        webPageOutput.println(Routines.spaceLines(2));
        webPageOutput.println("Once the first season has completed, staff signings are treated the same as player signings ");
        webPageOutput.println("in that you can specify the value and duration of a contract you are willing ");
        webPageOutput.println("to offer a member of staff. You can also trade and bid for staff with other ");
        webPageOutput.println("teams.");
        webPageOutput.println(Routines.spaceLines(2));
        webPageOutput.println("Once you've rated your staff, click on the \"Sign Contracts\" button and confirm your choices. ");
        webPageOutput.println("You will then have signed your staff, viewable on your roster and will be brought back to this page, to prepare for the draft.");
        webPageOutput.println(Routines.spaceLines(3));
        webPageOutput.println("<CENTER>");
        Routines.WriteHTMLLink(request,
                               response,
                               webPageOutput,
                               "SignStaff",
                               "league=" +
                               league +
                               "&team=" +
                               team,
                               "Sign Staff",
                               "boxLink",
                               true);
        webPageOutput.println("</CENTER>");
        Routines.tableDataEnd(false,true,true,webPageOutput);
        Routines.tableEnd(webPageOutput);
        }
      if(playerDraft)
        {
        Routines.myTableStart(false,webPageOutput);
        Routines.myTableHeader((String)session.getAttribute("teamName"),1,webPageOutput);
        Routines.tableEnd(webPageOutput);
        Routines.tableStart(false,webPageOutput);
        Routines.turnCountDown(league,true,false,database,webPageOutput,context,session);
        Routines.tableDataStart(true,false,false,true,false,0,0,"scoresrow",webPageOutput);
        webPageOutput.println("Next, prepare for the draft...");
        webPageOutput.println(Routines.spaceLines(2));
        webPageOutput.println("Your staff have been selected and signed.");
        webPageOutput.println(Routines.spaceLines(2));
        webPageOutput.println("You now need to prepare for the setup draft. The draft order is set randomly, however unlike ");
        webPageOutput.println("a normal draft, the setup draft is balanced. If you pick first in the first round, you pick last ");
        webPageOutput.println("in the second round. Whereas if you pick last in the first round, you pick first in the second round. ");
        webPageOutput.println("the overall effect is to balance the draft regardless of where you pick.");
        webPageOutput.println(Routines.spaceLines(2));
        webPageOutput.println("You need to select which position you wish to fill in each round, you do this from the \"Rate Positions\" link below. ");
        webPageOutput.println("Once your draft has selected a player for every position, your selections begin again from the highest rated position. So after the draft you will have ");
        webPageOutput.println("two players per position on your squad. The remaining places on your squad will be filled ");
        webPageOutput.println("players who are willing to sign for the league minimum salary, although you can replace these players ");
        webPageOutput.println("with free agents signed during the pre-season.");
        webPageOutput.println(Routines.spaceLines(2));
        webPageOutput.println("Your scout has rated every player available for the setup draft.");
        webPageOutput.println("You can view his rating of every player and every skill from the \"View Players\" link below.");
        webPageOutput.println(Routines.spaceLines(2));
        webPageOutput.println("Your scout has also rated every player for every position which will be used ");
        webPageOutput.println("to select players during the draft. You can view, or amend his ratings of players from the \"Rate Players\" link below.");
        webPageOutput.println(Routines.spaceLines(2));
        webPageOutput.println("You can continue to prepare for the draft right up to draft day, the draft will then be made ");
        webPageOutput.println("and this page will change to allow you to see your players and just as importantly prepare for your");
        webPageOutput.println("opening match.");
        webPageOutput.println(Routines.spaceLines(3));
        webPageOutput.println("<CENTER>");
        Routines.WriteHTMLLink(request,
                               response,
                               webPageOutput,
                               "RatePositions",
                               "league=" +
                               league +
                               "&team=" +
                               team,
                               "Rate Positions",
                               "boxLink",
                               true);
        webPageOutput.println(Routines.indent(2));
        Routines.WriteHTMLLink(request,
                               response,
                               webPageOutput,
                               "SortPlayers",
                               "league=" +
                               league +
                               "&team=" +
                               team,
                               "View Players",
                               "boxLink",
                               true);
        webPageOutput.println(Routines.indent(2));
        Routines.WriteHTMLLink(request,
                               response,
                               webPageOutput,
                               "RatePlayers",
                               "league=" +
                               league +
                               "&team=" +
                               team,
                               "Rate Players",
                               "boxLink",
                               true);
        webPageOutput.println("</CENTER>");
        Routines.tableDataEnd(false,true,true,webPageOutput);
        Routines.tableEnd(webPageOutput);
        }
      if(!lockDown&&!staffDraft&&!playerDraft)
        {
        Routines.myTableStart(true,webPageOutput);
        Routines.myTableHeader((String)session.getAttribute("teamName"),1,webPageOutput);
        webPageOutput.println("<TR ALIGN=\"center\" CLASS=\"bg1\">");
        webPageOutput.println("<TD>");
        Routines.WriteHTMLLink(request,
                               response,
                               webPageOutput,
                               "MyRoster",
                               "league=" +
                               league +
                               "&team=" +
                               team,
                               "My Roster",
                               null,
                               true);
        webPageOutput.println("</TD>");
        webPageOutput.println("</TR>");
        webPageOutput.println("<TR ALIGN=\"center\" CLASS=\"bg1\">");
        webPageOutput.println("<TD>");
        Routines.WriteHTMLLink(request,
                               response,
                               webPageOutput,
                               "MyOffensiveGamePlan",
                               "league=" +
                               league +
                               "&team=" +
                               team,
                               "My Offensive GamePlan",
                               null,
                               true);
        webPageOutput.println("</TD>");
        webPageOutput.println("</TR>");
        webPageOutput.println("<TR ALIGN=\"center\" CLASS=\"bg1\">");
        webPageOutput.println("<TD>");
        Routines.WriteHTMLLink(request,
                               response,
                               webPageOutput,
                               "MyDefensiveGamePlan",
                               "league=" +
                               league +
                               "&team=" +
                               team,
                               "My Defensive GamePlan",
                               null,
                               true);
        webPageOutput.println("</TD>");
        webPageOutput.println("</TR>");
        }
      Routines.tableEnd(webPageOutput);
      if(!lockDown&&!staffDraft&&!playerDraft)
        {
        webPageOutput.println(Routines.spaceLines(10));
        }
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }
   }