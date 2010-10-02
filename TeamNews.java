import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class TeamNews extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="TeamNews";

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
      String action=request.getParameter("action");
      if("Coach This Team".equals(action)||
         "Click here to coach the team you selected".equals(action))
         {
         int coachNumber=Routines.safeParseInt((String)session.getAttribute("coachNumber"));
         int league=Routines.safeParseInt(request.getParameter("league"));
         int selectedTeam=Routines.safeParseInt(request.getParameter("team"));
         if(coachNumber==0)
           {
           try
             {	
             response.sendRedirect("http://" +
                                   request.getServerName() +
                                   ":" +
                                   request.getServerPort() +
                                   request.getContextPath() +
                                   "/servlet/CoachMaintenance?league=" + league + "&team=" + selectedTeam);
             return;
             }
           catch(IOException error)
             {
             Routines.writeToLog(servletName,"Unable to redirect : " + error,false,context);	
             }	  
           }
         if(Routines.loginCheck(false,request,response,database,context))
           {
           return;
           }
         boolean valid=true;
         boolean afterMidSeason=true;
         boolean admin=false;
         ServletContext context = getServletContext();
         String payPerPlayText=context.getInitParameter("payperplay");
         boolean payPerPlay=false;
         if("yes".equalsIgnoreCase(payPerPlayText))
           {
           payPerPlay=true;
           }
         int alphaLeague=0;
         int liveLeague=0;
         int autoCoach=0;
         int credits=Routines.safeParseInt((String)session.getAttribute("credits"));
         int teamNumber=Routines.safeParseInt(request.getParameter("team"));
         if("true".equals(session.getAttribute("administrator")))
           {
           admin=true;
           }
         try
           {
           Statement sql=database.createStatement();
           ResultSet queryResult=sql.executeQuery("SELECT PreSeasonWeeks,RegularSeasonWeeks,Week,Alpha,Live " +
                                                  "FROM leagues " +
                                                  "WHERE LeagueNumber = " + league);
           if(queryResult.first())
             {
             int weeks=queryResult.getInt(1)+queryResult.getInt(2);
             int currentWeek=queryResult.getInt(3);
             if(currentWeek<(((weeks)/2)+1))
               {
               afterMidSeason=false;
               }
             alphaLeague=queryResult.getInt(4);
             liveLeague=queryResult.getInt(5);
             }
           else
             {
             session.setAttribute("message","Unable to find league details");
             valid=false;
             }
           if(valid)
             {
             queryResult=sql.executeQuery("SELECT AutoCoach " +
                                          "FROM coaches,coachteams " +
                                          "WHERE TeamNumber=" + teamNumber + " " +
                                          "AND coachteams.CoachNumber=coaches.CoachNumber");
             if(queryResult.first())
               {
               autoCoach=queryResult.getInt(1);
               }
             else
               {
               session.setAttribute("message","Unable to find coach details");
               valid=false;
               }
             }
           }
         catch(SQLException error)
           {
           session.setAttribute("message",error.getMessage());
           valid=false;
           }
         if(valid&&Routines.getMyTeam(request,league,database,context)!=0&&!admin)
           {
           session.setAttribute("message","You already have one team in this league");
           valid=false;
           }
         if(valid&&afterMidSeason&&!admin)
           {
           session.setAttribute("message","New coaches cannot join a league after midway point in the season");
           valid=false;
           }
         if(valid&&alphaLeague!=0&&!admin)
           {
           session.setAttribute("message","Alpha Leagues cannot be joined");
           valid=false;
           }
         if(valid&&autoCoach==0&&!admin)
           {
           session.setAttribute("message","Sorry, this team has now been taken by another coach");
           valid=false;
           }
         if(valid&&liveLeague!=0&&credits==0&&!admin&&payPerPlay)
           {
           session.setAttribute("message","Sorry, you do not have a credit to join this league. To purchase credits, click on the MyAccount button");
           valid=false;
           }
         if(valid)
           {
           synchronized(this)
              {
              credits--;
              try
                {
                int updated=0;
                Statement sql=database.createStatement();
                updated=sql.executeUpdate("UPDATE coachteams " +
                                          "SET CoachNumber=" + coachNumber + " " +
                                          "WHERE TeamNumber=" + teamNumber);
                if(updated!=1)
                  {
                  session.setAttribute("message","Unable to add new coach");
                  }
                if(liveLeague==1&&!admin)
                  {
                  updated=sql.executeUpdate("UPDATE coaches " +
                                            "SET Credits=" + credits + " " +
                                            "WHERE CoachNumber = " + coachNumber);
                  if(updated!=1)
                    {
                    session.setAttribute("message","Unable to update credits");
                    }
                  }
                }
              catch(SQLException error)
                {
                session.setAttribute("message",error.getMessage());
                }
              if(liveLeague==1&&!admin)
                {
                session.setAttribute("credits",String.valueOf(credits));
                }
              }
           }
         }
      Routines.WriteHTMLHead("Team News",//title
                             true,//showMenu
                             6,//menuHighLight
                             false,//seasonsMenu
		                     false,//weeksMenu
                             false,//scores
                             false,//standings
                             false,//gameCenter
                             false,//schedules
                             false,//previews
                             true,//teamCenter
		                     false,//draft
                             database,//database
                             request,//request
                             response,//response
                             webPageOutput,//webPageOutput
                             context);//context
      Routines.messageCheck(true,request,webPageOutput);
      Routines.WriteHTMLTail(request,response,webPageOutput);
      pool.returnConnection(database);
      }
   }