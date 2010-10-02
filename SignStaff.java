import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.Random;

public class SignStaff extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="SignStaff";

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
      int leagueNumber=Routines.safeParseInt(request.getParameter("leagueNumber"));
      int teamNumber=Routines.safeParseInt(request.getParameter("teamNumber"));
      if("Sign Contracts".equals(action))
        {
        if(session.isNew())
          {
          session.setAttribute("redirect",
                               "http://" +
                               request.getServerName() +
                               ":" +
                               request.getServerPort() +
                               request.getContextPath() +
                               "/servlet/SignStaff?jsessionid=" + session.getId() + "&league=" + leagueNumber + "&team=" + teamNumber);
          }
        }
      else
        {
        session.setAttribute("redirect",request.getRequestURL() + "?" + request.getQueryString());
        }
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
      Routines.WriteHTMLHead("Sign Staff",//title
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
      leagueNumber=Routines.safeParseInt((String)session.getAttribute("league"));
      teamNumber=Routines.safeParseInt((String)session.getAttribute("team"));
      int positionNumber=0;
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/SignStaff.jpg\"" +
                            " WIDTH='391' HEIGHT='79' ALT='Sign Staff'>");
      webPageOutput.println(Routines.spaceLines(1));
      webPageOutput.println("<IMG SRC=\"../Images/Boss.gif\"" +
                            " WIDTH='160' HEIGHT='120' ALT='Sign Staff'>");
      webPageOutput.println("</CENTER>");
      boolean lockDown=false;
      boolean staffDraft=false;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResults;
        queryResults=sql.executeQuery("SELECT Season,Week,LockDown " +
                                      "FROM leagues " +
                                      "WHERE LeagueNumber=" + leagueNumber);
        queryResults.first();
        int season=queryResults.getInt(1);
        int week=queryResults.getInt(2);
        int lockDownValue=queryResults.getInt(3);
        if(season==1&&week==0)
          {
          queryResults=sql.executeQuery("SELECT Status " +
                                        "FROM teams " +
                                        "WHERE TeamNumber=" + teamNumber);
          queryResults.first();
          if(queryResults.getInt(1)==0)
            {
            staffDraft=true;
            }
          }
        if(lockDownValue==1)
          {
          lockDown=true;
          }
        }
      catch(SQLException error)
        {
        session.setAttribute("message",error.getMessage());
        }
      boolean updated=true;
      if (staffDraft&&
         ("Move Position Up".equals(action)||
          "Move Position Down".equals(action)||
          "Confirmed".equals(action)))
          {
          updated=updateEntry(teamNumber,
                              leagueNumber,
                              action,
                              session,
                              request,
                              database);
          }
      if(!staffDraft||
         lockDown||
         "Confirmed".equals(action))
        {
        if(!staffDraft||lockDown)
          {
          session.setAttribute("message","Staff Signing deadline has passed, your staff have been signed by the system on your behalf");
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
                             "/servlet/SignStaff\" METHOD=\"POST\">");
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
      boolean confirmScreen=false;
      if("Sign Contracts".equals(action))
        {
        confirmScreen=true;
        }
      int draftPriorityNumber=0;
      Routines.myTableStart(false,webPageOutput);
      String headerText="";
      if(confirmScreen)
        {
        headerText="Please confirm your requirements for staff";
        }
      else
        {
        headerText="Please rate your requirements for staff";
        }
      Routines.myTableHeader(headerText,10,webPageOutput);
      if(!confirmScreen)
        {
        Routines.tableEnd(webPageOutput);
        Routines.tableStart(false,webPageOutput);
        }
      webPageOutput.println("<TR CLASS=\"columnrow\" ALIGN=\"center\">");
      webPageOutput.println("<TH ALIGN=\"center\">No</TH>");
      if(confirmScreen)
        {
        webPageOutput.println("<TH ALIGN=\"left\">Position</TH>");
        }
      else
        {
        webPageOutput.println("<TH ALIGN=\"center\">Sel</TH>");
        webPageOutput.println("<TH ALIGN=\"center\">Position</TH>");
        }
      if(!confirmScreen)
        {
        webPageOutput.println("<TH ALIGN=\"center\">Description</TH>");
        }
      webPageOutput.println("</TR>");
      boolean positionsFound=false;
      int rate=0;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT DraftPriorityNumber,PositionName,PositionDescription " +
                                     "FROM draftpriorities,positions " +
                                     "WHERE TeamNumber = " + teamNumber + " " +
                                     "AND draftpriorities.PositionNumber=positions.PositionNumber " +
                                     "ORDER BY draftpriorities.Sequence ASC");
        draftPriorityNumber=0;
        String positionName="";
        String positionDescription="";
        while(queryResult.next())
             {
             rate++;
             if(!positionsFound)
               {
               positionsFound=true;
               }
             draftPriorityNumber=queryResult.getInt(1);
             positionName=queryResult.getString(2);
             positionDescription=queryResult.getString(3);
             if(confirmScreen)
               {
               Routines.tableDataStart(true,false,false,true,false,3,0,"scoresrow",webPageOutput);
               webPageOutput.print(rate);
               Routines.tableDataEnd(false,false,false,webPageOutput);
               Routines.tableDataStart(true,false,false,false,false,97,0,"scoresrow",webPageOutput);
               webPageOutput.println(positionName);
               Routines.tableDataEnd(false,false,true,webPageOutput);
               }
             else
               {
               Routines.tableDataStart(true,false,false,true,false,3,0,"scoresrow",webPageOutput);
               webPageOutput.print(rate);
               Routines.tableDataEnd(false,false,false,webPageOutput);
               Routines.tableDataStart(true,false,false,false,false,3,0,"scoresrow",webPageOutput);
               webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"" + draftPriorityNumber  + "\" VALUE=\"true\">");
               Routines.tableDataEnd(false,false,false,webPageOutput);
               Routines.tableDataStart(true,false,false,false,false,25,0,"scoresrow",webPageOutput);
               webPageOutput.println(positionName);
               Routines.tableDataEnd(false,false,false,webPageOutput);
               Routines.tableDataStart(true,false,false,false,false,69,0,"scoresrow",webPageOutput);
               webPageOutput.println(positionDescription);
               Routines.tableDataEnd(false,false,true,webPageOutput);
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
        if(confirmScreen)
          {
          webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Confirmed\" NAME=\"action\">");
          webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel\" NAME=\"action\">");
          }
        else
          {
          webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Position Up\" NAME=\"action\">");
          webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Position Down\" NAME=\"action\">");
          webPageOutput.println(Routines.spaceLines(1));
          webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Sign Contracts\" NAME=\"action\">");
          }
        }
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
      final int[] skills={95,90,85,80,75,70,65,60,55,50,45,40,35,30,25,20,15,10,5};
      final int[] salaries={2000000,1900000,1800000,1700000,1600000,1500000,1400000,1300000,1200000,1100000,1000000,
                            900000,800000,700000,600000,500000,400000,300000,200000};
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
      if("Confirmed".equals(action))
        {
        ServletContext context=getServletContext();
        int numberOfNames[]=null;
        numberOfNames=Routines.getNumOfNames(context);
        Random random = new Random(teamNumber);
        synchronized(this)
          {
          try
            {
            Statement sql1=database.createStatement();
            Statement sql2=database.createStatement();
            ResultSet queryResult;
            // Retrieve latest collegeNumber;
            int numOfColleges=0;
            queryResult=sql1.executeQuery("SELECT DraftPriorityNumber " +
                                          "FROM draftpriorities " +
                                          "WHERE TeamNumber=" + teamNumber + " " +
                                          "AND PositionNumber=39");
            if(queryResult.first())
              {
              sql2.executeUpdate("DELETE FROM players " +
                                 "WHERE TeamNumber=" + teamNumber);
              }
            queryResult=sql1.executeQuery("SELECT   CollegeNumber " +
                                          "FROM     colleges " +
                                          "ORDER BY CollegeNumber DESC");
            if(queryResult.first())
              {
              numOfColleges=queryResult.getInt(1);
              }
            queryResult=sql1.executeQuery("SELECT PositionNumber,Sequence " +
                                          "FROM draftpriorities " +
                                          "WHERE TeamNumber = " + teamNumber + " " +
                                          "ORDER BY draftpriorities.Sequence ASC");
            while(queryResult.next())
                 {
                 int positionNumber=queryResult.getInt(1);
                 sequence=queryResult.getInt(2);
                 sequence--;
                 int forNameNumber=((int)(random.nextDouble()*numberOfNames[0])+1);
                 int surNameNumber=((int)(random.nextDouble()*numberOfNames[1])+1);
                 int collegeNumber=((int)(random.nextDouble()*numOfColleges)+1);
                 int motivationIntelligence=(int)(random.nextDouble()*101);
                 int motivationSuccess=(int)(random.nextDouble()*101);
                 int motivationMoney=(int)(random.nextDouble()*101);
                 int motivationTheGame=(int)(random.nextDouble()*101);
                 int intelligence=(int)(random.nextDouble()*101);
                 int ego=(int)(random.nextDouble()*101);
                 int attitude=(int)(random.nextDouble()*101);
                 int potential=(int)(random.nextDouble()*11);
                 int burnRate=((int)(random.nextDouble()*10))+1;
                 int height=((int)(random.nextDouble()*15))+66;
                 int weight=height*3;
                 int weightAdjust=((int)(random.nextDouble()*84));
                 if(weightAdjust<42)
                   {
                   weight=weight-=(weightAdjust/2);
                   }
                 else
                   {
                   weight=weight+=(weightAdjust/2);
                   }
                 String[] name=null;
                 name=Routines.getName(forNameNumber,
                                       surNameNumber,
                                       context);
//                 int updated=sql2.executeUpdate("INSERT INTO players " +
//                                                "(WorldNumber," +
//                                                "TeamNumber,CollegeNumber," +
//                                                "PositionNumber,Surname," +
//                                                "Forname,Height," +
//                                                "Weight,MotivationIntelligence," +
//                                                "MotivationSuccess,MotivationMoney," +
//                                                "MotivationTheGame,Intelligence," +
//                                                "Ego,Attitude,Potential,BurnRate," +
//                                                "Skill1,ContractValue,ContractLength,DateTimeStamp) VALUES (" +
//                                                leagueNumber + "," +
//                                                teamNumber + "," +
//                                                collegeNumber + "," +
//                                                positionNumber + ",\"" +
//                                                name[1] + "\",\"" +
//                                                name[0] + "\"," +
//                                                height + "," +
//                                                weight + "," +
//                                                motivationIntelligence + "," +
//                                                motivationSuccess + "," +
//                                                motivationMoney + "," +
//                                                motivationTheGame + "," +
//                                                intelligence + "," +
//                                                ego + "," +
//                                                attitude + "," +
//                                                potential + "," +
//                                                burnRate + "," +
//                                                skills[sequence] + "," +
//                                                salaries[sequence] + ",1,'" +
//                                                Routines.getDateTime(false) + "')");
//                 if(updated!=1)
//                   {
//				   Routines.writeToLog(servletName,"Staff not created, reason unknown",false,context);	
//                   }
                 }
            int updated=sql2.executeUpdate("DELETE FROM draftpriorities " +
                                           "WHERE TeamNumber=" + teamNumber);
            if(!Routines.createDraftRatings(leagueNumber,
                                            teamNumber,
                                            true,
                                            session,
                                            database,
                                            context))
              {
              String message=(String)session.getAttribute("message");
              session.removeAttribute("message");
			  Routines.writeToLog(servletName,message,false,context);
              }
            if(!Routines.createDraftPriorities(teamNumber,leagueNumber,database,context))
              {
			  Routines.writeToLog(servletName,"Draft Priorities not created (TeamNumber=" + teamNumber + "), reason unknown",false,context);	
              }
            updated=sql2.executeUpdate("UPDATE teams SET Status=1 " +
                                       "WHERE TeamNumber=" + teamNumber);
            if(updated!=1)
              {
			  Routines.writeToLog(servletName,"Team not updated (TeamNumber=" + teamNumber + "), reason unknown",false,context);		
              }
            }
          catch(SQLException error)
            {
			Routines.writeToLog(servletName,"Unable to create staff : " + error,false,context);		
            }
          }
        }
      return success;
      }
}