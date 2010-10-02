import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class RatePositions extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="RatePositions";

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

   public void doPost(HttpServletRequest request,
                      HttpServletResponse response)
      {
      doGet(request,response);
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
      String action=request.getParameter("action");
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
      Routines.WriteHTMLHead("Rate Positions",//title
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
      int leagueNumber=Routines.safeParseInt((String)session.getAttribute("league"));
      int teamNumber=Routines.safeParseInt((String)session.getAttribute("team"));
      int positionNumber=0;
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/RatePositions.jpg\"" +
                            " WIDTH='545' HEIGHT='79' ALT='Sign Staff'>");
      webPageOutput.println(Routines.spaceLines(1));
      webPageOutput.println("<IMG SRC=\"../Images/Boss.gif\"" +
                            " WIDTH='160' HEIGHT='120' ALT='Sign Staff'>");
      webPageOutput.println("</CENTER>");
      boolean[] returnBool=Routines.playerDraft(leagueNumber,teamNumber,session,database);
      boolean lockDown=returnBool[0];
      boolean playerDraft=returnBool[1];
      boolean updated=true;
      if (playerDraft&&
         ("Move Position Up".equals(action)||
          "Move Position Down".equals(action)))
          {
          updated=updateEntry(teamNumber,
                              leagueNumber,
                              action,
                              session,
                              request,
                              database);
          }
      if(!playerDraft||lockDown||"Return to MyTeam page".equals(action))
        {
        if(!playerDraft||lockDown)
          {
          session.setAttribute("message","Draft deadline has passed, the draft will commence shortly");
          }
        session.setAttribute("redirect",
                             "http://" +
                             request.getServerName() +
                             ":" +
                             request.getServerPort() +
                             request.getContextPath() +
                             "/servlet/MyTeam?jsessionid=" + session.getId() + "&league=" + leagueNumber + "&team=" + teamNumber);
        try
          {
          response.sendRedirect((String)session.getAttribute("redirect"));
          }
        catch(IOException error)
          {
		  Routines.writeToLog(servletName,"Unable to redirect : " + error,false,context);	
          }	  
        return;
        }
      webPageOutput.println("<FORM ACTION=\"http://" +
                             request.getServerName() +
                             ":" +
                             request.getServerPort() +
                             request.getContextPath() +
                             "/servlet/RatePositions\" METHOD=\"POST\">");
      if((String)session.getAttribute("message")!=null)
        {
        Routines.tableStart(false,webPageOutput);
        Routines.tableHeader("Messages",0,webPageOutput);
        Routines.tableDataStart(true,false,true,true,false,0,0,"scoresrow",webPageOutput);
        Routines.messageCheck(false,request,webPageOutput);
        Routines.tableDataEnd(true,false,true,webPageOutput);
        Routines.tableEnd(webPageOutput);
        webPageOutput.println(Routines.spaceLines(1));
        }
      viewScreen(teamNumber,
                 leagueNumber,
                 action,
                 session,
                 database,
                 request,
                 response,
                 webPageOutput);
      pool.returnConnection(database);
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }

   private void viewScreen(int teamNumber,
                           int leagueNumber,
                           String action,
                           HttpSession session,
                           Connection database,
                           HttpServletRequest request,
                           HttpServletResponse response,
                           PrintWriter webPageOutput)
      {
      int draftPriorityNumber=0;
      int numOfTeams=0;
      int ranking=0;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResults;
        queryResults=sql.executeQuery("SELECT COUNT(TeamNumber) " +
                                      "FROM leagueteams,divisions,conferences " +
                                      "WHERE leagueteams.DivisionNumber = divisions.DivisionNumber " +
                                      "AND divisions.ConferenceNumber=conferences.ConferenceNumber " +
                                      "AND conferences.LeagueNumber=" + leagueNumber);
        queryResults.first();
        numOfTeams=queryResults.getInt(1);
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Error counting teams = " + error,false,context);	
        }
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResults;
        queryResults=sql.executeQuery("SELECT Sequence " +
                                      "FROM draftboardteam " +
                                      "WHERE TeamNumber=" + teamNumber);
        queryResults.first();
        ranking=queryResults.getInt(1);
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Error getting ranking = " + error,false,context);	
        }
      Routines.myTableStart(false,webPageOutput);
      Routines.myTableHeader("Please select the position you wish to draft in each round of the draft",10,webPageOutput);
      webPageOutput.println("<TR CLASS=\"columnrow\" ALIGN=\"center\">");
      webPageOutput.println("<TH ALIGN=\"center\">Round</TH>");
      webPageOutput.println("<TH ALIGN=\"center\">Pick</TH>");
      webPageOutput.println("<TH ALIGN=\"center\">Sel</TH>");
      webPageOutput.println("<TH ALIGN=\"center\">Position</TH>");
      webPageOutput.println("</TR>");
      boolean positionsFound=false;
      int rate=0;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT DraftPriorityNumber,PositionName " +
                                     "FROM draftpriorities,positions " +
                                     "WHERE TeamNumber = " + teamNumber + " " +
                                     "AND draftpriorities.PositionNumber=positions.PositionNumber " +
                                     "ORDER BY draftpriorities.Sequence ASC");
        draftPriorityNumber=0;
        String positionName="";
        boolean flipFlop=true;
        while(queryResult.next())
             {
             rate++;
             if(!positionsFound)
               {
               positionsFound=true;
               }
             draftPriorityNumber=queryResult.getInt(1);
             positionName=queryResult.getString(2);
             Routines.tableDataStart(true,true,false,true,false,3,0,"scoresrow",webPageOutput);
             webPageOutput.print(rate);
             Routines.tableDataEnd(false,false,false,webPageOutput);
             Routines.tableDataStart(true,true,false,false,false,3,0,"scoresrow",webPageOutput);
             if(flipFlop)
               {
               webPageOutput.println(ranking);
               }
             else
               {
               webPageOutput.println(numOfTeams-(ranking-1));
               }
             Routines.tableDataStart(true,false,false,false,false,3,0,"scoresrow",webPageOutput);
             webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"" + draftPriorityNumber  + "\" VALUE=\"true\">");
             Routines.tableDataEnd(false,false,false,webPageOutput);
             Routines.tableDataStart(true,false,false,false,false,91,0,"scoresrow",webPageOutput);
             webPageOutput.println(positionName);
             Routines.tableDataEnd(false,false,true,webPageOutput);
             if(flipFlop)
               {
               flipFlop=false;
               }
             else
               {
               flipFlop=true;
               }
             }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to retrieve draftpriorities : " + error,false,context);	
        }
      if(!positionsFound)
        {
        Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
        webPageOutput.println("No Positions found.");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",0,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      if(positionsFound)
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Position Up\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Position Down\" NAME=\"action\">");
        webPageOutput.println(Routines.spaceLines(1));
        }
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Return to MyTeam page\" NAME=\"action\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"league\" VALUE=\"" + leagueNumber + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"team\" VALUE=\"" + teamNumber + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("</FORM>");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      }

   private boolean updateEntry(int teamNumber,
                               int leagueNumber,
                               String action,
                               HttpSession session,
                               HttpServletRequest request,
                               Connection database)
      {
      boolean success=false;
      int sequence=0;
      int draftPriorityNumber=0;
      try
        {
        // Get Latest SequenceNumber.
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT Sequence " +
                                     "FROM draftpriorities " +
                                     "WHERE TeamNumber=" + teamNumber + " " +
                                     "ORDER BY Sequence DESC");
        if(queryResult.first())
          {
          sequence=queryResult.getInt(1);
          }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to retrieve draftpriorities : " + error,false,context);	
        }
      if("Move Position Up".equals(action))
        {
        boolean moveRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT DraftPriorityNumber " +
                                         "FROM draftpriorities " +
                                         "WHERE TeamNumber = " + teamNumber + " " +
                                         "ORDER BY Sequence ASC");
          while(queryResult1.next())
               {
               draftPriorityNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(draftPriorityNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT draftpriorities.Sequence,PositionName FROM draftpriorities,positions " +
                                                "WHERE DraftPriorityNumber=" + draftPriorityNumber + " " +
                                                "AND draftpriorities.PositionNumber=positions.PositionNumber");
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==1)
                   {
                   session.setAttribute("message",queryResult2.getString(2) + " is already at the top of the position list");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE draftpriorities " +
                                            "SET Sequence=(Sequence+1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE TeamNumber=" + teamNumber + " " +
                                            "AND Sequence=" + (currentSequence-1));
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Position not moved (prior), reason unknown",false,context);	
                   }
                 updates=sql1.executeUpdate("UPDATE draftpriorities " +
                                            "SET Sequence=(Sequence-1),DateTimeStamp='" +
                                            Routines.getDateTime(false)  + "' " +
                                            "WHERE DraftPriorityNumber=" + draftPriorityNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Position not moved (current), reason unknown",false,context);	
                   }
                 }
               }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to move position : " + error,false,context);	
          }
        if(moveRequested)
          {
          session.setAttribute("message","Move successfull");
          }
        else
          {
          session.setAttribute("message","No position selected");
          }
        success=true;
        }
      if("Move Position Down".equals(action))
        {
        boolean moveRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT DraftPriorityNumber " +
                                         "FROM draftpriorities " +
                                         "WHERE TeamNumber=" + teamNumber + " " +
                                         "ORDER BY Sequence DESC");
          while(queryResult1.next())
               {
               draftPriorityNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(draftPriorityNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT draftpriorities.Sequence,PositionName FROM draftpriorities,positions " +
                                                "WHERE DraftPriorityNumber=" + draftPriorityNumber + " " +
                                                "AND draftpriorities.PositionNumber=positions.PositionNumber");
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==sequence)
                   {
                   session.setAttribute("message",queryResult2.getString(2) + " is already at the bottom of the position list");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE draftpriorities " +
                                            "SET Sequence=(Sequence-1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE TeamNumber=" + teamNumber + " " +
                                            "AND Sequence=" + (currentSequence+1));
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Position not moved (prior), reason unknown",false,context);	
                   }
                 updates=sql1.executeUpdate("UPDATE draftpriorities " +
                                            "SET Sequence=(Sequence+1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE DraftPriorityNumber=" + draftPriorityNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Position not moved (current), reason unknown",false,context);	
                   }
                 }
               }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to move position : " + error,false,context);	
          }
        if(moveRequested)
          {
          session.setAttribute("message","Move successfull");
          }
        else
          {
          session.setAttribute("message","No position selected");
          }
        success=true;
        }
      return success;
      }
}